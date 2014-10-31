package com.ford.messi.sms;

public class Message {
	
	private String sender;
	private String body;
	private int currentIndex;
	
	
	public Message() {		
	}
	
	public Message(String sender, String body, int currentIndex) {
		this.sender = sender;
		this.body = body;
		this.currentIndex = currentIndex;
	}
	
	
	public String getSender() {
		return sender;
	}
	
	public void setSender(String sender) {
		this.sender = sender;
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
		return "Sender: " + sender + "; Body: " + body + "; Current index: " + currentIndex;
	}

}