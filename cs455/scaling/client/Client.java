package cs455.scaling.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import cs455.scaling.ThreadPool.WorkerThread;
import cs455.scaling.server.ChangeRequest;
import cs455.scaling.util.ClientStatisticsCollector;
import cs455.scaling.util.HashCodeCalculator;

public class Client implements Runnable{
	
	private InetAddress hostAddress;
	private int port;
	private static int messageRate;
	private static int receivedMessageCount = 0;

	// The selector we'll be monitoring
	private Selector selector;

	// The buffer into which we'll read data when it's available
	//private ByteBuffer readBuffer = ByteBuffer.allocate(8192);
	private ByteBuffer readBuffer;

	// A list of PendingChange instances
	private List pendingChanges = new LinkedList();

	// Maps a SocketChannel to a list of ByteBuffer instances
	private Map pendingData = new HashMap();
	
	// Maps a SocketChannel to a RspHandler
	private Map rspHandlers = Collections.synchronizedMap(new HashMap());
	
	public static List<String> messageHashcodes = new ArrayList<String>();
	private HashCodeCalculator hashCodeCalculator = new HashCodeCalculator();
	private SendingThread sendingThread;
	
	public Client(InetAddress hostAddress, int port) throws IOException {
		this.hostAddress = hostAddress;
		this.port = port;
		this.selector = this.initSelector();
	}
	
