package com.ford.messi.applink;

import java.util.ArrayList;
import java.util.Vector;

import com.ford.messi.sms.Message;
import com.ford.messi.sms.PresetReply;
import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.exception.SyncExceptionCause;
import com.ford.syncV4.proxy.SyncProxyALM;
import com.ford.syncV4.proxy.interfaces.IProxyListenerALM;
import com.ford.syncV4.proxy.rpc.*;
import com.ford.syncV4.proxy.rpc.enums.ButtonName;
import com.ford.syncV4.proxy.rpc.enums.ButtonPressMode;
import com.ford.syncV4.proxy.rpc.enums.InteractionMode;
import com.ford.syncV4.proxy.rpc.enums.LockScreenStatus;
import com.ford.syncV4.proxy.rpc.enums.SoftButtonType;
import com.ford.syncV4.proxy.rpc.enums.SpeechCapabilities;
import com.ford.syncV4.proxy.rpc.enums.SyncDisconnectedReason;
import com.ford.syncV4.proxy.rpc.enums.SystemAction;
import com.ford.syncV4.proxy.rpc.enums.TextAlignment;
import com.ford.syncV4.util.DebugTool;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;

public class AppLinkService extends Service implements IProxyListenerALM {
	
	private final String TAG = this.getClass().getSimpleName();
	
	// variable used to increment correlation ID for every request sent to SYNC
	public int autoIncCorrId = 0;
	// variable to contain the current state of the service
	private static AppLinkService service = null;
	// variable to access the BluetoothAdapter
	private BluetoothAdapter mBtAdapter;
	// variable to create and call functions of the SyncProxy
	private SyncProxyALM proxy = null;	
	private Handler mHandler = new Handler();
	// Service shutdown timing constants
	private static final int CONNECTION_TIMEOUT = 60000;
	private static final int STOP_SERVICE_DELAY = 5000;
	
	private ArrayList<Message> messageList = new ArrayList<Message>();
	private Message currentMessage = null;
    private boolean alerting = false;
	
    
	public static AppLinkService getService() {
		return service;
	}
		
	public SyncProxyALM getProxy() {
		return proxy;
	}
	
	public void setMessageList(ArrayList<Message> messageList) {
		this.messageList = messageList;
	}
	
	/**
	 *  Runnable that stops this service if there hasn't been a connection to SYNC
	 *  within a reasonable amount of time since ACL_CONNECT.
	 */
	private Runnable mCheckConnectionRunnable = new Runnable() {
		@Override
		public void run() {
			Boolean stopService = true;
			// If the proxy has connected to SYNC, do NOT stop the service
			if (proxy != null && proxy.getIsConnected()) {
				stopService = false;
			}
			if (stopService) {
				mHandler.removeCallbacks(mCheckConnectionRunnable);
				mHandler.removeCallbacks(mStopServiceRunnable);
				stopSelf();
			}
		}
	};
	
	/**
	 * Runnable that stops this service on ACL_DISCONNECT after a short time delay.
	 * This is a workaround until some synchronization issues are fixed within the proxy.
	 */
	private Runnable mStopServiceRunnable = new Runnable() {
		@Override
		public void run() {
			// As long as the proxy is null or not connected to SYNC, stop the service
			if (proxy == null || !proxy.getIsConnected()) {
				mHandler.removeCallbacks(mCheckConnectionRunnable);
				mHandler.removeCallbacks(mStopServiceRunnable);
				stopSelf();
			}
		}
	};
	
	/**
	 * Queue's a runnable that stops the service after a small delay,
	 * unless the proxy manages to reconnects to SYNC.
	 */
	public void stopService() {
        mHandler.removeCallbacks(mStopServiceRunnable);
        mHandler.postDelayed(mStopServiceRunnable, STOP_SERVICE_DELAY);
	}

	public void startProxy() {
		if (proxy == null) {
			try {
				proxy = new SyncProxyALM(this, "Messi", true, "438316431");
			} catch (SyncException e) {
				e.printStackTrace();
				// error creating proxy, returned proxy = null
				if (proxy == null) {
					stopSelf();
				}
			}
		}
	}
	
