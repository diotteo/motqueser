package ca.dioo.java.motqueser;

import java.util.LinkedHashMap;
import java.util.Hashtable;
import java.util.Set;
import java.util.Map;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Calendar;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.IOException;

class ItemQueue {
	private static LinkedHashMap<Integer, ItemWrapper> itemQueue = new LinkedHashMap<Integer, ItemWrapper>();
	private static Map<String, ItemWrapper> indexMap = new Hashtable<String, ItemWrapper>();
	private static int minId = 0;
	private static int nextId = 0;
	private static long snoozeUntilTs = -1;


	private static class ItemWrapper extends ItemWithId {
		private boolean hidden;

		ItemWrapper(Item it) throws IOException {
			super(it);
			hidden = false;
		}

		boolean isHidden() {
			return hidden;
		}

		void hide() {
			hidden = true;
		}
	}


	public static class ItemWithId extends Item {
		private int id;

		ItemWithId(Item it) throws IOException {
			super(it);
			synchronized (ItemQueue.class) {
				id = nextId++;
			}
		}

		int getId() {
			return id;
		}
	}


	static class ItemBundle implements Iterable<ItemWithId> {
		private int lastId;
		private ArrayList<ItemWithId> itemList;

		ItemBundle(Collection<ItemWithId> itemList, int lastId) {
			this.itemList = new ArrayList<ItemWithId>(itemList);
			this.lastId = lastId;
		}


		public Iterator<ItemWithId> iterator() {
			return (Iterator<ItemWithId>)itemList.iterator();
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


	public static boolean isSnoozed() {
		return isSnoozed((new GregorianCalendar()).getTimeInMillis());
	}


	public static boolean isSnoozed(long timestamp) {
		return (timestamp <= snoozeUntilTs);
	}


	public static synchronized ItemWithId get(int id) throws ItemNotFoundException {
		ItemWithId it = itemQueue.get(id);
		if (it == null) {
			throw new ItemNotFoundException("no Item for id " + id);
		}
		return it;
	}


	public static synchronized ItemWithId getByEventId(String eventId) throws ItemNotFoundException {
		ItemWithId it = indexMap.get(eventId);
		if (it == null) {
			throw new ItemNotFoundException("no Item for eventId " + eventId);
		}
		return it;
	}


	public static synchronized boolean add(Item item) throws IOException {
		return add(item, (new GregorianCalendar()).getTimeInMillis());
	}


	public static synchronized boolean add(Item item, long timestamp) throws IOException {
		boolean ret = false;
		long curTs = timestamp;
		String eventId = item.getEventId();
		ItemWrapper iw_im = indexMap.get(eventId);

		Utils.debugPrintln(4, "cur:" + curTs + " snooze:" + snoozeUntilTs);
		if (isSnoozed(curTs)) {
			Utils.debugPrintln(4, "snoozed");
			return false;
		} else if (iw_im != null) {
			if (iw_im.isHidden()) {
				throw new IllegalArgumentException(eventId + " previously added");
			} else {
				throw new IllegalArgumentException(eventId + " already exists");
			}
		} else {
			ItemWrapper iw = new ItemWrapper(item);
			ItemWrapper tmp = itemQueue.put(iw.getId(), iw);
			assert (tmp == null);
			tmp = indexMap.put(iw.getEventId(), iw);
			assert (tmp == null);
			ret = true;
		}
		return ret;
	}


	public static synchronized boolean remove(int itemId) {
		boolean wasRemoved = false;

		ItemWrapper it = itemQueue.get(itemId);
		if (it != null && !it.isHidden()) {
			it.hide();
			wasRemoved = true;
		}

		return wasRemoved;
	}


	public static synchronized boolean keep(int itemId) {
		boolean wasRemoved = false;
		ItemWrapper it;

		it = itemQueue.get(itemId);
		if (it != null && !it.isHidden()) {
			it.hide();
			wasRemoved = true;
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
	public static synchronized ItemBundle getItems(int itemId) {
		GregorianCalendar cl = new GregorianCalendar();
		cl.add(Calendar.SECOND, -Config.getDelay());
		long minTs = cl.getTimeInMillis();
		int lastId = -1;
		ItemBundle ib = null;
		ArrayList<ItemWithId> itemList = new ArrayList<ItemWithId>();

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
				indexMap.remove(itwpr.getEventId());
				queueForScript(itwpr);

			/* If we find itemId, then all items added so far predate
			 * what was requested: empty the list.
			 */
			} else if (itwpr.getId() == itemId) {
				itemList.clear();
			} else if (itwpr.isHidden()) {
				//Pass
			} else {
				itemList.add(itwpr);
				lastId = id;
			}
		}

		if (itemList.size() > 0) {
			ib = new ItemBundle(itemList, lastId);
		}

		return ib;
	}
}
