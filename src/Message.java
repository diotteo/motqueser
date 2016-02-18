package ca.dioo.java.SurveillanceServer;

import java.util.Date;

class Item {
	private String media;
	private int id;
	private long ts;

	public Item(String media, int id) {
		this.media = media;
		this.id = id;
		ts = (new Date()).getTime();
	}


	public int getId() {
		return id;
	}


	public long getTimestamp() {
		return ts;
	}


	public String getMedia() {
		return media;
	}
}
