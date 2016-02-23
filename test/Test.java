import java.net.Socket;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.Iterator;
import java.lang.Math;
import java.util.Arrays;

import ca.dioo.java.MonitorLib.XmlFactory;
import ca.dioo.java.MonitorLib.MessageFactory;
import ca.dioo.java.MonitorLib.Message;
import ca.dioo.java.MonitorLib.ServerMessage;
import ca.dioo.java.MonitorLib.ClientMessage;
import ca.dioo.java.MonitorLib.MalformedMessageException;
import ca.dioo.java.MonitorLib.BadActionTypeException;


public class Test {
	public static final String HOSTNAME = "Bob";
	public static final int PORT = 55555;

	private static int prevId = -1;

	public static void main(String[] args) throws Exception {
		getEvent(3670);
	}


	private static void getEvent(int id) {
		try {
			Socket sock = new Socket(HOSTNAME, PORT);
			PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
			BufferedInputStream bis = new BufferedInputStream(sock.getInputStream());
			InputStreamReader in = new InputStreamReader(bis);
			ClientMessage cm = new ClientMessage();
			ClientMessage.Action a = new ClientMessage.Action(ClientMessage.Action.ActionType.GET_MSG);
			try {
				try {
					a.setId(id);
				} catch (BadActionTypeException e) {
					throw new Error("?!?:" + e.getMessage(), e);
				}
				cm.add(a);
				out.println(cm.getXmlString());

				Message m = MessageFactory.parse(XmlFactory.newXmlParser(in));
				if (!(m instanceof ServerMessage)) {
					System.out.println("Server sent us an unexpected message");
				} else {
					ServerMessage sm = (ServerMessage)m;
					try {
						ServerMessage.Item it = (ServerMessage.Item)sm.getSubMessage();
						if (it == null) {
							throw new Error("item is null");
						}

System.out.println("Server Message:\n" + sm.toString() + "\n");
try {
Thread.sleep(10000);
} catch (InterruptedException e) {
	System.exit(1);
}

						BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream("/tmp/test.avi"));
						boolean b_done = false;
						int size = 0;
						while (!sock.isInputShutdown() && !b_done) {
if (sock.isInputShutdown()) {
System.out.println("input down");
}
if (sock.isOutputShutdown()) {
System.out.println("output down");
}
							int readRet = 0;
							do {
								int len = bis.available();
								if (len > 0) {
System.out.println("available = " + len);
									byte[] data = new byte[len];
									readRet = bis.read(data, 0, data.length);

									if (readRet > 0) {
										output.write(data, 0, readRet);
										output.flush();
System.out.println("wrote " + readRet + " bytes to file");
										size += readRet;
									}
								}
							} while (readRet > 0);
						}
						output.close();
					} catch (ClassCastException e) {
						System.out.println("Server sent a server_message other than item_list");
					}
				}
			} catch (MalformedMessageException e) {
				System.out.println("Server sent us bogus message: " + e.getMessage());
			}
			out.close();
			in.close();
			sock.close();

		} catch (UnknownHostException e) {
			System.err.println("Unknown host \"" + HOSTNAME + "\": " + e.getMessage());
		} catch (IOException e) {
			System.err.println("I/O exception: " + e.getMessage());
		}
	}


	private static void getEventList() {
		try {
			Socket sock = new Socket(HOSTNAME, PORT);
			PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			ClientMessage cm = new ClientMessage();
			ClientMessage.Action a = new ClientMessage.Action(ClientMessage.Action.ActionType.GET_MSG_LIST);
			try {
				try {
					a.setPrevId(prevId);
				} catch (BadActionTypeException e) {
					throw new Error("?!?:" + e.getMessage(), e);
				}
				cm.add(a);
				out.println(cm.getXmlString());

				Message m = MessageFactory.parse(XmlFactory.newXmlParser(in));
				if (!(m instanceof ServerMessage)) {
					System.out.println("Server sent us an unexpected message");
				} else {
					ServerMessage sm = (ServerMessage)m;

					try {
						ServerMessage.ItemList il = (ServerMessage.ItemList)sm.getSubMessage();
						ServerMessage.Item it = null;
						for (Iterator<ServerMessage.Item> i = il.iterator(); i.hasNext(); ) {
							it = i.next();
						}
						if (it != null) {
							prevId = it.getId();
						}
					} catch (ClassCastException e) {
						System.out.println("Server sent a server_message other than item_list");
					}
				}
			} catch (MalformedMessageException e) {
				System.out.println("Server sent us bogus message: " + e.getMessage());
			}
			out.close();
			in.close();
			sock.close();

		} catch (UnknownHostException e) {
			System.err.println("Unknown host \"" + HOSTNAME + "\": " + e.getMessage());
		} catch (IOException e) {
			System.err.println("I/O exception: " + e.getMessage());
		}
	}
}