	public SocketChannel startNewConnection()
	{
		// Start a new connection
		SocketChannel socket = null;
		try {
			socket = this.initiateConnection();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//this.selector.wakeup();
		return socket;
	}

	public void send(SocketChannel socket, SelectionKey key, byte[] data, RspHandler handler) throws IOException {
		
		//synchronized (this.pendingChanges) {
			// Indicate we want the interest ops set changed
			//this.pendingChanges.add(new ChangeRequest(socket, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));
		
		// Register the response handler
		this.rspHandlers.put(socket, handler);
		
		// And queue the data we want written
		synchronized (this.pendingData) {
			List queue = (List) this.pendingData.get(socket);
			if (queue == null) {
				queue = new ArrayList();
				this.pendingData.put(socket, queue);
			}
			queue.add(ByteBuffer.wrap(data));
		}
		//}
		// Finally, wake up our selecting thread so it can make the required changes
		//this.selector.wakeup();
		this.write(key);
	}

	public void run() {
		SocketChannel socketChannel = null;
		try {
			socketChannel = SocketChannel.open();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			socketChannel.configureBlocking(false);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// Kick off connection establishment
		try {
			socketChannel.connect(new InetSocketAddress(this.hostAddress, this.port));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			socketChannel.register(this.selector, SelectionKey.OP_CONNECT);
		} catch (ClosedChannelException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		while (true) {
			try {
				// Process any pending changes
				/*synchronized (this.pendingChanges) {
					Iterator changes = this.pendingChanges.iterator();
					while (changes.hasNext()) {
						ChangeRequest change = (ChangeRequest) changes.next();
						switch (change.type) {
						case ChangeRequest.CHANGEOPS:
							SelectionKey key = change.socket.keyFor(this.selector);
							key.interestOps(change.ops);
							break;
						case ChangeRequest.REGISTER:
							System.out.println("Register");
							change.socket.register(this.selector, change.ops);
							break;
						}
					}
					this.pendingChanges.clear();
				}*/
				
				
			
				
				//System.out.println("Inside Initiate Connection");
				
				// Wait for an event one of the registered channels
				this.selector.select();

				// Iterate over the set of keys for which events are available
				Iterator selectedKeys = this.selector.selectedKeys().iterator();
				while (selectedKeys.hasNext()) {
					//System.out.println("Inside while loop");
					SelectionKey key = (SelectionKey) selectedKeys.next();
					selectedKeys.remove();

					if (!key.isValid()) {
						continue;
					}

					// Check what event is available and deal with it
					if (key.isConnectable()) {
						//System.out.println("calling finish connect");
						this.finishConnection(key);
					} else if (key.isReadable()) {
						this.read(key);
					} 
					/*else if (key.isWritable()) {
						this.write(key);
					}*/
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void read(SelectionKey key) throws IOException {
		
		boolean flag = false;
		SocketChannel socketChannel = (SocketChannel) key.channel();
		readBuffer = ByteBuffer.allocate(2);
		ByteBuffer readBuffer1 = null;

		// Clear out our read buffer so it's ready for new data
		this.readBuffer.clear();

		// Attempt to read off the channel
		int numRead = 0;
		try {
			numRead = socketChannel.read(this.readBuffer);
			if(numRead == 2)
			{
				flag = true;
				String size = new String(this.readBuffer.array());
				readBuffer1 = ByteBuffer.allocate(Integer.parseInt(size));
				numRead = 0;
				numRead = socketChannel.read(readBuffer1);
			}
			//while(this.readBuffer.hasRemaining())
			//{
				//numRead = socketChannel.read(this.readBuffer);
			//}
		} catch (IOException e) {
			// The remote forcibly closed the connection, cancel
			// the selection key and close the channel.
			key.cancel();
			socketChannel.close();
			return;
		}
		//readBuffer.flip();
		if (numRead == -1) {
			// Remote entity shut the socket down cleanly. Do the
			// same from our end and cancel the channel.
			key.channel().close();
			key.cancel();
			return;
		}

		//System.out.println("Length read: " + numRead);
		
		// Handle the response
		this.handleResponse(socketChannel, readBuffer1.array(), numRead);
		readBuffer1.clear();
		//key.interestOps(SelectionKey.OP_WRITE);
	}

	private void handleResponse(SocketChannel socketChannel, byte[] data, int numRead) throws IOException {
		// Make a correctly sized copy of the data before handing it
		// to the client
		byte[] rspData = new byte[numRead];
		System.arraycopy(data, 0, rspData, 0, numRead);
		
		// Look up the handler for this channel
		RspHandler handler = (RspHandler) this.rspHandlers.get(socketChannel);
		
		// And pass the response to it
		if (handler.handleResponse(sendingThread, rspData)) {
			receivedMessageCount++;
			// The handler has seen enough, close the connection
			//socketChannel.close();
			//socketChannel.keyFor(this.selector).cancel();
			//socketChannel.keyFor(this.selector).interestOps(SelectionKey.OP_WRITE);
		}
	}

	public int getRecievedMessages()
	{
		return receivedMessageCount;
	}
	
	public void setReceivedMessages()
	{
		receivedMessageCount = 0;
	}
	
	private void write(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();

		synchronized (this.pendingData) {
			List queue = (List) this.pendingData.get(socketChannel);

			// Write until there's not more data ...
			while (!queue.isEmpty()) {
				ByteBuffer buf = (ByteBuffer) queue.get(0);
				
				//while(buf.remaining() > 0)
				//{
					socketChannel.write(buf);
				//}
				if (buf.remaining() > 0) {
					// ... or the socket's buffer fills up
					break;
				}
				queue.remove(0);
			}

			if (queue.isEmpty()) {
				// We wrote away all data, so we're no longer interested
				// in writing on this socket. Switch back to waiting for
				// data.
				//key.interestOps(SelectionKey.OP_READ);
			}
		}
	}

	private void finishConnection(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
	
		// Finish the connection. If the connection operation failed
		// this will raise an IOException.
		try {
			socketChannel.finishConnect();
		} catch (IOException e) {
			// Cancel the channel's registration with our selector
			System.out.println(e);
			key.cancel();
			return;
		}
	
		// Register an interest in writing on this channel
		
		System.out.println("Starting the sending thread.");
		sendingThread = new SendingThread(this, key, socketChannel, messageRate);
		Thread thread = new Thread(sendingThread);
		thread.start();
		key.interestOps(SelectionKey.OP_READ);
		ClientStatisticsCollector statCollector = new ClientStatisticsCollector(this, sendingThread);
		Thread thread1 = new Thread(statCollector);
		thread1.start();
	}

	private SocketChannel initiateConnection() throws IOException {
		// Create a non-blocking socket channel
		SocketChannel socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false);
	
		// Kick off connection establishment
		socketChannel.connect(new InetSocketAddress(this.hostAddress, this.port));
		socketChannel.register(this.selector, SelectionKey.OP_CONNECT);
		System.out.println("Inside Initiate Connection");
		// Queue a channel registration since the caller is not the 
		// selecting thread. As part of the registration we'll register
		// an interest in connection events. These are raised when a channel
		// is ready to complete connection establishment.
		//synchronized(this.pendingChanges) {
			//this.pendingChanges.add(new ChangeRequest(socketChannel, ChangeRequest.REGISTER, SelectionKey.OP_CONNECT));
		//}
		
		return socketChannel;
	}

	private Selector initSelector() throws IOException {
		// Create a new selector
		return SelectorProvider.provider().openSelector();
	}

	/*private void sendingMessage(SocketChannel socket, Client client, int messageRate)
	{		
		int rate = 0;
		Random random = new Random();
		
		while(true)
		{
			while(rate < messageRate)
			{
				byte[] payload = new byte[8192];
				random.nextBytes(payload);
				System.out.println("Length of payload sent: " + payload.length);
				String msgHashcode = hashCodeCalculator.SHA1FromBytes(payload);
				messageHashcodes.add(msgHashcode);
				RspHandler handler = new RspHandler();
				try {
					client.send(socket, payload, handler);
				} catch (IOException e) {
					e.printStackTrace();
				}
				handler.waitForResponse(this);
				rate++;
			}
			
			rate = 0;
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}*/
	
	public static void main(String[] args) {
		
		InetAddress serverIP = null;
		try {
			serverIP = InetAddress.getByName(args[0]);
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		int portNumber = Integer.parseInt(args[1]);
		messageRate = Integer.parseInt(args[2]);
		
		try {
			Client client = new Client(serverIP, portNumber);
			Thread t = new Thread(client);
			//t.setDaemon(true);
			t.start();
			
			
			
			//SocketChannel socket = client.startNewConnection();
			//client.sendingMessage(socket, client, messageRate);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
