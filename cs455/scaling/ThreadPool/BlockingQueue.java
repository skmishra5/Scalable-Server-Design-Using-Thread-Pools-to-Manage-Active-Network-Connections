package cs455.scaling.ThreadPool;

import java.util.LinkedList;
import java.util.Queue;

public class BlockingQueue<E> implements CustomQueue<E> {

	private Queue<E> queue = new LinkedList<E>();
	
	@Override
	public synchronized void enqueue(E e) {

		queue.add(e);
		notify();
	}

	@Override
	public synchronized E dequeue() {
		E e = null;
        
        while(queue.isEmpty()){
            try {
                wait();
            } catch (InterruptedException e1) {
                return e;
            }
        }
        e = queue.remove();
        return e;
	}

}
