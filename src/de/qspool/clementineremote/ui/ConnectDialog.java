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

package de.qspool.clementineremote.ui;

import javax.jmdns.ServiceInfo;

import de.qspool.clementineremote.App;
import de.qspool.clementineremote.ClementineRemoteControlActivity;
import de.qspool.clementineremote.R;
import de.qspool.clementineremote.backend.Clementine;
import de.qspool.clementineremote.backend.elements.Disconnected;
import de.qspool.clementineremote.backend.elements.Disconnected.DisconnectReason;
import de.qspool.clementineremote.backend.mdns.ClementineMDnsDiscovery;
import de.qspool.clementineremote.backend.requests.RequestConnect;
import de.qspool.clementineremote.backend.requests.RequestDisconnect;
import de.qspool.clementineremote.utils.Utilities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View.OnClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The connect dialog
 */
public class ConnectDialog extends Activity {
	private final static int ANIMATION_DURATION = 2000;
	private Button mBtnConnect;
	private ImageButton mBtnSettings;
	private ImageButton mBtnClementine;
	private EditText mEtIp;
	private CheckBox mCbAutoConnect;
	
	ProgressDialog mPdConnect;
	
	private SharedPreferences mSharedPref;
    private ConnectDialogHandler mHandler = new ConnectDialogHandler(this);
    private int mAuthCode = 0;
    
    private ClementineMDnsDiscovery mClementineMDns;
    private AlphaAnimation mAlphaDown; 
    private AlphaAnimation mAlphaUp;
    private boolean mAnimationCancel;
    private boolean mFirstCall;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    // Remove title bar
	    this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    	
	    setContentView(R.layout.connectdialog);
	    
	    // Get the Layoutelements
	    mBtnConnect = (Button) findViewById(R.id.btnConnect);
	    mBtnConnect.setOnClickListener(oclConnect);
	    mBtnConnect.requestFocus();
	    
	    mBtnSettings = (ImageButton) findViewById(R.id.btnSettings);
	    mBtnSettings.setOnClickListener(oclSettings);
	    
	    mBtnClementine = (ImageButton) findViewById(R.id.btnClementineIcon);
	    mBtnClementine.setOnClickListener(oclClementine);
	    
	    // Setup the animation for the Clementine icon
	    mAlphaDown = new AlphaAnimation(1.0f, 0.3f);
	    mAlphaUp = new AlphaAnimation(0.3f, 1.0f);
	    mAlphaDown.setDuration(ANIMATION_DURATION);
	    mAlphaUp.setDuration(ANIMATION_DURATION);
	    mAlphaDown.setFillAfter(true);
	    mAlphaUp.setFillAfter(true);
	    mAlphaUp.setAnimationListener(mAnimationListener);
	    mAlphaDown.setAnimationListener(mAnimationListener);
	    mAnimationCancel = false;
	    
	    // Ip and Autoconnect
	    mEtIp = (EditText) findViewById(R.id.etIp);
	    mEtIp.setRawInputType(InputType.TYPE_CLASS_NUMBER);
	    mCbAutoConnect = (CheckBox) findViewById(R.id.cbAutoconnect);
	    
	    // Create a progress dialog
	    mPdConnect = new ProgressDialog(this);
	    mPdConnect.setCancelable(true);
	    mPdConnect.setOnCancelListener(oclProgressDialog);
	    mPdConnect.setMessage(getString(R.string.connectdialog_connecting));
	    
	    // Get old ip and auto-connect from shared prefences
	    mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
	    mEtIp.setText(mSharedPref.getString(App.SP_KEY_IP, ""));
	    mEtIp.setSelection(mEtIp.length());
	    mCbAutoConnect.setChecked(mSharedPref.getBoolean(App.SP_KEY_AC, false));
	    
