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

import de.qspool.clementineremote.App; 
import de.qspool.clementineremote.ClementineRemoteControlActivity;
import de.qspool.clementineremote.R;
import de.qspool.clementineremote.backend.event.OnConnectionClosedListener;
import de.qspool.clementineremote.backend.requests.RequestDisconnect;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

public class ClementineService extends Service {

	private NotificationCompat.Builder mNotifyBuilder;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			handleServiceAction(intent.getIntExtra(App.SERVICE_ID, 0));
		}

		return START_STICKY;
	}
	
	/**
	 * Handle the requests to the service
	 * @param action The action to perform
	 */
	private void handleServiceAction(int action) {
		switch (action) {
		case App.SERVICE_START:
			// Create a new instance
			if (App.mClementineConnection == null) {
				App.mClementineConnection = new ClementineConnection(this);
			}
			setupNotification(true);
			App.mClementineConnection.setNotificationBuilder(mNotifyBuilder);
			App.mClementineConnection.setOnConnectionClosedListener(occl);
			App.mClementineConnection.start();
			break;
		case App.SERVICE_CONNECTED:
			startForeground(App.NOTIFY_ID, mNotifyBuilder.build());
			break;
		case App.SERVICE_DISCONNECTED:
			stopForeground(true);
			try {
				App.mClementineConnection.join();
			} catch (InterruptedException e) {}
			App.mClementineConnection = null;
			break;		
		default: break;
		}
	}
	
	@Override
	public void onDestroy() {
		stopForeground(true);
		if (App.mClementine.isConnected()) {
			// Create a new request
			RequestDisconnect r = new RequestDisconnect();
			
			// Move the request to the message
			Message msg = Message.obtain();
			msg.obj = r;
			
			// Send the request to the thread
			App.mClementineConnection.mHandler.sendMessage(msg);
		}
		try {
			App.mClementineConnection.join();
		} catch (InterruptedException e) {}
		App.mClementineConnection = null;
	}
	
	/**
	 * Setup the Notification
	 */
	private void setupNotification(boolean ongoing) {
	    mNotifyBuilder = new NotificationCompat.Builder(App.mApp);
	    mNotifyBuilder.setSmallIcon(R.drawable.ic_launcher);
	    mNotifyBuilder.setOngoing(ongoing);
	    
	    // Set the result intent
	    Intent resultIntent = new Intent(App.mApp, ClementineRemoteControlActivity.class);
	    resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
	    // Create a TaskStack, so the app navigates correctly backwards
	    TaskStackBuilder stackBuilder = TaskStackBuilder.create(App.mApp);
	    stackBuilder.addParentStack(ClementineRemoteControlActivity.class);
	    stackBuilder.addNextIntent(resultIntent);
	    PendingIntent resultPendingintent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
	    mNotifyBuilder.setContentIntent(resultPendingintent);
	}
	
	private OnConnectionClosedListener occl = new OnConnectionClosedListener() {
		
		@Override
		public void onConnectionClosed() {
			Intent mServiceIntent = new Intent(ClementineService.this, ClementineService.class);
	    	mServiceIntent.putExtra(App.SERVICE_ID, App.SERVICE_DISCONNECTED);
	    	startService(mServiceIntent);
		}
	};
}
