package cs455.scaling.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cs455.scaling.ThreadPool.ThreadPoolManager;
import cs455.scaling.util.ServerStatisticsCollector;

public class Server implements Runnable{
	
	private InetAddress hostAddress;
	private int port;

	// The channel on which we'll accept connections
	private ServerSocketChannel serverChannel;

	// The selector we'll be monitoring
	private Selector selector;

	// The buffer into which we'll read data when it's available
	private ByteBuffer readBuffer = ByteBuffer.allocate(8192);

	private EchoWorker worker;

	// A list of PendingChange instances
	private List pendingChanges = new LinkedList();

	// Maps a SocketChannel to a list of ByteBuffer instances
	public Map pendingData = new HashMap();
	
	//Creating the thread pool manager
	public static ThreadPoolManager poolManager;
	
	private HashMap<SelectionKey, Boolean> keyTaskMap = new HashMap<SelectionKey, Boolean>();
	private Task lastTask = null;
	private static ArrayList<SelectionKey> listOfKeys = new ArrayList<SelectionKey>();
	private int numOfMessageProcessed = 0;

	public Server(InetAddress hostAddress, int port, EchoWorker worker) throws IOException {
		this.hostAddress = hostAddress;
		this.port = port;
		this.selector = this.initSelector();
		this.worker = worker;
	}

