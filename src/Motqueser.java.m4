// vim: syntax=java noexpandtab:
package ca.dioo.java.motqueser;

import gnu.getopt.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.DirectoryStream;

import ca.dioo.java.libmotqueser.Message;
import ca.dioo.java.libmotqueser.ControlMessage;
import ca.dioo.java.libmotqueser.ClientMessage;

enum NetConMode {
	SERVER,
	CLIENT,
	INVALID,
	}

enum ClientReqType {
	NEW_ITEM,
	DEL_ITEM,
	INVALID,
}

class Motqueser {
	private static final Class THIS_CLASS = Motqueser.class;
	public static final String PRGM = THIS_CLASS.getSimpleName();

	// This gets replaced by the git tag/hash at compile-time
	private static final String VERSION = "M4_VERSION_MACRO";

	private static NetConMode mode = NetConMode.SERVER;
	private static String itemId = null;
	private static ClientReqType reqType = ClientReqType.INVALID;


	public static void printHelp() {
		System.err.println("Usage: " + PRGM + " [options]"
				+ "\n  -h|--help"
				+ "\n  -p|--port <port>:"
				+ "\n      port to listen on (in client mode, port to connect to). If not specified, default to automatically allocated (random port)"
				+ "\n  -c|--client <event ID>:"
				+ "\n      client mode. Event ID to send to the server. The client must be run on the same machine as the server."
				+ "\n  -z|--delete:"
				+ "\n      Instead of sending a new event to the server, request a deletion. This is a client mode option."
				+ "\n  -V|--version:"
				+ "\n      Print version string and exit."
				+ "\n  -d|--debug [lvl]:"
				+ "\n      enable debugging. lvl is an integer, defaults to 1"
		);
	}


	public static void printHelp(int code) {
		printHelp();
		System.exit(code);
	}


	private static void parseArgs(String args[]) {
		LongOpt[] longopts = {
				new LongOpt("help",     LongOpt.NO_ARGUMENT,       null, 'h'),
				new LongOpt("port",     LongOpt.REQUIRED_ARGUMENT, null, 'p'),
				new LongOpt("client",   LongOpt.REQUIRED_ARGUMENT, null, 'c'),
				new LongOpt("version",  LongOpt.REQUIRED_ARGUMENT, null, 'V'),
				new LongOpt("debug",    LongOpt.OPTIONAL_ARGUMENT, null, 'd'),
				new LongOpt("delete",   LongOpt.NO_ARGUMENT,       null, 'z'),
		};

		Getopt g = new Getopt(PRGM, args, "c:d::hp:Vz", longopts);

		int o;
		while ((o = g.getopt()) != -1) {
			char c = (char)o;
			switch (o) {
			case 'c':
				{
					/* Allows us to accept everything between
					 * /path/to/1234-20001129144023.avi and 1234-20001129144023
					 */
					String s = new File(g.getOptarg()).getName();
					String a[] = s.split("-|[^[:digit:]]");
					if (a.length == 2) {
						itemId = a[0] + "-" + a[1];
					} else if (a.length == 1) {
						itemId = Utils.findEventByIndex(Integer.parseInt(a[0]));
					} else {
						System.err.println((char)g.getOptopt() + ": invalid argument format\n");
						printHelp(1);
					}

					mode = NetConMode.CLIENT;
					reqType = ClientReqType.NEW_ITEM;
				}
				break;
			case 'd':
				try {
					String arg = g.getOptarg();
					if (arg != null) {
						Utils.dbgLvl = Integer.parseInt(arg);
					} else {
						Utils.dbgLvl++;
					}
				} catch (NumberFormatException e) {
					System.err.println((char)g.getOptopt() + ": invalid argument: must be an integer\n");
					printHelp(1);
				}
				break;
			case 'h':
				printHelp(1);
				break;
			case 'p':
				{
					int p = 0;
					try {
						p = new Integer(g.getOptarg());
					} catch (NumberFormatException e) {
						System.err.println(c + ": argument must be a number");
						printHelp(1);
					}
					if (p < 1 || p > 65535) {
						System.err.println(c + ": \"" + p + "\" is not 1 <= port <= 65535");
						printHelp(1);
					}
					Config.port = p;
				}
				break;
			case 'V':
				System.out.println(PRGM + " version " + VERSION);
				System.exit(0);
				break;
			case 'z':
				reqType = ClientReqType.DEL_ITEM;
				break;
			case ':':
				System.err.println((char)g.getOptopt() + ": argument required\n");
				printHelp(1);
				break;
			case '?':
				System.err.println((char)g.getOptopt() + ": invalid option\n");
				printHelp(1);
				break;
			default:
				System.err.println(c + ": error\n");
				printHelp(1);
				break;
			}
		}

		if (mode == NetConMode.CLIENT) {
			if (Config.port == 0) {
				System.err.println("Error: port must be specified in client mode\n");
				printHelp(1);
			} else if (itemId == null) {
				System.err.println("Error: event ID must be specified in client mode\n");
				printHelp(1);
			}
		} else if (reqType != ClientReqType.INVALID) {
			System.err.println("Error: client option specified in server mode\n");
			printHelp(1);
		}
	}


