/***
 * EchoClient
 * Example of a TCP client 
 * Date: 10/01/04
 * Authors:
 */
package stream;

import java.io.*;
import java.net.*;


public class EchoClient {

    /**
    *  main method
    *  accepts a connection, receives a message from client then sends an echo to the client
    **/
    public static void main(String[] args) throws IOException {

        Socket echoSocket = null;
        MulticastSocket multicastSocket = null;
        PrintStream socOut = null;
        BufferedReader stdIn = null;
        BufferedReader socIn = null;
        ReceiverClientMulticast receiver = null;

        if (args.length != 2) {
            System.out.println("Usage: java EchoClient <EchoServer host> <EchoServer port>");
            System.exit(1);
        }

        String ip_lo = "localhost";
        String ip_mul = args[0];
        Integer port = Integer.parseInt(args[1]);

        try {
      	    // creation socket ==> connexion
      	    echoSocket = new Socket(ip_lo,port);
	          socIn = new BufferedReader(
	    		          new InputStreamReader(echoSocket.getInputStream()));    
	          socOut= new PrintStream(echoSocket.getOutputStream());
	          stdIn = new BufferedReader(new InputStreamReader(System.in));
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host:" + args[0]);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for "
                               + "the connection to:"+ args[0]);
            System.exit(1);
        }
                             
        String line;
        // new GUI();

        // at this point, client can communicate whith "global port" (ie.: 6001)
        // he is not connected

        // manage connection by username
        boolean connected = false;
        String username = null;
        String command = null;
        String response = null;

        while (!connected)
        {
            // ask client username in stdin
            System.out.print("Votre nom d'utilisateur : ");
            username = stdIn.readLine();
            command = "UsernameIs " + username;

            // send username to stdin
            try {
                socOut.println(command);
            } catch (Exception e) {
                Logger.error("EchoClient_main", "while sending: " + e.getMessage());
            }

            // get response from server
            try {
                response = socIn.readLine();
                Logger.debug("EchoClient_main", "username_response: " + response);
            } catch (IOException e) {
                Logger.error("EchoClient_main", "while receiving: " + e.getMessage());
            }

            // analyse response (OK or KO)
            if (response.equals("success"))
                connected = true;
            else
                System.out.println("Ce nom d'utilisateur est incorrect ou deja utilise.");
        }

        // at this point, user is connected, and can communicate with "global port"
        
        // friendly message <3
        System.out.println("Bonjour " + username);
        showConnectedUsers(socIn, socOut);

        // main loop of program
        while (true) {

            // get user input
        	line=stdIn.readLine();
            Logger.warning("EchoClient_run", "readed: " + line);
        	
            // check if user wants to exit
            if (line.equals(".")) break;
        	
            // send user input (it's a command)
            socOut.println(line);
            
            if(line.contains("joinserver")){

                // Close le thread de l'ancien multicast
                if(receiver != null){
                    receiver.close();
                }
                socOut.close();
                socIn.close();
                // stdIn.close();
                echoSocket.close();
                Integer wanted_port = Integer.parseInt(line.split(" ")[1]);
                echoSocket = new Socket(ip_lo, wanted_port);
                
                socIn = new BufferedReader(
	    		          new InputStreamReader(echoSocket.getInputStream()));    
                socOut= new PrintStream(echoSocket.getOutputStream());
                // stdIn = new BufferedReader(new InputStreamReader(System.in));
                multicastSocket = new MulticastSocket(wanted_port);
                multicastSocket.joinGroup(InetAddress.getByName(ip_mul));

                
                // Ouvre un thread pour écouter le multicast
                receiver = new ReceiverClientMulticast(multicastSocket);
                receiver.start();
                Logger.debug("EchoClient_run", "Socket: " + echoSocket.toString());
                Logger.debug("EchoClient_run", "MulticastSocket: " + multicastSocket.toString());

            }
        }
        socOut.close();
        socIn.close();
        stdIn.close();
        echoSocket.close();
    }

    public static void showConnectedUsers(BufferedReader bf_in, PrintStream bf_out) {
        try {
            // send command to server
            String command = "GetUsers";
            bf_out.println(command);

            // get response (users list as serialized string)
            String response = bf_in.readLine();
            String[] users = response.split("_;_");

            // print users or error message
            if (users.length > 0) {
                System.out.println("Liste des utilisateurs connectés :")
                for (String u: users) {
                    System.out.println("- " + u);
                }
                System.out.println(); // spacing
            } else {
                System.out.println("Aucun utilisateur n'est connecté");
            }
           
        } catch (IOException e) {
            Logger.error("EchoClient_showConnectedUsers", e.getMessage());
        }
    }
}


