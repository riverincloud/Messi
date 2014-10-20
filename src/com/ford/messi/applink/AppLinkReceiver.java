package com.ford.messi.applink;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AppLinkReceiver extends BroadcastReceiver {
	
	public void onReceive(Context context, Intent intent) {		
		// Start the AppLinkService on BT connection
		if (intent.getAction().compareTo(BluetoothDevice.ACTION_ACL_CONNECTED) == 0) {
			AppLinkApplication app = AppLinkApplication.getApplication();
			if (app != null) {
				app.startSyncProxyService();
			}
		}
		// Stop the AppLinkService on BT disconnection, 
		else if (intent.getAction().compareTo(BluetoothDevice.ACTION_ACL_DISCONNECTED) == 0) {
			AppLinkApplication app = AppLinkApplication.getApplication();
			AppLinkService als = AppLinkService.getService();			
			if (app != null && als != null) {
				app.endSyncProxyService();
			}
		}
		else if (intent.getAction().equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
			// signal your service to stop audio playback
		}
	}
}