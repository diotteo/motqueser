// vim: syntax=java noexpandtab:
package ca.dioo.java.motqueser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.DirectoryStream;
import java.util.ArrayList;
import java.io.File;

public class Config {
	private static File configFile = null;

	private static int port = -1;
	private static int notificationPort = -1;
	private static Path mediaDir = null;
	private static String script = null;
	private static int delay = -1;
	private static String deletePrefix = null;
	private static String deleteSuffix = null;


	static {
		configFile = getConfigFile();
		parseConfigFile(configFile);
	}


	//Singleton
	private Config() {}


	public static int getPort() {
		return port;
	}

	public static int getNotificationPort() {
		return notificationPort;
	}

	public static Path getMediaDir() {
		return mediaDir;
	}

	public static String getScript() {
		return script;
	}

	public static int getDelay() {
		return delay;
	}

	public static String getDeletePrefix() {
		return deletePrefix;
	}

	public static String getDeleteSuffix() {
		return deleteSuffix;
	}

	private static File getConfigFile() {
		return new File("motqueser.conf");
	}


	/**
	 * parses config file, populating Config fields
	 *
	 * -config file must be formatted as name = value
	 * -whitespace is trimmed everywhere, use double-quotes to protect literal
	 *   whitespace in values
	 *		 -E.g.: 'foo = "  bar " "' -> 'foo=  bar " '
	 *		 -Double-quotes are preserved unless the first and last characters are
	 *		   whitespaces, in which case those two and those two only are stripped
	 * -name is case-insensitive
	 * -comments and blank lines are allowed
	 * -comments are lines which start with '#' as the first non-whitespace character
	 * -blank lines are any line without a non-whitespace character
	 */
	private static void parseConfigFile(File cfg) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(cfg));
			String line;
			int lineno = 0;

			while ((line = br.readLine()) != null) {
				lineno++;
				String s = line.trim();

				if (s.length() < 1 || s.codePointAt(0) == "#".codePointAt(0)) {
					continue;
				}

				int sep = s.indexOf("=".codePointAt(0));
				if (sep == -1) {
					System.err.println("Error on line " + lineno + " in config file " + cfg + ": " + line);
					System.exit(1);
				}

				//names are case-insensitive
				String name = s.substring(0, sep).trim().toLowerCase();
				String val = s.substring(sep + 1, s.length()).trim();

				int last = val.length() - 1;
				final int QUOTE = "\"".codePointAt(0);
				if (val.codePointAt(0) == QUOTE && val.codePointAt(last) == QUOTE) {
					val = val.substring(1, last);
				}


				switch (name) {
				case "port":
					{
						int i = new Integer(val);
						if (i > 0 && i < 65536) {
							port = i;
						}
					}
					break;
				case "notification_port":
					{
						int i = new Integer(val);
						if (i > 0 && i < 65536 && i != port) {
							notificationPort = i;
						}
					}
					break;
				case "media_dir":
					mediaDir = (new File(val)).toPath();
					break;
				case "script":
					script = val;
					break;
				case "delete_prefix":
					deletePrefix = val;
					break;
				case "delete_suffix":
					deleteSuffix = val;
					break;
				case "delay":
					{
						int i = new Integer(val);
						if (i > 0) {
							delay = i;
						}
					}
					break;
				default:
					System.err.println("Ignoring unknown parameter \"" + name + "\" on line " + lineno);
					break;
				}
				//System.out.println("\"" + name + "\" = \"" + val + "\"");
			}

			if (mediaDir == null) {
				System.err.println("configuration option media_dir is required");
				System.exit(1);
			}
		} catch (IOException e) {
			throw new Error("File not found: " + cfg, e);
		}
	}
}
