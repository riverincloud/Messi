package com.ford.messi.sms;

import com.ford.messi.R;
import android.app.Activity;
import android.os.Bundle;

public class LockScreenActivity extends Activity {
	
	private static LockScreenActivity instance = null;
	
	
	public static LockScreenActivity getInstance() {
    	return instance;
    }
	
	public void exit() {
    	super.finish();
    }
	
		
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lockscreen);
		LockScreenActivity.instance = this;
    }
    
    // Disable back button on lockscreen
    @Override
    public void onBackPressed() {
    }
    
    @Override
    public void onDestroy() {
		LockScreenActivity.instance = null;
		super.onDestroy();
    }
    
}