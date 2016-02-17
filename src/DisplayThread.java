import java.net.*;
import java.io.*;
import java.util.Vector;

class DisplayThread extends Thread {
	private static final Class THIS_CLASS = DisplayThread.class;

	private long ts;

	public DisplayThread() {
		ts = -1;
	}

	public void run() {
		MessageProvider.MessageBundle mb;

		while (true) {
			while ((mb = MessageProvider.getMessages(ts)) != null) {
				System.out.println("ts = " + ts);
				ts = mb.getTimestamp();
				for (String msg: mb.getMessages()) {
					System.out.println("Message: " + msg);
				}
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				//Exiting
			}
		}
	}
}
