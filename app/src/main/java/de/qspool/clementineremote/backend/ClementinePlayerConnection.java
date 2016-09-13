/* This file is part of the Android Clementine Remote.
 * Copyright (C) 2013, Andreas Muttscheller <asfa194@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package de.qspool.clementineremote.backend;

import android.net.TrafficStats;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.util.ArrayList;
import java.util.Date;

import de.qspool.clementineremote.App;
import de.qspool.clementineremote.backend.listener.PlayerConnectionListener;
import de.qspool.clementineremote.backend.pb.ClementineMessage;
import de.qspool.clementineremote.backend.pb.ClementineMessage.ErrorMessage;
import de.qspool.clementineremote.backend.pb.ClementineMessageFactory;
import de.qspool.clementineremote.backend.pb.ClementineRemoteProtocolBuffer.Message.Builder;
import de.qspool.clementineremote.backend.pb.ClementineRemoteProtocolBuffer.MsgType;
import de.qspool.clementineremote.backend.pb.ClementineRemoteProtocolBuffer.ReasonDisconnect;
import de.qspool.clementineremote.backend.pb.ClementineRemoteProtocolBuffer.ResponseDisconnect;

/**
 * This Thread-Class is used to communicate with Clementine
 */
public class ClementinePlayerConnection extends ClementineSimpleConnection
        implements Runnable {

    public ClementineConnectionHandler mHandler;

    public final static int PROCESS_PROTOC = 874456;

    public enum ConnectionStatus {IDLE, CONNECTING, NO_CONNECTION, CONNECTED, LOST_CONNECTION, DISCONNECTED}

    private final long KEEP_ALIVE_TIMEOUT = 25000; // 25 Second timeout

    private final int MAX_RECONNECTS = 5;

    private Handler mUiHandler;

    private int mLeftReconnects;

    private long mLastKeepAlive;

    private ArrayList<PlayerConnectionListener> mListeners
            = new ArrayList<>();

    private ClementineMessage mRequestConnect;

    private long mStartTx;

    private long mStartRx;

    private long mStartTime;

    private Thread mIncomingThread;

    /**
     * Add a new listener for closed connections
     *
     * @param listener The listener object
     */
    public void addPlayerConnectionListener(PlayerConnectionListener listener) {
        mListeners.add(listener);
    }

    public void run() {
        // Start the thread
        Looper.prepare();
        mHandler = new ClementineConnectionHandler(this);

        fireOnConnectionStatusChanged(ConnectionStatus.IDLE);

        Looper.loop();
    }

    /**
     * Try to connect to Clementine
     *
     * @param message The Request Object. Stores the ip to connect to.
     */
    @Override
    public boolean createConnection(ClementineMessage message) {
        fireOnConnectionStatusChanged(ConnectionStatus.CONNECTING);

        // Reset the connected flag
        mLastKeepAlive = 0;

        // Now try to connect and set the input and output streams
        boolean connected = super.createConnection(message);

        // Check if Clementine dropped the connection.
        // Is possible when we connect from a public ip and clementine rejects it
        if (connected && !mSocket.isClosed()) {
            // Now we are connected

            // We can now reconnect MAX_RECONNECTS times when
            // we get a keep alive timeout
            mLeftReconnects = MAX_RECONNECTS;

            // Set the current time to last keep alive
            setLastKeepAlive(System.currentTimeMillis());

            // Until we get a new connection request from ui,
            // don't request the first data a second time
            mRequestConnect = ClementineMessageFactory
                    .buildConnectMessage(message.getIp(), message.getPort(),
                            message.getMessage().getRequestConnect().getAuthCode(),
                            false,
                            message.getMessage().getRequestConnect().getDownloader());

            // Save started transmitted bytes
            int uid = App.getApp().getApplicationInfo().uid;
            mStartTx = TrafficStats.getUidTxBytes(uid);
            mStartRx = TrafficStats.getUidRxBytes(uid);

            mStartTime = new Date().getTime();

            // Create a new thread for reading data from Clementine.
            // This is done blocking, so we receive the data directly instead of
            // waiting for the handler and still be able to send commands directly.
            mIncomingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (isConnected() && !mIncomingThread.isInterrupted()) {
                        checkKeepAlive();

                        ClementineMessage m = getProtoc(3000);
                        if (!m.isErrorMessage() || m.getErrorMessage() != ErrorMessage.TIMEOUT) {
                            Message msg = Message.obtain();
                            msg.obj = m;
                            msg.arg1 = PROCESS_PROTOC;
                            mHandler.sendMessage(msg);
                        }
                    }
                }
            });
            mIncomingThread.start();

            // Get hostname
            if (mSocket.getInetAddress() != null) {
                App.Clementine.setHostname(mSocket.getInetAddress().getHostName());
            }

            fireOnConnectionStatusChanged(ConnectionStatus.CONNECTED);

        } else {
            sendUiMessage(new ClementineMessage(ErrorMessage.NO_CONNECTION));
            fireOnConnectionStatusChanged(ConnectionStatus.NO_CONNECTION);
        }

        return connected;
    }

    /**
     * Process the received protocol buffer
     *
     * @param clementineMessage The Message received from Clementine
     */
    protected void processProtocolBuffer(ClementineMessage clementineMessage) {
        fireOnClementineMessageReceived(clementineMessage);

        // Close the connection if we have an old proto verion
        if (clementineMessage.isErrorMessage()) {
            closeConnection(clementineMessage);
        } else if (clementineMessage.getMessageType() == MsgType.DISCONNECT) {
            closeConnection(clementineMessage);
        } else {
            sendUiMessage(clementineMessage);
        }
    }

    /**
     * Send a message to the ui thread
     *
     * @param obj The Message containing data
     */
    private void sendUiMessage(Object obj) {
        Message msg = Message.obtain();
        msg.obj = obj;
        // Send the Messages
        if (mUiHandler != null) {
            mUiHandler.sendMessage(msg);
        }
    }

    /**
     * Send a request to clementine
     *
     * @param message The request as a RequestToThread object
     * @return true if data was sent, false if not
     */
    @Override
    public boolean sendRequest(ClementineMessage message) {
        // Send the request to Clementine
        boolean ret = super.sendRequest(message);

        // If we lost connection, try to reconnect
        if (!ret) {
            //
            if (mRequestConnect != null) {
                ret = super.createConnection(mRequestConnect);
            }
            if (!ret) {
                // Failed. Close connection
                Builder builder = ClementineMessage.getMessageBuilder(MsgType.DISCONNECT);
                ResponseDisconnect.Builder disc = builder.getResponseDisconnectBuilder();
                disc.setReasonDisconnect(ReasonDisconnect.Server_Shutdown);
                builder.setResponseDisconnect(disc);
                closeConnection(new ClementineMessage(builder));
            }
        }

        return ret;
    }

    /**
     * Disconnect from Clementine
     *
     * @param message The RequestDisconnect Object
     */
    @Override
    public void disconnect(ClementineMessage message) {
        if (isConnected()) {
            // Set the Connected flag to false, so the loop in
            // checkForData() is interrupted
            super.disconnect(message);

            // and close the connection
            closeConnection(message);
        }
    }

    /**
     * Close the socket and the streams
     */
    private void closeConnection(ClementineMessage clementineMessage) {
        // Disconnect socket
        closeSocket();

        sendUiMessage(clementineMessage);

        try {
            mIncomingThread.interrupt();
            mIncomingThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Fire the listener
        if (clementineMessage.isErrorMessage() &&
                (clementineMessage.getErrorMessage() == ErrorMessage.IO_EXCEPTION ||
                        clementineMessage.getErrorMessage() == ErrorMessage.KEEP_ALIVE_TIMEOUT)) {
            fireOnConnectionStatusChanged(ConnectionStatus.LOST_CONNECTION);
        }

        fireOnConnectionStatusChanged(ConnectionStatus.DISCONNECTED);

        // Close thread
        Looper.myLooper().quit();
    }

    /**
     * Check the keep alive timeout.
     * If we reached the timeout, we can assume, that we lost the connection
     */
    private void checkKeepAlive() {
        if (mLastKeepAlive > 0
                && (System.currentTimeMillis() - mLastKeepAlive) > KEEP_ALIVE_TIMEOUT) {
            // Check if we shall reconnect
            while (mLeftReconnects > 0) {
                closeSocket();
                if (super.createConnection(mRequestConnect)) {
                    mLeftReconnects = MAX_RECONNECTS;
                    break;
                }

                mLeftReconnects--;
            }

            // We tried, but the server isn't there anymore
            if (mLeftReconnects == 0) {
                Message msg = Message.obtain();
                msg.obj = new ClementineMessage(ErrorMessage.KEEP_ALIVE_TIMEOUT);
                msg.arg1 = PROCESS_PROTOC;
                mHandler.sendMessage(msg);
            }
        }
    }

    /**
     * Fire the event to all listeners
     *
     * @param status The current connection status
     */
    private void fireOnConnectionStatusChanged(ConnectionStatus status) {
        for (PlayerConnectionListener listener : mListeners) {
            listener.onConnectionStatusChanged(status);
        }
    }

    /**
     * Fire the event to all listeners
     */
    private void fireOnClementineMessageReceived(ClementineMessage msg) {
        for (PlayerConnectionListener listener : mListeners) {
            listener.onClementineMessageReceived(msg);
        }
    }

    /**
     * Set the ui Handler, to which the thread should talk to
     *
     * @param playerHandler The Handler
     */
    public void setUiHandler(Handler playerHandler) {
        this.mUiHandler = playerHandler;
    }

    /**
     * Set the last keep alive timestamp
     *
     * @param lastKeepAlive The time
     */
    public void setLastKeepAlive(long lastKeepAlive) {
        this.mLastKeepAlive = lastKeepAlive;
    }

    public long getStartTx() {
        return mStartTx;
    }

    public long getStartRx() {
        return mStartRx;
    }

    public long getStartTime() {
        return mStartTime;
    }

    public ClementineMessage getRequestConnect() {
        return mRequestConnect;
    }
}
