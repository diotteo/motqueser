package ca.dioo.java.SurveillanceServer;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Calendar;
import java.util.concurrent.locks.ReentrantLock;
import java.util.ArrayList;
import java.util.Iterator;

class ItemQueue {
	private static ConcurrentLinkedQueue<ItemWrapper> itemQueue = new ConcurrentLinkedQueue<ItemWrapper>();
	private static Hashtable<Integer, ItemWrapper> ht = new Hashtable<Integer, ItemWrapper>();
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


	public static boolean remove(int itemId) {
		boolean wasRemoved = false;

		lock.lock();
		try {
			ItemWrapper iw = ht.get(itemId);
			if (iw != null) {
				ht.remove(itemId);
				itemQueue.remove(iw);
				wasRemoved = true;
			}
		} finally {
			lock.unlock();
		}

		return wasRemoved;
	}


	public static boolean snoozeUntil(long ts) {
		boolean wasExtended = false;

		if (snoozeUntilTs < ts) {
			snoozeUntilTs = ts;
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
			if (curTs <= snoozeUntilTs) {
System.err.println("snoozed");
				return false;
			} else if (item.getId() < minId) {
				//FIXME: Error
				throw new Error("id too small: " + item.getId() + " < " + minId);
			} else if (ht.containsKey(item.getId())) {
				throw new IllegalArgumentException("id " + item.getId() + " already exists");
			} else {
				ItemWrapper iw = new ItemWrapper(item);
				ret = itemQueue.add(iw);
				ItemWrapper tmp = ht.put(iw.getId(), iw);
				assert (tmp == null);
			}
		} finally {
			lock.unlock();
		}
		return ret;
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

		lock.lock();
		try {
			Iterator<ItemWrapper> i = itemQueue.iterator();
			while (i.hasNext()) {
				ItemWrapper itwpr = i.next();
				if (itwpr.getTimestamp() < minTs) {
					int id = itwpr.getId();
					if (id > minId) {
						minId = id + 1;
					}
					i.remove();
					ht.remove(itwpr.getId());
					queueForScript(itwpr);

				} else {
					ArrayList<Item> itemList = new ArrayList<Item>();

					for ( ; ; itwpr = i.next()) {
						/* If we find itemId, then all items added so far predate
						 * what was requested: empty the list.
						 */
						if (itwpr.getId() == itemId) {
							itemList = new ArrayList<Item>();
						} else {
							itemList.add(itwpr);
						}

						if (!i.hasNext()) {
							break;
						}
					}

					if (itemList.size() > 0) {
						lastId = itwpr.getId();
						ib = new ItemBundle(itemList, lastId);
					}
				}
			}
		} finally {
			lock.unlock();
		}

		return ib;
	}
}
