# Scalable-Server-Design-Using-Thread-Pools-to-Manage-Active-Network-Connections
Instructions to execute the Project
-----------------------------------
-----------------------------------

1. The Makefile is kept at the "CS455_HW2/src" directory.

2. Please run "make clean" and "make all" in order to build the project.

3. Start the server on any host with the command "java cs455.scaling.server.Server <Port Number> <Number Of Threads> 

4. Edit the file called "machine_list" which is inside the same directory in order to change the client connection host names.

5. Edit the "test-scalability.sh" script to change the name the server host name and port number. Also change the message rate if you want to.

6. Run the "test-scalability.sh" script in order to start the client connections to server. This script is kept at the same path.

7. Check for server and client statistics.



File Description
----------------
----------------

Package "cs455.scaling.client"
------------------------------

1. Client.java: This class file creates connection to the server and handles and the read and write operation.

2. RspHandler.java: This class file handles the message read from the socketchannel and clear the message from the list for which the hashcode has been matched.

3. SendingThread.java: This thread is started from the client to send 8KB of data in message rate(per second).


Package "cs455.scaling.server"
------------------------------

1. ChangeRequest.java: This class is used to set interest options in Java NIO sockets.

2. Server.java: This class is used to handle all client connections, initiates the threadpool manager and creates task reading and writing operation.

3. Task.java: This class implements the Runnable interface and implements the reading operation, calculation of hascode and writing to the socket channel.


(Note: The EchoWorker class is not used now which was created for some work in project previously.)


Package "cs455.scaling.ThreadPool"
----------------------------------

1. BlockingQueue.java: This class file contains an implementation of blocking queue with wait and notify operation.

2. CustomQueue.java: This is an interface for Queue with enqueue and dequeue operations.

3. ThreadPoolManager.java: This class initializes the threads in thread pool and maintains the task queue. It also pushes the task into the task queue.

4. WorkerThread.java: It dequeues the task from the task queue whenever it is avaialable and executes the task.


Package "cs455.scaling.util"
----------------------------

1. ClientStatisticsCollector.java: This class collects the number of sent and received messages in the client side and displays them in every 10 seconds.

2. HashCodeCalculator.java: This class file calculates the hascode for a string.

3. ServerStatisticsCollector.java: This class collects the throughput and number of active connections and displays them in every 5 seconds.


