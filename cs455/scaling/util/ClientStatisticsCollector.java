package cs455.scaling.util;

import java.sql.Timestamp;

import cs455.scaling.client.Client;
import cs455.scaling.client.SendingThread;

public class ClientStatisticsCollector implements Runnable{

	Client m_client;
	SendingThread m_senderThread;
	
	public ClientStatisticsCollector(Client client, SendingThread senderThread)
	{
		m_client = client;
		m_senderThread = senderThread;
	}
	
	@Override
	public void run()
	{	
		while(true)
		{
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		    
		    System.out.println("###############################################################################################");
		    
		    System.out.println("[" + timestamp + "] " + "Total Sent Count: " + m_senderThread.getSentMessageCount() + ", " +
		    		"Total Received Count: " + m_client.getRecievedMessages());
		    m_senderThread.setSentMessageCount();
		    m_client.setReceivedMessages();
		    
		    System.out.println("###############################################################################################");
		    System.out.println("\n");
		}
		
	}
	
	
}
