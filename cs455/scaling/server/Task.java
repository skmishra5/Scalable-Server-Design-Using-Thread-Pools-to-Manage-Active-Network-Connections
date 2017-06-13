package cs455.scaling.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cs455.scaling.util.HashCodeCalculator;

public class Task implements Runnable{

	private SocketChannel m_socketChannel = null;
	private SelectionKey m_key;
	private byte[] m_data;
	private Server m_server;
	private EchoWorker m_worker;
	private HashCodeCalculator hashCodeCalculator = new HashCodeCalculator();
	// The buffer into which we'll read data when it's available
	private ByteBuffer readBuffer = ByteBuffer.allocate(8192);
	private int m_readWrite;
	//private byte[] m_data;
	//private Map pendingData = new HashMap();
	
	/*public Task(Server server, SocketChannel socketChannel, byte[] data)
	{
		m_server = server;
		m_socketChannel = socketChannel;
		//m_data = data;
	}*/
	
	public Task(Server server, SocketChannel socketChannel, SelectionKey key, EchoWorker worker, int readWrite, byte[] data)
	{
		m_server = server;
		m_socketChannel = socketChannel;
		m_key = key;
		m_worker = worker;
		m_readWrite = readWrite;
		m_data = data;
	}
	
	public int getReadOrWriteFlag()
	{
		return m_readWrite;
	}
	
	@Override
	public void run() {
		
		if(m_readWrite == 0){
		SocketChannel socketChannel = (SocketChannel) m_key.channel();
		// Clear out our read buffer so it's ready for new data
				this.readBuffer.clear();

				// Attempt to read off the channel
				int numRead = 0;
				try {
					while(this.readBuffer.hasRemaining() && numRead != -1)
					{
						//this.readBuffer.flip();
						numRead = socketChannel.read(this.readBuffer);
						//System.out.println("Number of bytes read: " + numRead + "with thread id: " + Thread.currentThread().getName());
						//this.readBuffer.clear();
					}
				} catch (IOException e) {
					// The remote forcibly closed the connection, cancel
					// the selection key and close the channel.
					System.out.println("Remote connection failed.");
					m_key.cancel();
					try {
						socketChannel.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					return;
				}
				readBuffer.flip();
				if (numRead == -1) {
					// Remote entity shut the socket down cleanly. Do the
					// same from our end and cancel the channel.
					System.out.println("No data to read");
					m_server.removeKeysFromList(m_key);
					try {
						m_key.channel().close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					m_key.cancel();
					return;
				}
				//if(numRead > 0)
				//{
				// Hand the data off to our worker thread
					//m_worker.processData(m_server, socketChannel, this.readBuffer.array(), 8192);
				//}
				
				//byte[] dataCopy = new byte[8192];
				//if(numRead != 0){
					//System.arraycopy(this.readBuffer, 0, dataCopy, 0, 8192);}
				if(numRead > 0)
				{
					String hashedData = hashCodeCalculator.SHA1FromBytes(this.readBuffer.array());
					//System.out.println("Hashcode in server side: " + hashedData);
					int length = hashedData.length();
					//System.out.println("Length sent to client: " + length);
					String message = Integer.toString(length) + hashedData;
					//m_server.send(m_socketChannel, Integer.toString(length).getBytes());
					m_server.send(socketChannel, m_key, message.getBytes());
					//send(socketChannel, message.getBytes());
				}
		}
		else
		{
			send(m_socketChannel, m_data);
		}
	}
	
	public void send(SocketChannel socketChannel, byte[] data)
	{
		m_key.interestOps(SelectionKey.OP_WRITE);
		synchronized (m_server.pendingData) {
			List queue = (List) m_server.pendingData.get(socketChannel);
			if (queue == null) {
				queue = new ArrayList();
				m_server.pendingData.put(socketChannel, queue);
			}
			queue.add(ByteBuffer.wrap(data));
		}
	}

}
