package stream;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;

public class ThreadGeneralNotifications extends Thread {
    private MulticastSocket multicast = null;
    
    public ThreadGeneralNotifications(MulticastSocket multicast){
        this.multicast = multicast;
    }
    
    public void run() {
        while(true){
            try{
                printMessage();
            } catch(IOException e){
                Logger.error("ThreadGeneralNotifications", e.getMessage());
            }
        }
    }

    public void printMessage() throws IOException{
        // make datagram packet to recieve
        byte[] message = new byte[256];
        DatagramPacket packet = new DatagramPacket(message, message.length);
        
        // recieve the packet
        multicast.receive(packet);
        System.out.println(new String(packet.getData()));
    }

    public void close(){
        multicast.close();
    }
}