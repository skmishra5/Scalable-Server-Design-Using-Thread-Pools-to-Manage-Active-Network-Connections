package cs455.scaling.server;

import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;

import cs455.scaling.util.HashCodeCalculator;

public class EchoWorker implements Runnable{

	private List queue = new LinkedList();
	private HashCodeCalculator hashCodeCalculator = new HashCodeCalculator();
	
	public void processData(Server server, SocketChannel socket, byte[] data, int count) {
		
		//System.out.println("Received from the client: " + new String(data));
		byte[] dataCopy = new byte[count];
		System.arraycopy(data, 0, dataCopy, 0, count);
		synchronized(queue) {
			queue.add(new ServerDataEvent(server, socket, dataCopy));
			queue.notify();
		}
	}
	
	public void run() {
		ServerDataEvent dataEvent;
		
		while(true) {
			// Wait for data to become available
			synchronized(queue) {
				while(queue.isEmpty()) {
					try {
						queue.wait();
					} catch (InterruptedException e) {
					}
				}
				dataEvent = (ServerDataEvent) queue.remove(0);
			}
			
			
			String hashedData = hashCodeCalculator.SHA1FromBytes(dataEvent.data);
			System.out.println("Hashcode in server side: " + hashedData);
			int length = hashedData.length();
			System.out.println("Length sent to client: " + length);
			String message = Integer.toString(length) + hashedData;
			//m_server.send(m_socketChannel, Integer.toString(length).getBytes());
			//dataEvent.server.send(dataEvent.socket, message.getBytes());
			
			/*Task task = new Task(dataEvent.server, dataEvent.socket, dataEvent.data);
			try {
				dataEvent.server.poolManager.execute(task);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			
			// Return to sender
			//dataEvent.server.send(dataEvent.socket, dataEvent.data);
		}
	}
}
