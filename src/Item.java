package ca.dioo.java.motqueser;

import java.nio.file.Path;
import java.io.IOException;

class Item {
	private int id;
	private Path vid;
	private String vidLen;


	public Item(int id) throws IOException {
		setId(id);
		vid = Utils.getVideoPathFromId(id);
		vidLen = Utils.getVidLen(vid);
	}


	public Path getPath() {
		return vid;
	}

	public String getVidLen() {
		return vidLen;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}


	public String toString() {
		return "id:" + Integer.toString(id);
	}
}