	    // Get the last auth code
	    mAuthCode = mSharedPref.getInt(App.SP_LAST_AUTH_CODE, 0);
	}
	
	@Override 
	public void onResume() {
		super.onResume();
		
		// Check if we are still connected
		if (App.mClementineConnection == null 
		 || !App.mClementineConnection.isAlive()) {
			setResult(ClementineRemoteControlActivity.RESULT_RESTART);
			finish();
		} else {
		    // Set the handler
		    App.mClementineConnection.setUiHandler(mHandler);
		    
		    // Get the parameters
		    Bundle extras = getIntent().getExtras();
		    mFirstCall = extras.getBoolean(App.SP_KEY_AC);
		    
		    // Check if Autoconnect is enabled
		    if (mCbAutoConnect.isChecked() && extras.getBoolean(App.SP_KEY_AC)) {
		    	connect();
		    } else {
			    // mDNS Discovery
		    	mClementineMDns = new ClementineMDnsDiscovery(mHandler);
		    	mClementineMDns.discoverServices();
			}
		    
		    extras.putBoolean(App.SP_KEY_AC, true);
		}
	}

	private OnClickListener oclConnect = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// And connect
			connect();
		}
	};
	
	private OnClickListener oclSettings = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Intent settingsIntent = new Intent(ConnectDialog.this, ClementineRemoteSettings.class);
			startActivity(settingsIntent);
		}
	};
	
	private OnClickListener oclClementine = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// Only when we have Jelly Bean or higher
			if (!mClementineMDns.getServices().isEmpty()) {
				mAnimationCancel = true;
				final Dialog listDialog = new Dialog(ConnectDialog.this, R.style.Dialog_Transparent);
				listDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				listDialog.setContentView(R.layout.dialog_list);
				
				// Set the title
				TextView tvTitle = (TextView) listDialog.findViewById(R.id.tvListTitle);
				tvTitle.setText(R.string.connectdialog_services);
				
				// Set the close button
				Button closeButton = (Button) listDialog.findViewById(R.id.btnListClose);
				closeButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						listDialog.cancel();
					}
			    });
				
				// Set the list adapter
				ListView listView = (ListView) listDialog.findViewById(R.id.lvClementines);
				ArrayAdapter<String> adapter = new ArrayAdapter<String>(ConnectDialog.this,
						R.layout.dialog_list_item, mClementineMDns.getHosts());
				listView.setAdapter(adapter);
				listView.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						ServiceInfo service = mClementineMDns.getServices().get(position);
						// Insert the host
						String ip = service.getInet4Addresses()[0].toString().split("/")[1];
						mEtIp.setText(ip);
						
						// Update the port
						SharedPreferences.Editor editor = mSharedPref.edit();
						editor.putString(App.SP_KEY_PORT, 
								String.valueOf(service.getPort()));
						editor.commit();
						connect();
					}
				});
				
				listDialog.show();
			}
		}
	};
	
	private OnCancelListener oclProgressDialog = new OnCancelListener() {
		@Override
		public void onCancel(DialogInterface dialog) {
			// Create a new request
			RequestDisconnect r = new RequestDisconnect();
			
			// Move the request to the message
			Message msg = Message.obtain();
			msg.obj = r;
			
			// Send the request to the thread
			App.mClementineConnection.mHandler.sendMessage(msg);
		}
		
	};
	
	/**
	 * Connect to clementine
	 */
	private void connect() {
		// Save the data
		SharedPreferences.Editor editor = mSharedPref.edit();
		editor.putBoolean(App.SP_KEY_AC, mCbAutoConnect.isChecked());
		editor.putString(App.SP_KEY_IP, mEtIp.getText().toString());
		editor.putInt(App.SP_LAST_AUTH_CODE, mAuthCode);
		editor.commit();
		
		mPdConnect.show();
		// Get the port to connect to			
		int port = Integer.valueOf(mSharedPref.getString(App.SP_KEY_PORT, String.valueOf(Clementine.DefaultPort)));
					
		// Create a new connect request
		RequestConnect r = new RequestConnect(mEtIp.getText().toString(), port, mAuthCode);
		
		// Move the request to the message
		Message msg = Message.obtain();
		msg.obj = r;
		
		// Send the request to the thread
		App.mClementineConnection.mHandler.sendMessage(msg);
	}
	
	/**
	 * Show the user the dialog to enter the auth code
	 */
	void showAuthCodePromt() {
		final Dialog authCodeDialog = new Dialog(this, R.style.Dialog_Transparent);
		authCodeDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		authCodeDialog.setContentView(R.layout.dialog_auth_code);
		
		// Set the Views
		Button connectButton = (Button) authCodeDialog.findViewById(R.id.btnConnectAuth);
		final EditText etAuthCode = (EditText) authCodeDialog.findViewById(R.id.etAuthCode);
		connectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					mAuthCode = Integer.parseInt(etAuthCode.getText().toString());
					authCodeDialog.cancel();
	        	    connect();
				} catch (NumberFormatException e) {
					Toast.makeText(ConnectDialog.this, R.string.invalid_code, Toast.LENGTH_SHORT).show();
				}
			}
	    });
		// Show the keyboard directly
		authCodeDialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		authCodeDialog.show();
	}
	
	/**
	 * We connected to clementine successfully. Now open other view
	 */
	void connected() {
		mClementineMDns.stopServiceDiscovery();
		setResult(ClementineRemoteControlActivity.RESULT_CONNECT);
		finish();
	}
	
	/**
	 * We couldn't connect to clementine. Inform the user
	 */
	void noConnection() {
		// Check if we have not a local ip
		WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ip = wifiInfo.getIpAddress();
		
		// Get the current wifi state
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		
		if (!networkInfo.isConnected()) {
			Utilities.ShowMessageDialog(this, R.string.connectdialog_error, R.string.wifi_disabled);
		} else if (!Utilities.ToInetAddress(ip).isSiteLocalAddress()) {
			Utilities.ShowMessageDialog(this, R.string.connectdialog_error, R.string.no_private_ip);
		} else {
			Utilities.ShowMessageDialog(this, R.string.connectdialog_error, R.string.check_ip);
		}
	}
	
	/**
	 * We have an old Proto version. User has to update Clementine
	 */
	void oldProtoVersion() {
		Utilities.ShowMessageDialog(this, R.string.error_versions, R.string.old_proto);
	}
	
	/**
	 * Clementine closed the connection
	 * @param disconnected The object to work with
	 */
	void disconnected(Disconnected disconnected) {
		if (disconnected.getReason() == DisconnectReason.WRONG_AUTH_CODE) {
			showAuthCodePromt();
		}
	}
	
	/**
	 * A service was found. Now show a toast and animate the icon
	 */
	void serviceFound() {
		if (mClementineMDns.getServices().isEmpty()) {
			mBtnClementine.clearAnimation();
		} else {
			// Start the animation
			mBtnClementine.startAnimation(mAlphaDown);
		}
		
		// On the first call show a toast that we found a host
        if (mFirstCall) {
        	mFirstCall = false;
        	Toast.makeText(this, R.string.connectdialog_mdns_found, Toast.LENGTH_LONG).show();
        }
	}
	
	private AnimationListener mAnimationListener = new AnimationListener() {
		@Override
		public void onAnimationEnd(Animation animation) {
			if (!mAnimationCancel) {
				if (animation.equals(mAlphaDown)) {
					mBtnClementine.startAnimation(mAlphaUp);
				} else {
					mBtnClementine.startAnimation(mAlphaDown);
				}
			} else {
				mBtnClementine.clearAnimation();
			}
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
		}

		@Override
		public void onAnimationStart(Animation animation) {
		}

	};
}
