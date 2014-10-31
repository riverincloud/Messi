package com.ford.messi.sms;

import java.util.ArrayList;

import com.ford.messi.R;
import com.ford.messi.applink.AppLinkActivity;
import com.ford.messi.applink.AppLinkApplication;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends AppLinkActivity {
	
	private final String TAG = this.getClass().getSimpleName();
	
	private static MainActivity main = null;	
	ArrayList<Message> messageList = new ArrayList<Message>();
	private ArrayAdapter<Message> adapter;
	
	
	public static MainActivity getMain() {
		return main;
	}
	
	public ArrayList<Message> getMessageList() {
		return messageList;
	}
	
	
	public void updateMessageList(ArrayList<Message> messageList) {
		Log.d(TAG, "updateMessageList called;");
		this.messageList = messageList;
		for (Message m : this.messageList) {
			Log.d(TAG, "Message object m: " + m.toString());
		}
		adapter.notifyDataSetChanged();		
	}
	

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        main = this;
        
        setContentView(R.layout.activity_main);        
        ListView listView = (ListView) findViewById(R.id.listview);
        adapter = new ArrayAdapter<Message>(this,
                android.R.layout.simple_list_item_1, messageList);
        listView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.reset:
	        	AppLinkApplication.getApplication().endSyncProxyInstance();
	        	AppLinkApplication.getApplication().startSyncProxyService();
	            return true;
	        case R.id.about:
	        	AppLinkApplication.getApplication().showAppVersion(this);
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, "onDestroy called");
		super.onDestroy();
	}
	
}
