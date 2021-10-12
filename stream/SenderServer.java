package stream;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class SenderServer {
    private static SenderServer ss;
    
    private DatagramSocket serverSocket;

    private String ip;
    
    private int port;

	private SenderServer(String ip, int port) throws SocketException, IOException{
		this.ip = ip;
        this.port = port;
        // socket used to send
        serverSocket = new DatagramSocket();
	}

    public static SenderServer getInstance(String ip,int port) throws SocketException, IOException {
        if(ss == null){
            ss = new SenderServer(ip, port);
        }
        return ss;
    }

    public void send(String argument) throws IOException{
        // make datagram packet
        byte[] message = ("Multicasting... "+argument).getBytes();
        DatagramPacket packet = new DatagramPacket(message, message.length, 
            InetAddress.getByName(ip), port);
        // send packet
        ss.serverSocket.send(packet);
    }
    
    public void close(){
        ss.serverSocket.close();
    }
}