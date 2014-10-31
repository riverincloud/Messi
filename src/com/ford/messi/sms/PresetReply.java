package com.ford.messi.sms;

public enum PresetReply {	
	
	DRIVING ("I am driving right now."),
	CALL_LATER ("I will call you back later.");
	
	
	private String value;
	
	
	private PresetReply(String value) {
		this.value = value;
	}
	
	public String getValue() { 
		return value; 
	}

}
