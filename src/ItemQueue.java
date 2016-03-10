package ca.dioo.java.motqueser;

import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Map;
import java.util.Enumeration;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Calendar;
import java.util.concurrent.locks.ReentrantLock;
import java.util.ArrayList;
import java.util.Iterator;

class ItemQueue {
	private static LinkedHashMap<Integer, ItemWrapper> itemQueue = new LinkedHashMap<Integer, ItemWrapper>();
	private static ReentrantLock lock = new ReentrantLock();
	private static int minId = 0;
	private static long snoozeUntilTs = -1;


	private static class ItemWrapper extends Item {
		private long ts;

		ItemWrapper(Item it) {
			super(it.getMedia(), it.getId());
			ts = (new Date()).getTime();
		}


		public long getTimestamp() {
			return ts;
		}
	}


	static class ItemBundle implements Iterable<Item> {
		private int lastId;
		private ArrayList<Item> itemList;

		ItemBundle(ArrayList<Item> itemList, int lastId) {
			this.itemList = itemList;
			this.lastId = lastId;
		}


		public Iterator<Item> iterator() {
			return (Iterator<Item>)itemList.iterator();
		}


		public int getLastId() {
			return lastId;
		}
	}


	/**
	 * Singleton
	 */
	private ItemQueue() {
	}


	public static boolean snoozeUntil(long ts) {
		boolean wasExtended = false;

		if (snoozeUntilTs < ts) {
			snoozeUntilTs = ts;
			Utils.debugPrintln(4, "Snoozing until " + ts);
			wasExtended = true;
		}

		return wasExtended;
	}


	public static boolean snoozeFor(int seconds) {
		if (seconds < 1) {
			return false;
		}

		GregorianCalendar cl = new GregorianCalendar();
		cl.add(Calendar.SECOND, seconds);
		snoozeUntil(cl.getTimeInMillis());
		return true;
	}


	public static void unsnooze() {
		snoozeUntilTs = -1;
	}


	public static boolean add(Item item) throws IllegalArgumentException {
		boolean ret = false;
		long curTs = (new GregorianCalendar()).getTimeInMillis();

		lock.lock();
		try {
			Utils.debugPrintln(4, "cur:" + curTs + " snooze:" + snoozeUntilTs);
			if (curTs <= snoozeUntilTs) {
				Utils.debugPrintln(4, "snoozed");
				return false;
			} else if (item.getId() < minId) {
				//FIXME: Error
				throw new Error("id too small: " + item.getId() + " < " + minId);
			} else if (itemQueue.containsKey(item.getId())) {
				throw new IllegalArgumentException("id " + item.getId() + " already exists");
			} else {
				ItemWrapper iw = new ItemWrapper(item);
				ItemWrapper tmp = itemQueue.put(iw.getId(), iw);
				assert (tmp == null);
			}
		} finally {
			lock.unlock();
		}
		return ret;
	}


	public static boolean remove(int itemId) {
		boolean wasRemoved = false;

		lock.lock();
		try {
			Item it = itemQueue.remove(itemId);
			if (null != it) {
				wasRemoved = true;
			}
		} finally {
			lock.unlock();
		}

		return wasRemoved;
	}


	public static boolean keep(int itemId) {
		boolean wasRemoved = false;
		Item it;

		lock.lock();
		try {
			it = itemQueue.remove(itemId);
			if (null != it) {
				wasRemoved = true;
			}
		} finally {
			lock.unlock();
		}

		if (wasRemoved) {
			queueForScript(it);
		}

		return wasRemoved;
	}


	private static void queueForScript(Item it) {
		ScriptRunnerThread.add(it);
	}


	/**
	 * if itemId = 3, only items added to the queue after ID 3 should be
	 * returned (i.e.: ID 3 will not be returned).
	 *
	 * @param: itemId the id of the last seen item or -1 for all item.
	 */
	public static ItemBundle getItems(int itemId) {
		GregorianCalendar cl = new GregorianCalendar();
		cl.add(Calendar.SECOND, -Config.getDelay());
		long minTs = cl.getTimeInMillis();
		int lastId = -1;
		ItemBundle ib = null;
		ArrayList<Item> itemList = new ArrayList<Item>();

		lock.lock();
		try {
			Set<Map.Entry<Integer, ItemWrapper>> set = itemQueue.entrySet();
			Map.Entry<Integer, ItemWrapper> entry;
			Iterator<Map.Entry<Integer, ItemWrapper>> i;
			for (i = set.iterator(); i.hasNext(); ) {
				entry = i.next();
				ItemWrapper itwpr = entry.getValue();
				int id = itwpr.getId();

				if (itwpr.getTimestamp() < minTs) {
					if (id > minId) {
						minId = id + 1;
					}
					i.remove();
					queueForScript(itwpr);

				/* If we find itemId, then all items added so far predate
				 * what was requested: empty the list.
				 */
				} else if (itwpr.getId() == itemId) {
					itemList.clear();
				} else {
					itemList.add(itwpr);
					lastId = id;
				}
			}

			if (itemList.size() > 0) {
				ib = new ItemBundle(itemList, lastId);
			}
		} finally {
			lock.unlock();
		}

		return ib;
	}
}
