package ca.dioo.java.SurveillanceServer;

public class Utils {
	public static int dbgLvl = 0;

	public static void debugPrintln(int minLvl, String msg) {
		if (dbgLvl >= minLvl) {
			System.out.println(msg);
		}
	}
}