	public void disposeSyncProxy() {
		if (proxy != null) {
			try {
				proxy.dispose();
			} catch (SyncException e) {
				e.printStackTrace();
			}
			proxy = null;
			AppLinkApplication.getLockManager().clearLockScreen();
		}
	}
	
	public void reset() {
		if (proxy != null) {
			try {
				proxy.resetProxy();
			} catch (SyncException e1) {
				e1.printStackTrace();
				//something goes wrong, & the proxy returns as null, stop the service.
				// do not want a running service with a null proxy
				if (proxy == null) {
					stopSelf();
				}
			}
		} else {
			startProxy();
		}
	}	
	
	public void onProxyClosed(String info, Exception e) {
		AppLinkApplication.getLockManager().setHMILevel(null);
		AppLinkApplication.getLockManager().clearLockScreen();
		
		if ((((SyncException) e).getSyncExceptionCause() != SyncExceptionCause.SYNC_PROXY_CYCLED))	{
			if (((SyncException) e).getSyncExceptionCause() != SyncExceptionCause.BLUETOOTH_DISABLED) {
				Log.d(TAG, "onProxyClosed called; Reset proxy");
				reset();
			}
		}
	}
	
	
	private void addSubMenus() {
		try {
			proxy.addSubMenu(1, "Inbox", autoIncCorrId++);
		} catch (SyncException e) {
			DebugTool.logError("Failed to send addSubMenu", e);
		}
	}
	
	private void subcribeButtons() {
		try {
	        proxy.subscribeButton(ButtonName.OK, autoIncCorrId++);
	        proxy.subscribeButton(ButtonName.SEEKLEFT, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.SEEKRIGHT, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.TUNEUP, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.TUNEDOWN, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_1, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_2, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_3, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_4, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_5, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_6, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_7, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_8, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_9, autoIncCorrId++);
			proxy.subscribeButton(ButtonName.PRESET_0, autoIncCorrId++);
		} catch (SyncException e) {}
	}
	
	private SoftButton createSoftButton(int id, String text, SoftButtonType type, 
			SystemAction systemAction, boolean isHighlighted) {
		SoftButton sb = new SoftButton();
		sb.setSoftButtonID(id);
		sb.setText(text);	
		sb.setType(type);
		sb.setSystemAction(systemAction);
		sb.setIsHighlighted(isHighlighted);
		return sb;
	}
	
	private Vector<SoftButton> setUpSoftButtons(SoftButtonsGroup group) {		
		SoftButton sb1;
		SoftButton sb2;
		SoftButton sb3;
		SoftButton sb4;
		Vector<SoftButton> softButtons = new Vector<SoftButton>();
		
		switch(group) {
			case MSG_ALERT:
				Log.d(TAG, "setUpSoftButtons called for MSG_ALERT");
				sb1 = createSoftButton(1, "Listen", SoftButtonType.SBT_TEXT, SystemAction.DEFAULT_ACTION, false);
				sb2 = createSoftButton(2, "View", SoftButtonType.SBT_TEXT, SystemAction.DEFAULT_ACTION, false);
				sb3 = createSoftButton(3, "Ignore", SoftButtonType.SBT_TEXT, SystemAction.DEFAULT_ACTION, false);
		        softButtons.add(sb1);
		 		softButtons.add(sb2);
		 		softButtons.add(sb3);
				break;
			case MSG_ALERT_LISTEN:
				Log.d(TAG, "setUpSoftButtons called for MSG_ALERT_LISTEN");
				sb1 = createSoftButton(1, "Listen", SoftButtonType.SBT_TEXT, SystemAction.DEFAULT_ACTION, true);
				sb2 = createSoftButton(2, "View", SoftButtonType.SBT_TEXT, SystemAction.DEFAULT_ACTION, false);
				sb3 = createSoftButton(3, "Ignore", SoftButtonType.SBT_TEXT, SystemAction.DEFAULT_ACTION, false);
		        softButtons.add(sb1);
		 		softButtons.add(sb2);
		 		softButtons.add(sb3);
				break;
			case MSG_DETAILS:
				Log.d(TAG, "setUpSoftButtons called for MSG_DETAILS");
				sb1 = createSoftButton(4, "Listen", SoftButtonType.SBT_TEXT, SystemAction.DEFAULT_ACTION, false);
				sb2 = createSoftButton(7, "Next", SoftButtonType.SBT_TEXT, SystemAction.DEFAULT_ACTION, false);
		    	sb3 = createSoftButton(5, "Reply", SoftButtonType.SBT_TEXT, SystemAction.DEFAULT_ACTION, false);	
		    	sb4 = createSoftButton(6, "Call", SoftButtonType.SBT_TEXT, SystemAction.DEFAULT_ACTION, false);		    	
		        softButtons.add(sb1);
		 		softButtons.add(sb2);
		 		softButtons.add(sb3);
		 		softButtons.add(sb4);
				break;
			case MSG_DETAILS_LISTEN:
				Log.d(TAG, "setUpSoftButtons called for MSG_DETAILS_LISTEN");
				sb1 = createSoftButton(4, "Listen", SoftButtonType.SBT_TEXT, SystemAction.DEFAULT_ACTION, true);
				sb2 = createSoftButton(7, "Next", SoftButtonType.SBT_TEXT, SystemAction.DEFAULT_ACTION, false);
		    	sb3 = createSoftButton(5, "Reply", SoftButtonType.SBT_TEXT, SystemAction.DEFAULT_ACTION, false);	
		    	sb4 = createSoftButton(6, "Call", SoftButtonType.SBT_TEXT, SystemAction.DEFAULT_ACTION, false);		    	
		        softButtons.add(sb1);
		 		softButtons.add(sb2);
		 		softButtons.add(sb3);
		 		softButtons.add(sb4);
				break;
			default:
				return softButtons;
		}
		return softButtons;
	}
	
