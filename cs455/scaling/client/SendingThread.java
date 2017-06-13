package cs455.scaling.client;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cs455.scaling.util.HashCodeCalculator;

public class SendingThread implements Runnable{
	
	int rate = 0;
	int m_msgRate;
	Random random = new Random();
	Client m_client;
	SocketChannel m_socket;
	SelectionKey m_key;
	private static int numSentMessages = 0;
	
	public static List<String> messageHashcodes = new ArrayList<String>();
	private HashCodeCalculator hashCodeCalculator = new HashCodeCalculator();
	
	public SendingThread(Client client, SelectionKey key, SocketChannel socket, int messageRate)
	{
		this.m_client = client;
		this.m_key = key;
		this.m_socket = socket;
		this.m_msgRate = messageRate;
	}
	
	public int getSentMessageCount()
	{
		return numSentMessages;
	}
	
	public void setSentMessageCount()
	{
		numSentMessages = 0;
	}
	
	@Override
	public void run() {
		
		/*RspHandler handler = new RspHandler();
		try {
			m_client.send(m_socket, m_key, "Hello World".getBytes(), handler);
		} catch (IOException e) {
			e.printStackTrace();
		}
		handler.waitForResponse(this);*/
		
		while(true)
		{
			while(rate < m_msgRate)
			{
				byte[] payload = new byte[8192];
				random.nextBytes(payload);
				//System.out.println("Length of payload sent: " + payload.length);
				String msgHashcode = hashCodeCalculator.SHA1FromBytes(payload);
				//System.out.println("Hash code in client side: " + msgHashcode);
				messageHashcodes.add(msgHashcode);
				RspHandler handler = new RspHandler();
				try {
					m_client.send(m_socket, m_key, payload, handler);
				} catch (IOException e) {
					e.printStackTrace();
				}
				//handler.waitForResponse(this);
				rate++;
				
				numSentMessages++;
				try {
					Thread.sleep(1000/m_msgRate);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			rate = 0;
			
		}
		
	}

}
