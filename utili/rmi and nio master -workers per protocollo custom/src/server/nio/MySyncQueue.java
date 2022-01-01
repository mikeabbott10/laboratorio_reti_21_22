package server.nio;

import java.util.*;

/**
 * MySyncQueue modella una coda sincronizzata.
 *
 * @author Samuel Fabrizi
 * @version 1.0
 */
public class MySyncQueue<T> {

	private LinkedList<T> list;
	/**
	 * variabile booleana con la seguente semantica
	 * 		true se non ci sono più item da aggiungere
	 * 		false altrimenti
	 */
	private boolean done;

	/**
	 * numero di elementi presenti nella coda
	 */
	private int size;  		// number of directories in the queue

	public MySyncQueue() {
		list = new LinkedList<T>();
		done = false;
		size = 0;
	}

	/**
	 * Aggiunge un item alla coda
	 *
	 * @param s item da aggiungere alla coda
	 */
	public synchronized void add(T s) {
		list.add(s);
		size++;
		notify();
	}

	/**
	 * Preleva e ritorna un item dalla coda
	 *
	 * @return 	un item se la coda non è vuota
	 * 			null se done == true e coda vuota
	 */
	public synchronized T remove() throws InterruptedException{
		T s;
		while (!done && size == 0) {
			wait();
		}
		if (size > 0) {
			s = list.remove();
			size--;
			//notifyAll();
		} else
			s = null;
		return s;
	}

	/**
	 * comunica che non ci sono più item da aggiungere alla coda
	 */
	public synchronized void finish() {
		done = true;
		notifyAll();
	}
}