	private Choice createChoice(int id, String name) {
		Choice c = new Choice();
		c.setChoiceID(id);
		c.setMenuName(name);
		Vector<String> v = new Vector<String>();
		v.add(name);
		c.setVrCommands(v);
		return c;
	}
	
	private Vector<TTSChunk> convertPhoneNumber(String phoneNumber) {
		Vector<TTSChunk> chunks = new Vector<TTSChunk>();
		TTSChunk prefix = new TTSChunk();
		prefix.setText("Incoming message from ");
		prefix.setType(SpeechCapabilities.TEXT);
		chunks.add(prefix);
		char[] phoneNumberArray = phoneNumber.toCharArray();
		for (char c : phoneNumberArray) {
			if (c != '+') {
				String s = String.valueOf(c);
				TTSChunk chunk = new TTSChunk();
				chunk.setText(s);
				chunk.setType(SpeechCapabilities.TEXT);
				chunks.add(chunk);
			}
		}
		Log.d(TAG, "Phone number TTSChunk: " + chunks.toString());
		return chunks;
	}
	
	public void alertMessage(int startIndex) {
		if (alerting == false) {
			currentMessage = messageList.get(startIndex);
			try {
				proxy.alert(convertPhoneNumber(currentMessage.getSender()), 
						"Incoming msg from ", currentMessage.getSender(), null, 
						true, 10000, setUpSoftButtons(SoftButtonsGroup.MSG_ALERT), autoIncCorrId++);
				Log.d(TAG, "alertMessage - Phone number chunk" + convertPhoneNumber(currentMessage.getSender()));
				alerting = true;
			} catch (SyncException e) {
				DebugTool.logError("Failed to send Alert", e);
			}
		}
	}
	
	private void viewMessage(int index, SoftButtonsGroup group) {
		currentMessage = messageList.get(index);
		Log.d(TAG, "Selected message: " + currentMessage.toString());
		try {
			proxy.scrollablemessage(index+1 + "/" + messageList.size() + " " + 
					currentMessage.getSender() + ": " + '\n' +	currentMessage.getBody(), 10000, 
					setUpSoftButtons(group), autoIncCorrId++);	
		} catch (SyncException e) {
			DebugTool.logError("Failed to send ScrollableMessage", e);
		}		
	}
	
	private void quickReply(String sender, PresetReply reply) {
		try {
			SmsManager smsManager = SmsManager.getDefault();
			smsManager.sendTextMessage(sender, null, reply.getValue(), null, null);
		} catch (Exception e) {
			DebugTool.logError("Failed to send Reply", e);
		}
	}
	
