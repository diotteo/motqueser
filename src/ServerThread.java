package ca.dioo.java.motqueser;

import java.net.Socket;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.PushbackInputStream;

import ca.dioo.java.libmotqueser.XmlFactory;
import ca.dioo.java.libmotqueser.MessageFactory;
import ca.dioo.java.libmotqueser.Message;
import ca.dioo.java.libmotqueser.ClientMessage;
import ca.dioo.java.libmotqueser.ControlMessage;
import ca.dioo.java.libmotqueser.ErrorMessage;
import ca.dioo.java.libmotqueser.ServerMessage;
import ca.dioo.java.libmotqueser.MalformedMessageException;

class ServerThread extends Thread {
	private static final Class THIS_CLASS = ServerThread.class;

	private Socket sock;
	private BufferedOutputStream os;
	private BufferedInputStream is;
	private PushbackInputStream pis;
	private PrintWriter wtr;
	private BufferedReader rdr;

	public ServerThread(Socket socket) {
		sock = socket;

		try {
			os = new BufferedOutputStream(sock.getOutputStream());
			pis = new PushbackInputStream(sock.getInputStream());
			is = new BufferedInputStream(pis);
			wtr = new PrintWriter(os, true);
			rdr = new BufferedReader(new InputStreamReader(is));
		} catch (IOException e) {
			throw new Error("Error creating ServerThread", e);
		}
	}


	public void run() {
		try {
			int b;
			END: while (!sock.isClosed() && (b = pis.read()) > -1) {
				pis.unread(b);
				Message m = MessageFactory.parse(XmlFactory.newXmlParser(rdr));

				if (m instanceof ClientMessage) {
					processClientMessage((ClientMessage)m);

				} else if (m instanceof ControlMessage) {
					if (!sock.getInetAddress().getHostAddress().equals("127.0.0.1")) {
						String msg = "Access forbidden";
						ErrorMessage em = new ErrorMessage(msg);
						wtr.println(em.getXmlString());
						System.err.println(msg);

					} else {
						System.out.println("control connection detected");
						processControlMessage((ControlMessage)m);
					}
				} else {
					System.err.println("Bogus message, discarding: Message is not a ClientMessage");
					break END;
				}
			}
			Utils.debugPrintln(2, "Closing thread");

		} catch (MalformedMessageException e) {
			System.err.println(ca.dioo.java.commons.Utils.getPrettyStackTrace(e));
			System.err.println("Bogus message, discarding: " + e.getMessage());
		} catch (IOException e) {
			System.err.println(ca.dioo.java.commons.Utils.getPrettyStackTrace(e));
			System.err.println("Exception in thread \n" + e.getMessage());
		} finally {
			try {
				sock.close();
			} catch (IOException e) {
				//Pass
			}
		}
	}


	private void processControlMessage(ControlMessage cm) throws IOException {
		StringBuffer sb = new StringBuffer();
		sb.append("Control Message:");
		for (ControlMessage.Item it: cm) {
			sb.append(" Item: " + it.getId());
			if (ItemQueue.isSnoozed()) {
				Utils.debugPrintln(2, "Snoozed, deleting media files for " + it.getId());

				//FIXME: delete media? maybe add a configuration parameter to control this?
				Utils.deleteById(it.getId());
			} else {
				ItemQueue.add(new Item(it.getId()));
			}
		}
		System.out.println(sb.toString());
		//FIXME: fine-grained response
		wtr.println("success");
	}


	private void processClientMessage(ClientMessage cm)
			throws UnsupportedOperationException, IOException {

		Utils.debugPrintln(1, "Client Message:" + cm);

		ServerMessage sm = new ServerMessage(cm.getVersion());
		sm.buildAsResponse(cm);
		ServerMessage.Response resp = sm.getResponse();

		if (resp instanceof ServerMessage.ItemListResponse) {
			processItemListRequest((ServerMessage.ItemListResponse)resp, sm);

		} else if (resp instanceof ServerMessage.ItemResponse) {
			processItemRequest((ServerMessage.ItemResponse)resp, sm);

		} else if (resp instanceof ServerMessage.ItemDeletionResponse) {
			processItemDeletionRequest((ServerMessage.ItemDeletionResponse)resp, sm);

		} else if (resp instanceof ServerMessage.ItemPreservationResponse) {
			processItemPreservationRequest((ServerMessage.ItemPreservationResponse)resp, sm);

		} else if (resp instanceof ServerMessage.SnoozeResponse) {
			processSnoozeRequest((ServerMessage.SnoozeResponse)resp, sm);

		} else if (resp instanceof ServerMessage.UnsnoozeResponse) {
			processUnsnoozeRequest((ServerMessage.UnsnoozeResponse)resp, sm);

		} else {
			throw new UnsupportedOperationException("Unimplemented response: " + resp.getClass().getName());
		}
		Utils.debugPrintln(1, "Server Message: " + sm);
	}


