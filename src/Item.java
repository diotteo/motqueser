package ca.dioo.java.SurveillanceServer;

import java.util.Date;

class Item {
	private String media;
	private int id;

	public Item(String media, int id) {
		this.media = media;
		this.id = id;
	}


	public void setId(int id) {
		this.id = id;
	}


	public int getId() {
		return id;
	}


	public String getMedia() {
		return media;
	}


	public String toString() {
		return "id:" + Integer.toString(id) + " media:" + media;
	}
}
