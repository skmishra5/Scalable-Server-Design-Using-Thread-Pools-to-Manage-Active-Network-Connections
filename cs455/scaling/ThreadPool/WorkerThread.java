package cs455.scaling.ThreadPool;

import java.io.IOException;

import cs455.scaling.server.Task;

public class WorkerThread implements Runnable{
	private BlockingQueue<Runnable> m_queue;
	String m_name = "";
	private boolean isStopped = false;
     
    public WorkerThread(BlockingQueue<Runnable> queue, String name){
        this.m_queue = queue;
        this.m_name = name;
    }
     
     
    @Override
    public void run() {
        while(!isStopped){
            //Runnable r = m_queue.dequeue();
        	Runnable task = (Task) m_queue.dequeue();
            // print the dequeued item
            //System.out.println(" Taken Item by thread name:" + this.m_name );
            // run it
            task.run();
            
          
            //System.out.println(" Task completed of thread:" + this.m_name);
        }
    }
    
    public synchronized void doStop(){
        isStopped = true;
        //this.interrupt(); //break pool thread out of dequeue() call.
    }

    public synchronized boolean isStopped(){
        return isStopped;
    }
}
