package com.ford.messi.applink;

import android.content.Intent;
import android.util.Log;

import com.ford.messi.sms.LockScreenActivity;
import com.ford.syncV4.proxy.LockScreenManager;

public class LockManager extends LockScreenManager {
	
	private final String TAG = this.getClass().getSimpleName();
	
	public void showLockScreen() {
		// only show the lockscreen if main activity is currently on top
		// else, wait until onResume() to show the lockscreen so it doesn't 
		// pop-up while a user is using another app on the phone
		if (AppLinkApplication.getCurrentActivity() != null) {
			if (((AppLinkActivity) AppLinkApplication.getCurrentActivity()).isActivityonTop() == true) {
				Intent i = new Intent(AppLinkApplication.getApplication(), LockScreenActivity.class);
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				i.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
				AppLinkApplication.getApplication().startActivity(i);
				Log.d(TAG, "LockScreen is shown.");
			}
		}
	}

	public void clearLockScreen() {
		if (LockScreenActivity.getInstance() != null) {  
			LockScreenActivity.getInstance().exit();
			Log.d(TAG, "LockScreen is cleared.");
		}
	}

}
