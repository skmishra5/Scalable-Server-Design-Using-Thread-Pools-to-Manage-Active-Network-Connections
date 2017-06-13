package cs455.scaling.ThreadPool;

public interface CustomQueue<E> {
	
	public void enqueue(E e);
	
	public E dequeue();
}
