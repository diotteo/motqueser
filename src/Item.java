package ca.dioo.java.motqueser;

import java.nio.file.Path;
import java.io.IOException;

class Item {
	private int id;
	private Path vid;


	public Item(int id) throws IOException {
		setId(id);
		vid = Utils.getVideoPathFromId(id);
	}

	public Path getPath() {
		return vid;
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
