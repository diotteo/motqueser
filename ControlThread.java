import java.net.*;
import java.io.*;

class ControlThread extends Thread {
	private static final Class THIS_CLASS = ControlThread.class;

	private Socket sock;

	public ControlThread(Socket sock) {
		this.sock = sock;
	}

	public void run() {
		try (
			PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
		) {
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				MessageProvider.addMessage(inputLine);
			}
			System.out.println("Closing thread");
			sock.close();
		} catch (IOException e) {
			System.err.println("Exception in thread \n" + e.getMessage());
		}
	}
}