	private void processItemListRequest(ServerMessage.ItemListResponse il, ServerMessage sm) {
		ItemQueue.ItemBundle ib = ItemQueue.getItems(il.getPrevId());
		if (ib != null) {
			for (Item it: ib) {
				il.add(new ServerMessage.Item(it.getId()));
			}
		}
		wtr.println(sm.getXmlString());
	}


	private void processItemRequest(ServerMessage.ItemResponse it, ServerMessage sm)
			throws UnsupportedOperationException, IOException {

		Path mediaPath;
		switch (it.getMediaType()) {
		case VID:
			mediaPath = Utils.getVideoPathFromId(it.getId());
			break;
		case IMG:
			mediaPath = Utils.getImagePathFromId(it.getId());
			break;
		default:
			throw new UnsupportedOperationException("Unknown media type: " + it.getMediaType());
		}

		if (mediaPath == null) {
			String errMsg = "No file matching filter for id " + it.getId();
			System.err.println(errMsg);

			ErrorMessage em = new ErrorMessage(errMsg);
			wtr.println(em.getXmlString());

		} else {
			File mediaFile = new File(mediaPath.toString());

			//FIXME: should improve this eventually
			if (mediaFile.length() > Integer.MAX_VALUE) {
				throw new UnsupportedOperationException("File " + mediaPath.toString() + " is WAY too large");
			} else {
				int fileLen = (int)mediaFile.length();
				byte[] fileContent = new byte[fileLen];
System.out.println("file \"" + mediaPath.toString() + "\" is " + fileLen + " bytes long");
				int len = (new FileInputStream(mediaFile)).read(fileContent);

				it.setMediaSize(len);
				wtr.println(sm.getXmlString());
				wtr.flush();
				os.write(new byte[]{(byte)0xEE, (byte)0x00, (byte)0xFF}, 0, 3);
				os.write(fileContent, 0, fileContent.length);
				os.flush();
			}
		}
	}


	private void processItemDeletionRequest(ServerMessage.ItemDeletionResponse idr, ServerMessage sm) {
		boolean canRemove = ItemQueue.remove(idr.getId());
		if (!canRemove) {
			String errMsg = "Disallowed deletion request for ID " + idr.getId();
			ErrorMessage em = new ErrorMessage(errMsg);
			System.err.println(errMsg);
			wtr.println(em.getXmlString());

		} else {
			System.err.println("Deleting item id " + idr.getId());
			Utils.deleteById(idr.getId());
			wtr.println(sm.getXmlString());
		}
	}


	private void processItemPreservationRequest(ServerMessage.ItemPreservationResponse ipr, ServerMessage sm) {
		boolean itemExists = ItemQueue.keep(ipr.getId());
		if (!itemExists) {
			String errMsg = "Disallowed preservation request for ID " + ipr.getId();
			ErrorMessage em = new ErrorMessage(errMsg);
			System.err.println(errMsg);
			wtr.println(em.getXmlString());

		} else {
			System.err.println("Keeping item id " + ipr.getId());
			wtr.println(sm.getXmlString());
		}
	}


	private void processSnoozeRequest(ServerMessage.SnoozeResponse sr, ServerMessage sm) {
		int interval = sr.getSnoozeInterval();
		ItemQueue.snoozeFor(interval);
		wtr.println(sm.getXmlString());
	}


	private void processUnsnoozeRequest(ServerMessage.UnsnoozeResponse ur, ServerMessage sm) {
		ItemQueue.unsnooze();
		wtr.println(sm.getXmlString());
	}
}