	private void callBack(String sender) {
		Log.d(TAG, "callBack called; sender: " + sender);
	    Intent intent = new Intent(Intent.ACTION_CALL);
	    intent.setData(Uri.parse("tel:" + sender));
	    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    if (intent.resolveActivity(getPackageManager()) != null) {
	    	Log.d(TAG, "app will start ACTION_CALL");
	    	AppLinkApplication.getApplication().startActivity(intent);
	    }
	}
	
	
	@Override
	public void onCreate() {
		super.onCreate();
		service = this;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// Remove any previous stop service runnables that could be from a recent ACL Disconnect
		mHandler.removeCallbacks(mStopServiceRunnable);
		// Start the proxy when the service starts
        if (intent != null) {
        	mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    		if (mBtAdapter != null) {
    			if (mBtAdapter.isEnabled()) {
    				startProxy();
    			}
    		}
		}        
        // Queue the check connection runnable to stop the service if no connection is made
        mHandler.removeCallbacks(mCheckConnectionRunnable);
        mHandler.postDelayed(mCheckConnectionRunnable, CONNECTION_TIMEOUT);
        
        return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		disposeSyncProxy();
		AppLinkApplication.getLockManager().clearLockScreen();
		service = null;		
		super.onDestroy();
	}
   
	@Override
	public void onOnHMIStatus(OnHMIStatus onHMIStatus) {
		switch(onHMIStatus.getSystemContext()) {
			case SYSCTXT_MAIN:
				Log.i(TAG, "SYSCTXT_MAIN");
				break;
			case SYSCTXT_VRSESSION:
				Log.i(TAG, "SYSCTXT_VRSESSION");
				break;
			case SYSCTXT_MENU:
				Log.i(TAG, "SYSCTXT_MENU");
				break;
			default:
				return;
		}

		switch(onHMIStatus.getAudioStreamingState()) {
			case AUDIBLE:
				Log.i(TAG, "AUDIBLE");
				// play audio if applicable
				break;
			case NOT_AUDIBLE:
				Log.i(TAG, "NOT_AUDIBLE");
				// pause/stop/mute audio if applicable
				break;
			default:
				return;
		}
		  
		switch(onHMIStatus.getHmiLevel()) {
			case HMI_FULL:
				Log.i(TAG, "HMI_FULL");				
						
				// subscribe to buttons
				subcribeButtons();
				
				if (onHMIStatus.getFirstRun()) {
					Log.i(TAG, "HMI FirstRun");
					// Set HelpPrompt
					// Set TimeoutPrompt					
					// add sub menus
					addSubMenus();	
				
					// send welcome message if applicable
					try {
						proxy.show("Welcome!", "Ready to receive", "SMS message", null, null, null, null, TextAlignment.LEFT_ALIGNED, autoIncCorrId++);
					} catch (SyncException e) {
						DebugTool.logError("Failed to send show Welcome", e);
					}
				} else if (messageList.size() == 0) {
					try {
						proxy.show("SMS inbox empty.", "", TextAlignment.LEFT_ALIGNED, autoIncCorrId++);
					} catch (SyncException e) {
						DebugTool.logError("Failed to send show Inbox empty", e);
					}
				} else {				
					for (Message m : messageList) {
						try {
							proxy.addCommand(m.getCurrentIndex(), m.getCurrentIndex()+1 + " " + m.getSender(), 
									1, m.getCurrentIndex()+1, null, autoIncCorrId++);
						} catch(SyncException e) {
							DebugTool.logError("Failed to send addCommand", e);
						}
					}
					try {
						proxy.show("More > Inbox", "", TextAlignment.LEFT_ALIGNED, autoIncCorrId++);
					} catch (SyncException e) {
						DebugTool.logError("Failed to send show More > Inbox", e);
					}
				}
				break;
			case HMI_LIMITED:
				Log.i(TAG, "HMI_LIMITED");
				break;
			case HMI_BACKGROUND:
				Log.i(TAG, "HMI_BACKGROUND");
				break;
			case HMI_NONE:
				Log.i(TAG, "HMI_NONE");
				break;
			default:
				return;
		}
	}

	@Override
	public void onOnDriverDistraction(OnDriverDistraction notification) {
		Log.d(TAG, "OnDriverDistraction: " + notification.getState());
	}
	
