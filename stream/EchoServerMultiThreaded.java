/***
 * EchoServer
 * Example of a TCP server
 * Date: 10/01/04
 * Authors:
 */

package stream;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class EchoServerMultiThreaded {

	private static String ip_lo = "localhost";
	private static String ip_mul;
	private static Integer port;
	private static HashMap<String, Socket> listeUtilisateur = new HashMap<String, Socket>();
	private static SenderServer generalNotificationsMulticast;
	private static Integer maxPort;
	private static HashMap<String, Room> listeRoom = new HashMap<String, Room>();
	private static HashMap<String, Room> listeGroups = new HashMap<String, Room>();
	public static Semaphore mutexConversation = new Semaphore(1);

	/**
	 * main method
	 * 
	 * @param EchoServer port
	 * 
	 **/
	public static void main(String args[]) {
		ServerSocket listenSocket;

		// sanity check
		if (args.length != 2) {
			System.out.println("Usage: java EchoServer <EchoServer ip> <EchoServer port>");
			System.exit(1);
		}

		// getting network informations
		ip_mul = args[0];
		port = Integer.parseInt(args[1]);
		maxPort = port;

		// init multicast socket for general notifications
		initGeneralNotifications();

		try {

			listenSocket = new ServerSocket(port);
			Logger.debug("EchoServerMultiThreaded_main", "Server ready...");
			while (true) {

				// when a user connects, manage it in another thread
				Socket clientSocket = listenSocket.accept();
				Logger.debug("EchoServerMultiThreaded_main", "Connexion from:" + clientSocket.getInetAddress());
				ClientThread ct = new ClientThread(clientSocket);
				ct.start();
			}
		} catch (Exception e) {
			Logger.error("EchoServerMultiThreaded_main", e.getMessage());
		}
	}

	/**
	 * 
	 */
	private static void initGeneralNotifications() {

		try {
			generalNotificationsMulticast = new SenderServer(ip_mul, port);
		} catch (SocketException e) {
			Logger.error("EchoServerMultiThreaded_initGeneralNotifications", e.getMessage());
		} catch (IOException e) {
			Logger.error("EchoServerMultiThreaded_initGeneralNotifications", e.getMessage());
		}
	}

	public static String manageGroup(String room_name) {
		String output;

		/**
		 * 
		 * Output doit renvoyer le port de connexion pour la conversation Ce n'est
		 * qu'ensuite que le client pourra se connecter à la room
		 */

		Logger.warning("EchoServerMultiThreaded_manageGroup", "Looking for group '" + room_name + "'");
		Logger.warning("EchoServerMultiThreaded_manageGroup", "length: " + String.valueOf(room_name.length()));

		boolean alreadyExists = false;
		for (String k: listeGroups.keySet()) {
			Integer comp = k.compareTo(room_name);
			Logger.warning("EchoServerMultiThreaded_manageRoom", "Testing '" + k.trim() + "' -> " + String.valueOf(comp));
			Logger.warning("EchoServerMultiThreaded_manageRoom", "length: " + String.valueOf(k.trim().length()));
			if (comp == 0) {
				alreadyExists = true;
				Logger.warning("EchoServerMultiThreaded_manageRoom", "Room found!");
			}
		}
		
		if (!alreadyExists) {
			// room doesn't exist, create it
			Logger.warning("EchoServerMultiThreaded_manageRoom", "Creating room '" + room_name + "'");
			output = createGroup(room_name);
		} else {
			// room exists
			Room roomToJoin = listeGroups.get(room_name);
			output = Integer.toString(roomToJoin.port);
		}

		Logger.warning("EchoServerMultiThreaded_manageRoom", "port:" + output);

		return output;
	}


	public static String createGroup(String roomName) {
		String output = "";
		try {

			// create ServerSocket by searching next available port
			boolean portToFind = true;
			int portDepart = EchoServerMultiThreaded.maxPort + 1;
			ServerSocket listenSocket = null;
			while (portToFind) {
				try {
					listenSocket = new ServerSocket(portDepart);
					EchoServerMultiThreaded.maxPort = portDepart;
					Logger.debug("EchoServerMultiThreaded_createRoom", "Server ready");
					portToFind = false;
				} catch (IOException e) {
					portDepart++;
				}
			}

			// create multicast socket for room
			SenderServer listenMulticast = new SenderServer(ip_mul, portDepart);

			// create room and push instance to static data structure
			Room newRoom = new Room(listenSocket, listenMulticast, portDepart);

			Logger.debug("EchoServerMultiThreaded_createRoom", "added room: '" + roomName + "' (length " + String.valueOf(roomName.length()) + " )");
			listeGroups.put(roomName, newRoom);

			output = Integer.toString(portDepart);
		} catch (Exception e) {
			Logger.error("EchoServerMultiThreaded_createRoom", e.getMessage());
		}
		return output;
	}

	public static String connectGroup(String group_name, String username) {
		String output = "";

		try {

			// getting room
			Room roomToJoin = listeGroups.get(group_name);

			// getting room sockets
			ServerSocket clientSocketToJoin = roomToJoin.serversocket;
			Socket clientSocket = clientSocketToJoin.accept();
			SenderServer clientMulticastToListen = roomToJoin.multicast;
			Logger.debug("EchoServerMultiThreaded_connectRoom",
					"Connexion from:" + clientSocket.getInetAddress() + " with port " + roomToJoin.port);

			// manage room in another thread
			ClientThread ct = new ClientThread(clientSocket, clientMulticastToListen, username, group_name);
			ct.start();

			output = "Room joined !";
		} catch (Exception e) {
			Logger.error("EchoServerMultiThreaded_connectRoom", e.getMessage());
		}
		return output;
	}

	/**
	 * Send message to notify that a user just connects
	 * 
	 * @param connected_username user who just connects
	 * @throws IOException
	 */
	public static void notifyConnection(String connected_username) throws IOException {
		generalNotificationsMulticast.send("NEWCONNECTION " + connected_username);
	}

	public static void notifyDisconnection(String connected_username) throws IOException {
		generalNotificationsMulticast.send("DISCONNECTION " + connected_username);
	}

	/**
	 * If needed, create a room for two users.
	 * 
	 * @param username1 the client asking for a room (current user)
	 * @param username2 the receiver
	 * @return the port of corresponding room.
	 */
	public static String manageRoom(String username1, String username2) {
		String output;

		/**
		 * 
		 * Output doit renvoyer le port de connexion pour la conversation Ce n'est
		 * qu'ensuite que le client pourra se connecter à la room
		 */

		String roomName = (username1.trim().compareTo(username2.trim()) >= 0) ? username1.trim() + "_" + username2.trim()
				: username2.trim() + "_" + username1.trim();


		Logger.warning("EchoServerMultiThreaded_manageRoom", "Looking for room '" + roomName + "'");
		Logger.warning("EchoServerMultiThreaded_manageRoom", "length: " + String.valueOf(roomName.length()));

		boolean alreadyExists = false;
		for (String k: listeRoom.keySet()) {
			Integer comp = k.compareTo(roomName);
			Logger.warning("EchoServerMultiThreaded_manageRoom", "Testing '" + k.trim() + "' -> " + String.valueOf(comp));
			Logger.warning("EchoServerMultiThreaded_manageRoom", "length: " + String.valueOf(k.trim().length()));
			if (comp == 0) {
				alreadyExists = true;
				Logger.warning("EchoServerMultiThreaded_manageRoom", "Room found!");
			}
		}
		
		if (!alreadyExists) {
			// room doesn't exist, create it
			Logger.warning("EchoServerMultiThreaded_manageRoom", "Creating room '" + roomName + "'");
			output = createRoom(roomName);
		} else {
			// room exists
			Room roomToJoin = listeRoom.get(roomName);
			output = Integer.toString(roomToJoin.port);
		}

		Logger.warning("EchoServerMultiThreaded_manageRoom", "port:" + output);

		return output;
	}

	/**
	 * Connect a user to a created room. Accepts its socket.
	 * 
	 * @param username1 the client asking for a room (current user)
	 * @param username2 the receiver
	 * @return the port of corresponding room.
	 */
	public static String connectRoom(String username1, String username2) {
		String output = "";
		String roomName = (username1.trim().compareTo(username2.trim()) >= 0) ? username1.trim() + "_" + username2.trim()
				: username2.trim() + "_" + username1.trim();
		try {

			// getting room
			Room roomToJoin = listeRoom.get(roomName);

			// getting room sockets
			ServerSocket clientSocketToJoin = roomToJoin.serversocket;
			Socket clientSocket = clientSocketToJoin.accept();
			SenderServer clientMulticastToListen = roomToJoin.multicast;
			Logger.debug("EchoServerMultiThreaded_connectRoom",
					"Connexion from:" + clientSocket.getInetAddress() + " with port " + roomToJoin.port);

			// manage room in another thread
			ClientThread ct = new ClientThread(clientSocket, clientMulticastToListen,username1.trim(),roomName);
			ct.start();

			output = "Room joined !";
		} catch (Exception e) {
			Logger.error("EchoServerMultiThreaded_connectRoom", e.getMessage());
		}
		return output;
	}

	/**
	 * Create room for two users.
	 * 
	 * @param username1 the client asking for a room (current user)
	 * @param username2 the receiver
	 * @return the port of corresponding room.
	 */
	public static String createRoom(String roomName) {
		String output = "";
		try {

			// create ServerSocket by searching next available port
			boolean portToFind = true;
			int portDepart = EchoServerMultiThreaded.maxPort + 1;
			ServerSocket listenSocket = null;
			while (portToFind) {
				try {
					listenSocket = new ServerSocket(portDepart);
					EchoServerMultiThreaded.maxPort = portDepart;
					Logger.debug("EchoServerMultiThreaded_createRoom", "Server ready");
					portToFind = false;
				} catch (IOException e) {
					portDepart++;
				}
			}

			// create multicast socket for room
			SenderServer listenMulticast = new SenderServer(ip_mul, portDepart);

			// create room and push instance to static data structure
			Room newRoom = new Room(listenSocket, listenMulticast, portDepart);

			Logger.debug("EchoServerMultiThreaded_createRoom", "added room: '" + roomName + "' (length " + String.valueOf(roomName.length()) + " )");
			listeRoom.put(roomName, newRoom);

			output = Integer.toString(portDepart);
		} catch (Exception e) {
			Logger.error("EchoServerMultiThreaded_createRoom", e.getMessage());
		}
		return output;
	}

	/**
	 * Add user to list if not already inside.
	 * 
	 * @param name   name of new user
	 * @param socket user's socket
	 * @return whether user has been added or not.
	 */
	public static boolean addUser(String name, Socket socket) {
		boolean output = false;

		// check if user already connected
		if (!listeUtilisateur.containsKey(name)) {
			listeUtilisateur.put(name, socket);
			output = true;
		}

		return output;
	}

	/**
	 * Remove user to list
	 * 
	 * @param name   name of the user
	 * @return whether user has been removed or not.
	 */
	public static boolean removeUser(String name) {
		boolean output = false;

		// check if user is still connected
		if (listeUtilisateur.containsKey(name)) {
			listeUtilisateur.remove(name);
			output = true;
		}

		return output;
	}

	/**
	 * Get all connected users.
	 * 
	 * @return serialized users list.
	 */
	public static String getConnectedUsers() {
		String output = String.join("_;_", listeUtilisateur.keySet());
		return output;
	}

	/**
	 * Password generation method.
	 * 
	 * @param len length of generated password
	 * @return generated passwords
	 */
	public static String generateRandomPassword(int len) {
		String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghi" + "jklmnopqrstuvwxyz!@#$%&";
		Random rnd = new Random();
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++)
			sb.append(chars.charAt(rnd.nextInt(chars.length())));
		return sb.toString();
	}
}
