package ca.dioo.java.SurveillanceServer;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Calendar;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Vector;
import java.util.Collection;
import java.util.Iterator;

class ItemQueue {
	private static ConcurrentLinkedQueue<Item> itemQueue = new ConcurrentLinkedQueue<Item>();
	private static ReentrantLock lock = new ReentrantLock();
	private static final int DELAY = -600;

	static class ItemBundle {
		private int maxId;
		private Vector<Item> itemList;

		ItemBundle(Vector<Item> itemList, int maxId) {
			this.itemList = itemList;
			this.maxId = maxId;
		}


		public Collection<Item> getItems() {
			return itemList;
		}


		public long getTimestamp() {
			return ts;
		}
	}


	/**
	 * Singleton
	 */
	private ItemQueue() {
	}


	public static boolean add(Item item) {
		return itemQueue.add(item);
	}


	/**
	 * if itemId = 3, only items with ID >= 4 should be returned.
	 *
	 * @param: itemId the id of the last seen item or -1 if no messages.
	 */
	public static ItemBundle getItems(int itemId) {
		GregorianCalendar cl = new GregorianCalendar();
		cl.add(Calendar.MINUTE, DELAY);
		long minTs = cl.getTimeInMillis();
		long lastTs = -1;
		ItemBundle ib = null;

		lock.lock();
		try {
			Iterator<Item> i = itemQueue.iterator();
			while (i.hasNext()) {
				Item item = i.next();
				if (item.getTimestamp() < minTs) {
					i.remove();
				} else if (item.getTimestamp() <= timestamp) {
					continue;
				} else {
					Vector<String> itemList = new Vector<String>();
					itemList.add(item.getItem());
					while (i.hasNext()) {
						item = i.next();
						itemList.add(item.getItem());
					}
					lastTs = item.getTimestamp();
					ib = new ItemBundle(itemList, lastTs);
				}
			}
		} finally {
			lock.unlock();
		}

		return ib;
	}
}
