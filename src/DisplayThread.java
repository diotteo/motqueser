package ca.dioo.java.SurveillanceServer;

import java.net.*;
import java.io.*;
import java.util.Vector;

class DisplayThread extends Thread {
	private static final Class THIS_CLASS = DisplayThread.class;

	private int lastId;

	public DisplayThread() {
		lastId = -1;
	}

	public void run() {
		ItemQueue.ItemBundle ib;

		EXIT: while (true) {
			while ((ib = ItemQueue.getItems(lastId)) != null) {
				System.out.println("last id = " + lastId);
				lastId = ib.getLastId();
System.out.println("new last id = " + lastId);
				for (Item it: ib) {
					System.out.println("Item: " + it);
				}
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				break EXIT;
			}
		}
	}
}
