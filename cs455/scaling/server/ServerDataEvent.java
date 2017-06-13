package cs455.scaling.server;

import java.nio.channels.SocketChannel;

public class ServerDataEvent {
	public Server server;
	public SocketChannel socket;
	public byte[] data;
	
	public ServerDataEvent(Server server, SocketChannel socket, byte[] data) {
		this.server = server;
		this.socket = socket;
		this.data = data;
	}
}
