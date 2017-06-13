package cs455.scaling.util;

import java.sql.Timestamp;

import cs455.scaling.server.Server;

public class ServerStatisticsCollector implements Runnable{

	private Server m_server;
	
	public ServerStatisticsCollector(Server server)
	{
		m_server = server;
	}
	
	@Override
	public void run() {
		while(true)
		{
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			
			double throughput = m_server.getNumOfMsgProcessed()/5;
		    
		    System.out.println("#####################################################################################################");
		    
		    System.out.println("[" + timestamp + "] " + "Current Server Throughput: " + throughput + " messages/s" + ", " +
		    		"Active Client Connections: " + m_server.getNumberOfActiveConnections());
		    
		    m_server.setNumOfMsgProcessed();
		    		    
		    System.out.println("#####################################################################################################");
		    System.out.println("\n");
		}
		
	}

}
