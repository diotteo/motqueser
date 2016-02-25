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

import ca.dioo.java.commons.BinaryWithHeaderStream;

public class Test {
	public static final String HOSTNAME = "Bob";
	public static final int PORT = 55555;

	private static int prevId = -1;

	public static void main(String[] args) throws Exception {
		getEvent(3670);
		delEvent(333);
	}


	private static void delEvent(int id) {
		try {
			Socket sock = new Socket(HOSTNAME, PORT);
			PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
			InputStream is = sock.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(is);
			InputStreamReader in = new InputStreamReader(bis);
			ClientMessage cm = new ClientMessage();
			ClientMessage.ItemDeletionRequest req = new ClientMessage.ItemDeletionRequest(id);
			try {
				cm.add(req);
				System.out.println("Client Message:\n" + cm.getXmlString());
				out.println(cm.getXmlString());

				Message m = MessageFactory.parse(XmlFactory.newXmlParser(in));
				if (!(m instanceof ServerMessage)) {
					System.out.println("Server sent us an unexpected message");
				} else {
					ServerMessage sm = (ServerMessage)m;
					try {
						ServerMessage.ItemDeletionResponse it = (ServerMessage.ItemDeletionResponse)sm.getResponse();
						if (it == null) {
							throw new Error("item is null");
						}

						System.out.println("Server Message:\n" + sm.toString() + "\n");
					} catch (ClassCastException e) {
						System.out.println("Server sent a server_message other than item_list");
					}
				}
			} catch (MalformedMessageException e) {
				System.out.println("Server sent us bogus message: " + e.getMessage());
			}
			sock.close();

		} catch (UnknownHostException e) {
			System.err.println("Unknown host \"" + HOSTNAME + "\": " + e.getMessage());
		} catch (IOException e) {
			System.err.println("I/O exception: " + e.getMessage());
		}
	}


	private static void getEvent(int id) {
		try {
			Socket sock = new Socket(HOSTNAME, PORT);
			PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
			InputStream is = sock.getInputStream();
			BinaryWithHeaderStream bins = new BinaryWithHeaderStream(is);
			BufferedInputStream bis = new BufferedInputStream(bins);
			InputStreamReader in = new InputStreamReader(bis);
			ClientMessage cm = new ClientMessage();
			ClientMessage.ItemRequest req = new ClientMessage.ItemRequest();
			try {
				req.setId(id);
				cm.add(req);
				System.out.println("Client Message:\n" + cm.getXmlString());
				out.println(cm.getXmlString());

				Message m = MessageFactory.parse(XmlFactory.newXmlParser(in));
				if (!(m instanceof ServerMessage)) {
					System.out.println("Server sent us an unexpected message");
				} else {
					ServerMessage sm = (ServerMessage)m;
					try {
						ServerMessage.ItemResponse it = (ServerMessage.ItemResponse)sm.getResponse();
						if (it == null) {
							throw new Error("item is null");
						}

						System.out.println("Server Message:\n" + sm.toString() + "\n");
						BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream("/tmp/test.avi"));
						int size = 0;
						int readRet;
						byte[] data = bins.getBuffer();
						output.write(data);
						do {
							readRet = is.read(data, 0, data.length);

							if (readRet > 0) {
								output.write(data, 0, readRet);
								output.flush();
								size += readRet;
							}
						} while (readRet > -1);
						output.close();
					} catch (ClassCastException e) {
						System.out.println("Server sent a server_message other than item_list");
					}
				}
			} catch (MalformedMessageException e) {
				System.out.println("Server sent us bogus message: " + e.getMessage());
			}
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
			ClientMessage.ItemListRequest req = new ClientMessage.ItemListRequest();
			try {
				req.setPrevId(prevId);
				cm.add(req);
				out.println(cm.getXmlString());

				Message m = MessageFactory.parse(XmlFactory.newXmlParser(in));
				if (!(m instanceof ServerMessage)) {
					System.out.println("Server sent us an unexpected message");
				} else {
					ServerMessage sm = (ServerMessage)m;

					try {
						ServerMessage.ItemListResponse il = (ServerMessage.ItemListResponse)sm.getResponse();
						ServerMessage.ItemResponse it = null;
						for (Iterator<ServerMessage.ItemResponse> i = il.iterator(); i.hasNext(); ) {
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
