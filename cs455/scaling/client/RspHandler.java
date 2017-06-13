package cs455.scaling.client;

public class RspHandler {
	private byte[] rsp = null;
	
	public synchronized boolean handleResponse(SendingThread sendThread, byte[] rsp) {
		this.rsp = rsp;
		String receivedBytes = new String(this.rsp);
		if(sendThread.messageHashcodes.contains(receivedBytes))
		{
			System.out.println("Hashcode Matched. Removing from the list....");
			sendThread.messageHashcodes.remove(receivedBytes);
		}
		else
		{
			System.out.println("Unique String");
		}
		
		System.out.println(new String(this.rsp));
		
		//this.notify();
		return true;
	}
	
	public synchronized void waitForResponse(SendingThread sendThread) {
		while(this.rsp == null) {
			try {
				this.wait();
			} catch (InterruptedException e) {
			}
		}
		
		String receivedBytes = new String(this.rsp);
		if(sendThread.messageHashcodes.contains(receivedBytes))
		{
			System.out.println("Hashcode Matched. Removing from the list....");
			sendThread.messageHashcodes.remove(receivedBytes);
		}
		else
		{
			System.out.println("Unique String");
		}
		
		System.out.println(new String(this.rsp));
	}
}
