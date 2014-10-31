package com.ford.messi.sms;

import java.util.ArrayList;

import com.ford.messi.applink.AppLinkService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

public class SmsReceiver extends BroadcastReceiver {	
	
	private final String TAG = this.getClass().getSimpleName();
	
	
	@Override
	public void onReceive(Context context, Intent intent) {		
		Log.d(TAG, "onReceive called; sms intent action: " + intent.getAction());
		if (intent.getAction().compareTo(Telephony.Sms.Intents.SMS_RECEIVED_ACTION) == 0) {
			ArrayList<Message> messageList = MainActivity.getMain().getMessageList();
			int startIndex = messageList.size();		
			SmsMessage[] smsArray = Telephony.Sms.Intents.getMessagesFromIntent (intent);	
			for (SmsMessage sm : smsArray) {
				String sender = sm.getDisplayOriginatingAddress();
				String body = sm.getDisplayMessageBody();
				int currentIndex = messageList.size();
				Message m = new Message(sender, body, currentIndex);
				messageList.add(m);
			}
			Log.d(TAG, "Updated messageList size: " + messageList.size());
		
			MainActivity.getMain().updateMessageList(messageList);
			AppLinkService.getService().setMessageList(messageList);
			AppLinkService.getService().alertMessage(startIndex);
		}
    }	

}
