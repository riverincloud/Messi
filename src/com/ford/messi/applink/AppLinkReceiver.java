package com.ford.messi.applink;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AppLinkReceiver extends BroadcastReceiver {
	
	private final String TAG = this.getClass().getSimpleName();
	
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG,"onReceive called; AppLink intent action: " + intent.getAction());
		if (intent.getAction().compareTo(BluetoothDevice.ACTION_ACL_CONNECTED) == 0) {
			// Start the AppLinkService on BT connection
			AppLinkApplication app = AppLinkApplication.getApplication();
			if (app != null) {
				app.startSyncProxyService();
			}
		} else if (intent.getAction().compareTo(BluetoothDevice.ACTION_ACL_DISCONNECTED) == 0) {
			// Stop the AppLinkService on BT disconnection
			AppLinkApplication app = AppLinkApplication.getApplication();
			AppLinkService als = AppLinkService.getService();			
			if (app != null && als != null) {
				app.endSyncProxyService();
			}
		} else if (intent.getAction().equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
			// Signal your service to stop audio playback
		}
	}
	
}