	@Override
	public void onError(String info, Exception e) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void onGenericResponse(GenericResponse response) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void onOnCommand(OnCommand notification) {
		Log.d(TAG, "OnCommand notification - cmdID: " + notification.getCmdID());
		viewMessage(notification.getCmdID(), SoftButtonsGroup.MSG_DETAILS);
	}
	
	@Override
	public void onAddCommandResponse(AddCommandResponse response) {
		Log.d(TAG, "AddCommandResponse: " + response.getResultCode());
		if (response.getSuccess()) {
			
		}
	}
	
	@Override
	public void onAddSubMenuResponse(AddSubMenuResponse response) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void onCreateInteractionChoiceSetResponse(
			CreateInteractionChoiceSetResponse response) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void onAlertResponse(AlertResponse response) {
		Log.d(TAG, "Alert Resppnse: " + response.getResultCode());
		if (response.getSuccess()== true) {
			alerting = false;
		}
	}
	
	@Override
	public void onDeleteCommandResponse(DeleteCommandResponse response) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void onDeleteInteractionChoiceSetResponse(
			DeleteInteractionChoiceSetResponse response) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void onDeleteSubMenuResponse(DeleteSubMenuResponse response) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void onPerformInteractionResponse(PerformInteractionResponse response) {
		try {
			if (response.getChoiceID() == 1) {
				quickReply(currentMessage.getSender(), PresetReply.DRIVING);
			} else if (response.getChoiceID() == 2) {
				quickReply(currentMessage.getSender(), PresetReply.CALL_LATER);
			}
			proxy.alert("Quick reply sent", "Quick reply sent.", null, false, 3000, autoIncCorrId++);
			alerting = true;
		} catch (Exception e) {
			DebugTool.logError("Failed to PerformInteraction quick reply", e);
		}
	}
	
	@Override
	public void onResetGlobalPropertiesResponse(
			ResetGlobalPropertiesResponse response) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void onSetGlobalPropertiesResponse(SetGlobalPropertiesResponse response) {
	}
	
	@Override
	public void onSetMediaClockTimerResponse(SetMediaClockTimerResponse response) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void onShowResponse(ShowResponse response) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void onSpeakResponse(SpeakResponse response) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void onOnButtonEvent(OnButtonEvent notification) {
		Log.d(TAG, "Button: " + notification.getButtonName() + "; Event Mode: " + notification.getButtonEventMode());
		// TODO Auto-generated method stub	
	}
	
	
	@Override
	public void onOnButtonPress(OnButtonPress notification) {
		Log.d(TAG, "OnButtonPress notification - " +
				"Button: " + notification.getButtonName() + "; Event Mode: " + notification.getButtonPressMode());
		
		if (notification.getButtonPressMode() == ButtonPressMode.SHORT) {
			if (notification.getCustomButtonName() == 1) {
				Log.d(TAG, "Soft button 1-Listen down");
				try {
					proxy.alert(currentMessage.getBody(), 
							"Incoming msg from ", currentMessage.getSender(), null, 
							false, 10000, setUpSoftButtons(SoftButtonsGroup.MSG_ALERT_LISTEN), autoIncCorrId++);
					alerting = true;
				} catch (SyncException e) {
					DebugTool.logError("Failed to send Alert", e);
				}
			} else if (notification.getCustomButtonName() == 2) {
				Log.d(TAG, "Soft button 2-View down");
				viewMessage(currentMessage.getCurrentIndex(), SoftButtonsGroup.MSG_DETAILS);	
			} else if (notification.getCustomButtonName() == 3) {
				Log.d(TAG, "Soft button 3-Ignore down");
				alerting = false;
				currentMessage = null;
			} else if (notification.getCustomButtonName() == 4) {
				Log.d(TAG, "Soft button 4-Listen down");
				try {
					proxy.speak(currentMessage.getBody(), autoIncCorrId++);
				} catch (SyncException e) {
					DebugTool.logError("Failed to send Speak", e);
				}
				viewMessage(currentMessage.getCurrentIndex(), SoftButtonsGroup.MSG_DETAILS_LISTEN);
			} else if (notification.getCustomButtonName() == 5) {
				Log.d(TAG, "Soft button 5-Reply down");
				Choice c1 = createChoice(1, "Driving");
				Choice c2 = createChoice(2, "Call later");
				Vector<Choice> choiceSet = new Vector<Choice>();
				choiceSet.add(c1);
				choiceSet.add(c2);
				try {
					proxy.createInteractionChoiceSet(choiceSet, 1, autoIncCorrId++);
				} catch (SyncException e) {
					DebugTool.logError("Failed to create interaction choice set", e);
				}
				try {
					proxy.performInteraction("Please select a quick reply", "Please select a quick reply:", 
					1, null, null, InteractionMode.BOTH, null, null, autoIncCorrId++);
				} catch (SyncException e) { 
					DebugTool.logError("Failed to perform interaction", e);
				}
			} else if (notification.getCustomButtonName() == 6) {
				Log.d(TAG, "Soft button 6-Call down");
				callBack(currentMessage.getSender());
			} else if (notification.getCustomButtonName() == 7) {
				Log.d(TAG, "Soft button 7-Next down");
				Log.d(TAG, "currentIndex: " + currentMessage.getCurrentIndex() + "; messageList size: " + messageList.size());
				if (currentMessage.getCurrentIndex()+1 == messageList.size()) {
					viewMessage(0, SoftButtonsGroup.MSG_DETAILS);
				} else {
					viewMessage(currentMessage.getCurrentIndex()+1, SoftButtonsGroup.MSG_DETAILS);
				}
			}
		} 
	}
	
	
	@Override
	public void onSubscribeButtonResponse(SubscribeButtonResponse response) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void onUnsubscribeButtonResponse(UnsubscribeButtonResponse response) {
		// TODO Auto-generated method stub	
	}
	
