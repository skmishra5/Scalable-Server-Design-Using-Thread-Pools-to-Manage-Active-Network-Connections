package cs455.scaling.ThreadPool;

import java.util.ArrayList;
import java.util.List;

public class ThreadPoolManager {
	
	private final int NUMBER_OF_THREADS;
	private BlockingQueue<Runnable> taskQueue = new BlockingQueue<Runnable>();
    private List<WorkerThread> threads = new ArrayList<WorkerThread>();
    private boolean isStopped = false;

    public ThreadPoolManager(int noOfThreads){

    	this.NUMBER_OF_THREADS = noOfThreads;
    	startThreads();
        
    }
    
    public void startThreads()
    {
    	for(Integer i=0; i < NUMBER_OF_THREADS; i++){
    		WorkerThread workerThread = new WorkerThread(taskQueue, i.toString());
    		Thread newThread = new Thread(workerThread);
            threads.add(workerThread);
            newThread.start();
        }
    }

    public synchronized void  execute(Runnable task) throws Exception{
        if(this.isStopped) throw
            new IllegalStateException("ThreadPool is stopped");

        this.taskQueue.enqueue(task);
    }

    public synchronized void stop(){
        this.isStopped = true;
        for(WorkerThread thread : threads){
           thread.doStop();
        }
    }
}
