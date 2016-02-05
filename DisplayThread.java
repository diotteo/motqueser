import java.net.*;
import java.io.*;
import java.util.concurrent.ConcurrentLinkedQueue;

class DisplayThread extends Thread {
	private static final Class THIS_CLASS = DisplayThread.class;

	private Socket sock;
	private ConcurrentLinkedQueue<String> msgQueue;

	public DisplayThread(ConcurrentLinkedQueue<String> msgQueue) {
		this.sock = sock;
		this.msgQueue = msgQueue;
	}

	public void run() {
		String msg;

		while (true) {
			while ((msg = msgQueue.poll()) != null) {
				System.out.println("message: " + msg);
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				//Exiting
			}
		}
	}
}