	//FIXME: have server verify that it sees the file
	//FIXME: implement sessions, allowing control and client messages from everywhere
	private static void executeAsClient() {
		Message msg;
		String host;
		Item it = null;

		switch (reqType) {
		case DEL_ITEM:
			throw new UnsupportedOperationException("functionality removed");
			//msg = prepareClientMessage(itemId);
			//break;
		case NEW_ITEM:
			{
				CtrlMsgTuple tup = prepareControlMessage(itemId);
				msg = tup.cm;
				it = tup.it;
			}
			break;
		default:
			throw new Error("Invalid request type: " + reqType);
		}

		try {
			Socket sock = new Socket(InetAddress.getByName("127.0.0.1"), Config.port);
			PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			out.println(msg.getXmlString());

			if (msg instanceof ControlMessage) {
				String result = in.readLine();
				if (result == null) {
					System.out.println("Connection closed unexpectedly");
				} else if (!result.equals("success")) {
					System.err.println(result);
					System.exit(2);
				}
			} else if (msg instanceof ClientMessage) {
				String line;
				while ((line = in.readLine()) != null) {
					System.out.println(line);
				}
			}

			in.close();
			out.close();
			sock.close();
		} catch (UnknownHostException e) {
			System.err.println("You don't have a loopback interface. Fix your system.");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("Error in socket communication: " + e.getMessage());

			if (reqType == ClientReqType.NEW_ITEM) {
				System.err.println("Executing script.");
				try {
					System.exit(ScriptRunnerThread.executeScript(it));
				} catch (IOException|InterruptedException e2) {
					throw new Error(e2.getMessage(), e2);
				}
			}
			System.exit(1);
		}
	}


	private static class CtrlMsgTuple {
		ControlMessage cm;
		Item it;

		CtrlMsgTuple(ControlMessage cm, Item it) {
			this.cm = cm;
			this.it = it;
		}
	}


	private static CtrlMsgTuple prepareControlMessage(String itemId) {
		Path itemPath = null;
		Item it = null;
		try {
			it = Utils.getItemFromString(itemId);
			itemPath = Utils.getVideoPathFromItem(it);
		} catch (IOException e) {
			System.err.println("No file matched filter for item id " + itemId + ":" + e.getMessage());
			System.exit(2);
		}

		ControlMessage cm = new ControlMessage();
		ControlMessage.Item cm_it = new ControlMessage.Item(itemId);
		cm.add(cm_it);

		return new CtrlMsgTuple(cm, it);
	}


	//private static Message prepareClientMessage(String itemId) {
	//	ClientMessage cm = new ClientMessage();
	//	Item it = Utils.getItemFromString(itemId);
	//	ClientMessage.ItemDeletionRequest req = new ClientMessage.ItemDeletionRequest(ItemQueue.get());
	//	cm.add(req);
	//	return cm;
	//}


	public static void executeAsServer() {
		try {
			ServerSocket servSock = new ServerSocket(Config.port);
			System.out.println("Starting " + PRGM + " version " + VERSION + " listening on port " + servSock.getLocalPort());

			DisplayThread dt = new DisplayThread();
			ScriptRunnerThread srt = new ScriptRunnerThread();
			NotificationThread nt = new NotificationThread();
			dt.start();
			srt.start();
			nt.start();

			while (true) {
				Socket sock = servSock.accept();

				Utils.debugPrintln(2, "Starting new thread...");
				ServerThread st = new ServerThread(sock);
				st.start();
			}
		} catch (IOException e) {
			System.err.println("Exception listening on port " + Config.port + "\n" + e.getMessage());
			System.exit(1);
		}
	}


	public static void main(String args[]) {
		parseArgs(args);

		if (mode == NetConMode.CLIENT) {
			executeAsClient();
		} else {
			executeAsServer();
		}
	}
}
