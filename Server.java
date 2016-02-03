import gnu.getopt.*;
import java.net.*;
import java.io.*;
import java.util.ArrayList;

class Server {
	private static final Class THIS_CLASS = Server.class;
	public static final String PRGM = THIS_CLASS.getSimpleName();

	private static int port = 0;


	public static void printHelp() {
		System.out.println("Usage: Server [options]\n"
				+ "  -h|--help\n"
				+ "  -p|--port <port>:\n"
				+ "      port to listen on. If not specified, default to automatically allocated (random port)"
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
		};

		Getopt g = new Getopt(PRGM, args, "p:H:h", longopts);

		if (args.length < 1) {
			printHelp(1);
		}

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
					System.out.println(c + ": argument must be a number");
					printHelp(1);
				}
				if (port < 1 || port > 65535) {
					System.out.println(c + ": \"" + port + "\" is not 1 <= port <= 65535");
					printHelp(1);
				}
				break;
			case ':':
				System.out.println((char)g.getOptopt() + ": argument required\n");
				printHelp(1);
				break;
			case '?':
				System.out.println((char)g.getOptopt() + ": invalid option\n");
				printHelp(1);
				break;
			default:
				System.out.println(c + ": error\n");
				printHelp(1);
				break;
			}
		}
	}


	public static void main(String args[]) {
		//ArrayList<ServerThread> thList = new ArrayList<ServerThread>();
		parseArgs(args);

		try {
			ServerSocket servSock = new ServerSocket(port);

			while (true) {
				Socket sock = servSock.accept();

				System.out.println("Starting new thread...");
				ServerThread st = new ServerThread(sock);
				//thList.add(st);
				st.start();
			}
		} catch (IOException e) {
			System.out.println("Exception listening on port " + port + "\n" + e.getMessage());
			System.exit(1);
		}
	}
}
