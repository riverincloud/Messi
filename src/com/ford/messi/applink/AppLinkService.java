package com.ford.messi.applink;

import java.util.ArrayList;
import java.util.Vector;

import com.ford.messi.sms.MainActivity;
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
import com.ford.syncV4.proxy.rpc.enums.TextAlignment;
import com.ford.syncV4.util.DebugTool;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;

public class AppLinkService extends Service implements IProxyListenerALM {
	
	private final String TAG = this.getClass().getSimpleName();
    
	private ArrayList<Message> messageList = new ArrayList<Message>();
	private Message currentMessage = null;
	private int startIndex;
    
    boolean audible = true;
    boolean alerting = false;
	
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
				proxy = new SyncProxyALM(this, "Hello AppLink", true, "438316430");
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
	
	public void subcribeButtons() {
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
	
	public SoftButton createSoftButton(int id, String text, SoftButtonType type) {
		SoftButton sb = new SoftButton();
		sb.setSoftButtonID(id);
		sb.setText(text);	
		sb.setType(type);
		return sb;
	}
	public Vector<SoftButton> setUpSoftButtons(SoftButton sb1, SoftButton sb2, SoftButton sb3) {
		Vector<SoftButton> softButtons = new Vector<SoftButton>();
        softButtons.add(sb1);
 		softButtons.add(sb2);
 		softButtons.add(sb3);
 		return softButtons;
	}
   
	public void onProxyClosed(String info, Exception e) {
		AppLinkApplication.getLockManager().setHMILevel(null);
		AppLinkApplication.getLockManager().clearLockScreen();
		
		if ((((SyncException) e).getSyncExceptionCause() != SyncExceptionCause.SYNC_PROXY_CYCLED))
		{
			if (((SyncException) e).getSyncExceptionCause() != SyncExceptionCause.BLUETOOTH_DISABLED) 
			{
				Log.d(TAG, "Reset proxy in onProxyClosed");
				reset();
			}
		}
	}
   
	@Override
	public void onOnHMIStatus(OnHMIStatus onHMIStatus) {
		switch(onHMIStatus.getSystemContext()) {
			case SYSCTXT_MAIN:
				break;
			case SYSCTXT_VRSESSION:
				break;
			case SYSCTXT_MENU:
				break;
			default:
				return;
		}

		switch(onHMIStatus.getAudioStreamingState()) {
			case AUDIBLE:
				// play audio if applicable
				audible = true;
				break;
			case NOT_AUDIBLE:
				// pause/stop/mute audio if applicable
				audible = false;
				break;
			default:
				return;
		}
		  
		switch(onHMIStatus.getHmiLevel()) {
			case HMI_FULL:
				Log.i(TAG, "HMI_FULL");
				
				if (currentMessage == null) {
					
					if (onHMIStatus.getFirstRun()) {
						// setup app on SYNC
						// Set HelpPrompt
						// Set TimeoutPrompt
					
						// send welcome message if applicable
						try {
							proxy.show("Welcome!", "", TextAlignment.LEFT_ALIGNED, autoIncCorrId++);
						} catch (SyncException e) {
							DebugTool.logError("Failed to send show Welcome", e);
						}		
						// send addcommands
					
						// subscribe to buttons
						subcribeButtons();
						
					} else {
						try {
							proxy.show("No new message.", "", TextAlignment.LEFT_ALIGNED, autoIncCorrId++);
						} catch (SyncException e) {
							DebugTool.logError("Failed to send show No New Message", e);
						}
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
		// TODO Auto-generated method stub
	}
	
	@Override
	public void onAddCommandResponse(AddCommandResponse response) {
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
		try {
			if (response.getChoiceID() == 20111) {
				quickReply(currentMessage.getSender(), PresetReply.DRIVING);
			} else if (response.getChoiceID() == 20112) {
				quickReply(currentMessage.getSender(), PresetReply.CALL_LATER);
			}
			proxy.alert("Quick reply sent", "Quick reply sent.", null, false, 3000, autoIncCorrId++);
			alerting = true;
		} catch (Exception e) {
			DebugTool.logError("Failed to PerformInteraction quick reply", e);
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
		// TODO Auto-generated method stub
		Log.d(TAG, "Button: " + notification.getButtonName() + "; Event Mode: " + notification.getButtonPressMode());
		
		if (notification.getButtonPressMode() == ButtonPressMode.SHORT) {
			if (notification.getCustomButtonName() == 101) {
				Log.d(TAG, "101 down: " + currentMessage.toString());
				try {
					SoftButton sb1 = createSoftButton(101, "Listen", SoftButtonType.SBT_TEXT);
					SoftButton sb2 = createSoftButton(102, "View", SoftButtonType.SBT_TEXT);
					SoftButton sb3 = createSoftButton(103, "Cancel", SoftButtonType.SBT_TEXT);
					Vector<SoftButton> softButtons = setUpSoftButtons(sb1, sb2, sb3);
					proxy.alert(currentMessage.getBody(), "Message from ", currentMessage.getSender(), 
							currentMessage.getCurrentIndex()+1 + "/" + messageList.size(), 
		            		true, 10000, softButtons, autoIncCorrId++);
					alerting = true;
				} catch (SyncException e) {
					DebugTool.logError("Failed to send Alert", e);
				}
			} else if (notification.getCustomButtonName() == 102) {
				Log.d(TAG, "102 down: " + currentMessage.toString());
				try {
	            	SoftButton sb1 = createSoftButton(201, "Reply", SoftButtonType.SBT_TEXT);
					SoftButton sb2 = createSoftButton(202, "Next", SoftButtonType.SBT_TEXT);
					SoftButton sb3 = createSoftButton(203, "Call", SoftButtonType.SBT_TEXT);
					Vector<SoftButton> softButtons = setUpSoftButtons(sb1, sb2, sb3);
					proxy.show(currentMessage.getBody(), currentMessage.getCurrentIndex()+1 + "/" + messageList.size(), "", "", 
							null, softButtons, null, TextAlignment.LEFT_ALIGNED, autoIncCorrId++);
					Log.d(TAG, softButtons.toString());
					
					//proxy.scrollablemessage(currentMessage.getCurrentIndex()+1 + "/" + messageList.size() + ": " + currentMessage.getBody(),
                      //      10000, softButtons, autoIncCorrId++);
					//Log.d(TAG, "scrollablle message shown");
					
				} catch (SyncException e) {
					DebugTool.logError("Failed to send Show", e);
				}					
			} else if (notification.getCustomButtonName() == 103) {
				alerting = false;
				Log.d(TAG, "103 down: " + currentMessage.toString());
				Log.d(TAG, "startIndex: " + startIndex + "; messageList size; " + messageList.size());
				if (startIndex < messageList.size()) {
					alertMessage(startIndex);
				} else {
					currentMessage = null;
				}
			} else if (notification.getCustomButtonName() == 201) {
				Log.d(TAG, "201 down: " + currentMessage.toString());
					
				Choice c1 = new Choice();
				Choice c2 = new Choice();
				c1.setChoiceID(20111);
				c1.setMenuName("Driving");
				Vector<String> v1 = new Vector<String>();
				v1.add("Driving");
				c1.setVrCommands(v1);
				c2.setChoiceID(20112);
				c2.setMenuName("Call later");
				Vector<String> v2 = new Vector<String>();
				v2.add("Call later");
				c2.setVrCommands(v2);
				Vector<Choice> choiceSet = new Vector<Choice>();
				choiceSet.add(c1);
				choiceSet.add(c2);
				try {
					proxy.createInteractionChoiceSet(choiceSet, Integer.valueOf(2011), Integer.valueOf(autoIncCorrId++));
				} catch (SyncException e) {
					DebugTool.logError("Failed to create interaction choice set", e);
				}
				try {
					proxy.performInteraction("Please select a preset reply", "Please select a preset reply:", 
					Integer.valueOf(2011), null, null, InteractionMode.BOTH, null, null, autoIncCorrId++);
				} catch (SyncException e) {
					DebugTool.logError("Failed to perform interaction", e);
				}
					
			} else if (notification.getCustomButtonName() == 202) {
				Log.d(TAG, "202 down: " + currentMessage.toString());
				Log.d(TAG, "startIndex: " + startIndex + "; messageList size; " + messageList.size());
				if (startIndex < messageList.size()) {
					alertMessage(startIndex);
				} else {
					currentMessage = null;
				}
			} else if (notification.getCustomButtonName() == 203) {
				Log.d(TAG, "201 down: " + currentMessage.toString());
				MainActivity.getMain().callBack(currentMessage.getSender());
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
		// TODO Auto-generated method stub
		
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
		// TODO Auto-generated method stub
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
	
	
	public void alertMessage(int startIndex) {
		this.startIndex = startIndex;
		if (alerting == false) {
			currentMessage = messageList.get(startIndex);
			try {
				SoftButton sb1 = createSoftButton(101, "Listen", SoftButtonType.SBT_TEXT);
				SoftButton sb2 = createSoftButton(102, "View", SoftButtonType.SBT_TEXT);
				SoftButton sb3 = createSoftButton(103, "Cancel", SoftButtonType.SBT_TEXT);
				Vector<SoftButton> softButtons = setUpSoftButtons(sb1, sb2, sb3);
				proxy.alert(convertPhoneNumber(currentMessage.getSender()), 
						"Message from ", currentMessage.getSender(), currentMessage.getCurrentIndex()+1 + "/" + messageList.size(), 
						true, 10000, softButtons, autoIncCorrId++);
				alerting = true;
			} catch (SyncException e) {
				DebugTool.logError("Failed to send Alert", e);
			}
			this.startIndex++;
		}
	}
	
	public Vector<TTSChunk> convertPhoneNumber(String phoneNumber) {
		Vector<TTSChunk> chunks = new Vector<TTSChunk>();
		TTSChunk prefix = new TTSChunk();
		prefix.setText("Message from ");
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
		return chunks;
	}
	
}
