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
			while ((inputLine = in.readLine()) != null) {
				out.println(inputLine);

				if (inputLine.equals("close")) {
					out.println("Closing thread");
					break;
				}
			}
			sock.close();
		} catch (IOException e) {
			System.out.println("Exception in thread \n" + e.getMessage());
		}
	}
}
