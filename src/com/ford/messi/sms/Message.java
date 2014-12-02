package com.ford.messi.sms;

import com.ford.messi.applink.AppLinkApplication;

import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.PhoneLookup;
import android.util.Log;

public class Message {
	
	private final String TAG = this.getClass().getSimpleName();
	
	private String sender;
	private String title;
	private String body;
	private int currentIndex;
	
	public Message() {		
	}
	
	public Message(String sender, String body, int currentIndex) {
		this.sender = sender;
		setTitle(sender);
		this.body = body;
		this.currentIndex = currentIndex;
	}
	
	public String getSender() {
		return sender;
	}
	
	public void setSender(String sender) {
		this.sender = sender;
	}
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String sender) {
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(sender));
		Log.d(TAG, "uri: " + uri);
		Cursor c = AppLinkApplication.getApplication().getContentResolver().query(uri, 
				new String[]{PhoneLookup.DISPLAY_NAME}, null, null, null);		
		if (c != null) {
			while (c.moveToNext()) {
		        title = c.getString(0);
			}
		} else {
			title = sender;
		}
		if (title == null) {
			title = sender;
		}
		Log.d(TAG, "Msg title: " + getTitle());
	}
	
	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public int getCurrentIndex() {
		return currentIndex;
	}
	
	public void setCurrentIndex(int currentIndex) {
		this.currentIndex = currentIndex;
	}
	
	
	public String toString() {
		return "Sender: " + sender + "; Title: " + title + 
				"; Body: " + body + "; Current index: " + currentIndex;
	}

}