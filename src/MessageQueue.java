package ca.dioo.java.SurveillanceServer;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Calendar;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Vector;
import java.util.Collection;
import java.util.Iterator;

class MessageQueue {
	private static ConcurrentLinkedQueue<Message> msgQueue = new ConcurrentLinkedQueue<Message>();
	private static ReentrantLock lock = new ReentrantLock();
	private static final int DELAY = -600;

	static class MessageBundle {
		private long ts;
		private Vector<String> msgList;

		MessageBundle(Vector<String> msgList, long ts) {
			this.msgList = msgList;
			this.ts = ts;
		}


		public Collection<String> getMessages() {
			return msgList;
		}


		public long getTimestamp() {
			return ts;
		}
	}


	/**
	 * Singleton
	 */
	private MessageQueue() {
	}


	public static void addMessage(String msg) {
		msgQueue.add(new Message(msg));
	}


	/**
	 * @return: the timestamp to the last message or -1 if no messages
	 */
	public static MessageBundle getMessages(long timestamp) {
		GregorianCalendar cl = new GregorianCalendar();
		cl.add(Calendar.MINUTE, DELAY);
		long minTs = cl.getTimeInMillis();
		long lastTs = -1;
		MessageBundle mb = null;

		lock.lock();
		try {
			Iterator<Message> i = msgQueue.iterator();
			while (i.hasNext()) {
				Message msg = i.next();
				if (msg.getTimestamp() < minTs) {
					i.remove();
				} else if (msg.getTimestamp() <= timestamp) {
					continue;
				} else {
					Vector<String> msgList = new Vector<String>();
					msgList.add(msg.getMessage());
					while (i.hasNext()) {
						msg = i.next();
						msgList.add(msg.getMessage());
					}
					lastTs = msg.getTimestamp();
					mb = new MessageBundle(msgList, lastTs);
				}
			}
		} finally {
			lock.unlock();
		}

		return mb;
	}
}
