import java.net.*;
import java.io.*;
import java.util.concurrent.ConcurrentLinkedQueue;

class ControlThread extends Thread {
	private static final Class THIS_CLASS = ControlThread.class;

	private Socket sock;
	private ConcurrentLinkedQueue<String> msgQueue;

	public ControlThread(Socket sock, ConcurrentLinkedQueue<String> msgQueue) {
		this.sock = sock;
		this.msgQueue = msgQueue;
	}

	public void run() {
		try (
			PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
		) {
			String inputLine;
			while (!sock.isInputShutdown() && in.ready()) {
				inputLine = in.readLine();
				assert inputLine != null : "inputLine is null";
				msgQueue.add(inputLine);
			}
			System.out.println("Closing thread");
			sock.close();
		} catch (IOException e) {
			System.err.println("Exception in thread \n" + e.getMessage());
		}
	}
}
