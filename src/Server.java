package ca.dioo.java.SurveillanceServer;

import gnu.getopt.*;
import java.net.*;
import java.io.*;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.DirectoryStream;

import ca.dioo.java.MonitorLib.ControlMessage;

enum NetConMode {
	SERVER,
	CLIENT,
	INVALID }

class Server {
	private static final Class THIS_CLASS = Server.class;
	public static final String PRGM = THIS_CLASS.getSimpleName();
	private static final String VERSION = "0.1";

	private static int port = 0;
	private static NetConMode mode = NetConMode.SERVER;
	private static String message;


	public static void printHelp() {
		System.err.println("Usage: Server [options]"
				+ "\n  -h|--help"
				+ "\n  -p|--port <port>:"
				+ "\n      port to listen on (in client mode, port to connect to). If not specified, default to automatically allocated (random port)"
				+ "\n  -c|--client <message>:"
				+ "\n      client mode. Message to send to the server. The client must be run on the same machine as the server."
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
		};

		Getopt g = new Getopt(PRGM, args, "c:p:hVd::", longopts);

		int o;
		while ((o = g.getopt()) != -1) {
			char c = (char)o;
			switch (o) {
			case 'h':
				printHelp(1);
				break;
			case 'p':
				try {
					port = new Integer(g.getOptarg());
				} catch (NumberFormatException e) {
					System.err.println(c + ": argument must be a number");
					printHelp(1);
				}
				if (port < 1 || port > 65535) {
					System.err.println(c + ": \"" + port + "\" is not 1 <= port <= 65535");
					printHelp(1);
				}
				break;
			case 'c':
				message = g.getOptarg();
				mode = NetConMode.CLIENT;
				break;
			case 'V':
				System.out.println(PRGM + " version " + VERSION);
				System.exit(0);
				break;
			case 'd':
				Utils.dbgLvl = 1;
				try {
					String arg = g.getOptarg();
					if (arg != null) {
						Utils.dbgLvl = new Integer(arg);
					}
				} catch (NumberFormatException e) {
					System.err.println((char)g.getOptopt() + ": invalid argument: must be an integer\n");
					printHelp(1);
				}
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

		if (mode == NetConMode.CLIENT && port == 0) {
			System.err.println("Error: port must be specified in client mode\n");
			printHelp(1);
		}
	}


	private static void executeAsClient() {
		String fn = null;
		Path mediaDir = null;
		try {
			String dirStr = (new BufferedReader(new FileReader("dir.conf"))).readLine();
			mediaDir = FileSystems.getDefault().getPath(dirStr);
			DirectoryStream<Path> ds = Files.newDirectoryStream(mediaDir, message + "-*.avi");

			boolean b_first = true;
			for (Path p: ds) {
				if (fn != null) {
					System.err.println("More than one file matches filter, aborting.");
					System.exit(2);
				}
				fn = p.getFileName().toString();
				if (b_first) {
					b_first = false;
					System.out.println("matching files:");
				}
				System.out.println(fn);
			}
		} catch (IOException e) {
			System.err.println("Error matching file:" + e.getMessage());
			System.exit(2);
		}
		if (fn == null) {
			System.err.println("No file matching filter in " + mediaDir);
			System.exit(2);
		}

		try {
			ControlMessage cm = new ControlMessage();
			ControlMessage.Item it = new ControlMessage.Item(new Integer(message));
			ControlMessage.Media m = new ControlMessage.Media(fn);
			it.add(m);
			cm.add(it);

			Socket sock = new Socket(InetAddress.getByName("127.0.0.1"), port);
			PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			out.println(cm.getXmlString());

			String result = in.readLine();
			if (!result.equals("success")) {
				System.err.println(result);
				System.exit(2);
			}

			in.close();
			out.close();
			sock.close();
		} catch (UnknownHostException e) {
			System.err.println("You don't have a loopback interface. Fix your system.");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("Error in socket communication: " + e.getMessage());
			System.exit(1);
		}
	}


	public static void executeAsServer() {
		try {
			ServerSocket servSock = new ServerSocket(port);
			System.out.println("Starting " + PRGM + " listening on port " + servSock.getLocalPort());

			DisplayThread dt = new DisplayThread();
			dt.start();

			while (true) {
				Socket sock = servSock.accept();

				if (sock.getInetAddress().getHostAddress().equals("127.0.0.1")) {
					System.out.println("control connection detected");
					ControlThread ct = new ControlThread(sock);
					ct.start();
				} else {
					Utils.debugPrintln(2, "Starting new thread...");
					ServerThread st = new ServerThread(sock);
					st.start();
				}
			}
		} catch (IOException e) {
			System.err.println("Exception listening on port " + port + "\n" + e.getMessage());
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