	public void send(SocketChannel socket, SelectionKey key, byte[] data) {
		
		Task task = new Task(this, socket, key, worker, 1, data);
		try {
			//keyTaskMap.put(key, true);
			poolManager.execute(task);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/*synchronized (this.pendingChanges) {
			// Indicate we want the interest ops set changed
			this.pendingChanges.add(new ChangeRequest(socket, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));

			// And queue the data we want written
			synchronized (this.pendingData) {
				List queue = (List) this.pendingData.get(socket);
				if (queue == null) {
					queue = new ArrayList();
					this.pendingData.put(socket, queue);
				}
				queue.add(ByteBuffer.wrap(data));
			}
		}*/

		// Finally, wake up our selecting thread so it can make the required changes
		//this.selector.wakeup();
	}

	public void run() {
		while (true) {
			try {
				// Process any pending changes
				synchronized (this.pendingChanges) {
					Iterator changes = this.pendingChanges.iterator();
					while (changes.hasNext()) {
						ChangeRequest change = (ChangeRequest) changes.next();
						switch (change.type) {
						case ChangeRequest.CHANGEOPS:
							SelectionKey key = change.socket.keyFor(this.selector);
							key.interestOps(change.ops);
						}
					}
					this.pendingChanges.clear();
				}

				// Wait for an event one of the registered channels
				this.selector.selectNow();

				// Iterate over the set of keys for which events are available
				Iterator selectedKeys = this.selector.selectedKeys().iterator();
				while (selectedKeys.hasNext()) {
					SelectionKey key = (SelectionKey) selectedKeys.next();
					selectedKeys.remove();

					if (!key.isValid()) {
						continue;
					}

					// Check what event is available and deal with it
					if (key.isAcceptable()) {
						this.accept(key);
					} else if (key.isReadable()) {
						this.read(key);
					} else if (key.isWritable()) {
						this.write(key);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void accept(SelectionKey key) throws IOException {
		// For an accept to be pending the channel must be a server socket channel.
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

		// Accept the connection and make it non-blocking
		SocketChannel socketChannel = serverSocketChannel.accept();
		Socket socket = socketChannel.socket();
		socketChannel.configureBlocking(false);

		// Register the new SocketChannel with our Selector, indicating
		// we'd like to be notified when there's data waiting to be read
		socketChannel.register(this.selector, SelectionKey.OP_READ);
		listOfKeys.add(key);
	}
	
	public int getNumberOfActiveConnections()
	{
		return listOfKeys.size();
	}
	
	public void removeKeysFromList(SelectionKey key)
	{
		//System.out.println("Inside removeKeysFromList");
		listOfKeys.remove(0);
		/*synchronized(listOfKeys){
			if(listOfKeys.contains(key))
			{
				System.out.println("Removing key");
				listOfKeys.remove(key);
			}
		}*/
	}

	private void read(SelectionKey key) throws IOException {
			
		SocketChannel socketChannel = (SocketChannel) key.channel();
		
		if(keyTaskMap.size() != 0){
			
			if(keyTaskMap.containsKey(key))
			{
				if(!keyTaskMap.get(key))
				{
					Task task = new Task(this, socketChannel, key, worker, 0, null);
					try {
						keyTaskMap.put(key, true);
						poolManager.execute(task);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			else
			{
				Task task = new Task(this, socketChannel, key, worker, 0, null);
				try {
					keyTaskMap.put(key, true);
					poolManager.execute(task);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		else
		{
			Task task = new Task(this, socketChannel, key, worker, 0, null);
			try {
				keyTaskMap.put(key, true);
				poolManager.execute(task);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// Clear out our read buffer so it's ready for new data
		/*this.readBuffer.clear();

		// Attempt to read off the channel
		int numRead = 0;
		try {
			while(this.readBuffer.hasRemaining() && numRead != -1)
			{
				//this.readBuffer.flip();
				numRead = socketChannel.read(this.readBuffer);
				System.out.println("Number of bytes read: " + numRead + "with thread id: " + Thread.currentThread().getName());
				//this.readBuffer.clear();
			}
		} catch (IOException e) {
			// The remote forcibly closed the connection, cancel
			// the selection key and close the channel.
			System.out.println("Remote connection failed.");
			key.cancel();
			socketChannel.close();
			return;
		}
		readBuffer.flip();
		if (numRead == -1) {
			// Remote entity shut the socket down cleanly. Do the
			// same from our end and cancel the channel.
			System.out.println("No data to read");
			key.channel().close();
			key.cancel();
			return;
		}

		// Hand the data off to our worker thread
		this.worker.processData(this, socketChannel, this.readBuffer.array(), 8192);*/
	}

	private void write(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();

		synchronized (this.pendingData) {
			List queue = (List) this.pendingData.get(socketChannel);

			// Write until there's not more data ...
			while (!(queue).isEmpty()) {
				ByteBuffer buf = (ByteBuffer) (queue).get(0);
				//socketChannel.write(buf);
				//if (buf.remaining() > 0) {
					// ... or the socket's buffer fills up
					//break;
				//}
				//buf.flip();
				//while(buf.remaining() > 0)
				//{
					socketChannel.write(buf);
				//}
				
				if (buf.remaining() > 0) {
				 //... or the socket's buffer fills up
					break;
				}
				queue.remove(0);
			}

			if ((queue).isEmpty()) {
				// We wrote away all data, so we're no longer interested
				// in writing on this socket. Switch back to waiting for
				// data.
				key.interestOps(SelectionKey.OP_READ);
			}
			numOfMessageProcessed++;
		}
		keyTaskMap.put(key, false);
	}
	
	public int getNumOfMsgProcessed()
	{
		return numOfMessageProcessed;
	}
	
	public void setNumOfMsgProcessed()
	{
		numOfMessageProcessed = 0;
	}

	private Selector initSelector() throws IOException {
		// Create a new selector
		Selector socketSelector = SelectorProvider.provider().openSelector();

		// Create a new non-blocking server socket channel
		this.serverChannel = ServerSocketChannel.open();
		serverChannel.configureBlocking(false);

		// Bind the server socket to the specified address and port
		InetSocketAddress isa = new InetSocketAddress(this.hostAddress, this.port);
		serverChannel.socket().bind(isa);

		// Register the server socket channel, indicating an interest in 
		// accepting new connections
		serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);
		
		ServerStatisticsCollector servStatColl = new ServerStatisticsCollector(this);
		Thread t = new Thread(servStatColl);
		t.start();

		return socketSelector;
	}

	public static void main(String[] args) {
		
		InetAddress host = null;
		try {
			host = InetAddress.getLocalHost();
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		int portNumber = Integer.parseInt(args[0]);
		int threadPoolSize = Integer.parseInt(args[1]);
		
		poolManager = new ThreadPoolManager(threadPoolSize);
		
		try {
			EchoWorker worker = new EchoWorker();
			//new Thread(worker).start();
			new Thread(new Server(host, portNumber, worker)).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
