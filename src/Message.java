package ca.dioo.java.SurveillanceServer;

import java.util.Date;

class Message {
	private String msg;
	private long ts;

	public Message(String msg) {
		this.msg = msg;
		ts = (new Date()).getTime();
	}


	public long getTimestamp() {
		return ts;
	}


	public String getMessage() {
		return msg;
	}
}
