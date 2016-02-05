import java.net.*;
import java.io.*;

class ServerThread extends Thread {
	private static final Class THIS_CLASS = ServerThread.class;

	private Socket sock;

	public ServerThread(Socket sock) {
		this.sock = sock;
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
				out.println(inputLine);

				if (inputLine.equals("close")) {
					System.out.println("Close command received");
					break;
				}
			}
			System.out.println("Closing thread");
			sock.close();
		} catch (IOException e) {
			System.err.println("Exception in thread \n" + e.getMessage());
		}
	}
}