	@Override
	public void onOnPermissionsChange(OnPermissionsChange notification) {
		// TODO Auto-generated method stub	
	}
	
	@Override
	public void onOnTBTClientState(OnTBTClientState notification) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		  return null;
	}
	 
	
	@Override
	public void onSubscribeVehicleDataResponse(SubscribeVehicleDataResponse response) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onUnsubscribeVehicleDataResponse(
			UnsubscribeVehicleDataResponse response) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onGetVehicleDataResponse(GetVehicleDataResponse response) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onReadDIDResponse(ReadDIDResponse response) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onGetDTCsResponse(GetDTCsResponse response) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onOnVehicleData(OnVehicleData notification) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onPerformAudioPassThruResponse(PerformAudioPassThruResponse response) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onEndAudioPassThruResponse(EndAudioPassThruResponse response) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onOnAudioPassThru(OnAudioPassThru notification) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onPutFileResponse(PutFileResponse response) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onDeleteFileResponse(DeleteFileResponse response) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onListFilesResponse(ListFilesResponse response) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onSetAppIconResponse(SetAppIconResponse response) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onScrollableMessageResponse(ScrollableMessageResponse response) {
		Log.d(TAG, "Alert Resppnse: " + response.getResultCode());
		if (response.getSuccess()== true) {
			alerting = false;
		}
	}
	
	@Override
	public void onChangeRegistrationResponse(ChangeRegistrationResponse response) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onSetDisplayLayoutResponse(SetDisplayLayoutResponse response) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onOnLanguageChange(OnLanguageChange notification) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onSliderResponse(SliderResponse response) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDiagnosticMessageResponse(DiagnosticMessageResponse arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onOnHashChange(OnHashChange arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onOnKeyboardInput(OnKeyboardInput arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onOnLockScreenNotification(OnLockScreenStatus notification) {
		Log.d(TAG, "OnLockScreenStatus: " + notification.getShowLockScreen());
		if (notification.getShowLockScreen() == LockScreenStatus.REQUIRED || 
				notification.getShowLockScreen() == LockScreenStatus.OPTIONAL) {
			AppLinkApplication.getLockManager().showLockScreen();
		} else {
			AppLinkApplication.getLockManager().clearLockScreen();
		}
	}

	@Override
	public void onOnSystemRequest(OnSystemRequest arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onOnTouchEvent(OnTouchEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProxyClosed(String arg0, Exception arg1,
			SyncDisconnectedReason arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSystemRequestResponse(SystemRequestResponse arg0) {
		// TODO Auto-generated method stub
		
	}
	
}
