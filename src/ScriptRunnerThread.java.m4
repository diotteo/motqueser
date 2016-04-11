// vim: syntax=java noexpandtab:
package ca.dioo.java.motqueser;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.IOException;
import java.io.InputStream;

class ScriptRunnerThread extends Thread {
	private static final Class THIS_CLASS = ScriptRunnerThread.class;

	private static ConcurrentLinkedQueue<Item> queue;
	private static ScriptRunnerThread singleton = null;
	private static Runtime rt = Runtime.getRuntime();

	public ScriptRunnerThread() {
		if (singleton != null) {
			throw new Error("Only one instance allowed");
		}
		queue = new ConcurrentLinkedQueue<Item>();
		singleton = this;
	}


	public static ScriptRunnerThread getThread() {
		return singleton;
	}


	public static boolean add(Item it) {
		if (singleton == null) {
			throw new Error("Instance not initialized");
		}
		return queue.add(it);
	}


	public static int executeScript(Item it) throws IOException, InterruptedException {
		byte[] b = new byte[32768];
		String cmd = Config.getScript() + " " + it.getVidPath();
		Process p = rt.exec(cmd);

		if (Utils.dbgLvl >= 3) {
			System.out.println("Executing " + cmd);

			InputStream is = p.getInputStream();
			int nb;
			while ((nb = is.read(b)) > -1) {
				System.out.write(b, 0, nb);
			}
		}

		int ret = p.waitFor();

		if (Utils.dbgLvl >= 3) {
			System.out.println("\nDone");
		}

		return ret;
	}


	public void run() {
		Item it;

		EXIT: while (true) {
			while ((it = queue.poll()) != null) {
				try {
					executeScript(it);
				} catch (IOException|InterruptedException e) {
					//Pass
				}
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				break EXIT;
			}
		}
	}
}
