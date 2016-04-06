// vim: syntax=java noexpandtab:
package ca.dioo.java.motqueser;

import java.nio.file.Path;
import java.io.IOException;
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.text.SimpleDateFormat;

class Item {
	private static int lastId = 0;
	private int idx; //motion idx
	private long timestamp; //motion timestamp
	private Path img;
	private Path vid;
	private String vidLen;


	public Item(int idx, long timestamp) throws IOException {
		setIdx(idx);
		setTimestamp(timestamp);
		img = Utils.getImagePathFromItem(this);
		vid = Utils.getVideoPathFromItem(this);
		vidLen = Utils.getVidLen(vid);
	}


	/**
	 * copy constructor
	 */
	public Item(Item it) {
		idx = it.idx;
		timestamp = it.timestamp;
		img = it.img;
		vid = it.vid;
		vidLen = it.vidLen;
	}


	public Path getImgPath() {
		return img;
	}

	public Path getVidPath() {
		return vid;
	}

	public int getImgSize() throws IOException {
		long size = img.toFile().length();

		if (size > Integer.MAX_VALUE || size < 1) {
			throw new IOException("wrong image file size");
		}
		return (int)size;
	}

	public int getVidSize() throws IOException {
		long size = vid.toFile().length();

		if (size > Integer.MAX_VALUE || size < 1) {
			throw new IOException("wrong video file size");
		}
		return (int)size;
	}

	public String getVidLen() {
		return vidLen;
	}

	public void setIdx(int idx) {
		this.idx = idx;
	}

	public int getIdx() {
		return idx;
	}

	public void setTimestamp(long ts) {
		timestamp = ts;
	}

	public long getTimestamp() {
		return timestamp;
	}


	public String getEventId() {
		String id = idx + "-";
		Calendar cl = new GregorianCalendar();
		cl.setTimeInMillis(timestamp * 1000);
		id += new SimpleDateFormat("yyyyMMddkkmmss").format(cl.getTime());

		return id;
	}


	public String toString() {
		return "event ID: " + getEventId();
		//"idx:" + Integer.toString(idx) + " ts:" + new Date(timestamp);
	}
}
