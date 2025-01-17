/***
 * ClientThread
 * Example of a TCP server
 * Date: 14/12/08
 * Authors:
 */

package stream;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class ClientThread extends Thread {

	private Socket clientSocket;
	private SenderServer multicast;
	private String wanted_username;
	private String roomName;
	private long thread_id;
	private boolean removeUser = true;

	ClientThread(Socket s, SenderServer multicast, String wanted_username, String roomName) {
		this.clientSocket = s;
		this.wanted_username = wanted_username;
		this.roomName = roomName;
		this.multicast = multicast;
		this.thread_id = Thread.currentThread().getId();
		showThreadInfos();
	}

	ClientThread(Socket s) {
		this.clientSocket = s;
		this.thread_id = Thread.currentThread().getId();
		showThreadInfos();
		this.multicast = null;
	}

	/**
	 * Debug method to print thread instance informations
	 */
	private void showThreadInfos() {
		Logger.debug("ClientThread_construct", "Thread name: " + Thread.currentThread().getName());
		Logger.debug("ClientThread_construct", "Thread ID: " + this.thread_id);
	}

	/**
	 * receives a request from client then sends an echo to the client
	 * 
	 * @param clientSocket the client socket
	 **/
	public void run() {
		try {

			// init buffers
			BufferedReader socIn = null;
			String output = null;
			socIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			PrintStream socOut = new PrintStream(clientSocket.getOutputStream());

			String line = "init";
			// joining a room means get old conversation messages
			if(roomName != null){
				List<String> oldConversation = Message.getHistoricConversation(roomName);
				for (String message : oldConversation) {
					socOut.println(message);
				}
				// signaling that all the old messages have been received
				socOut.println("END OF OLD MESSAGES");
				Logger.debug("ClientThread_run", "Old messages sent");

			}
			while (true && line != null) {

				// waiting and reading client input
				Logger.warning("ClientThread_run", thread_id + " waiting for a line ");
				line = socIn.readLine();
				Logger.warning("ClientThread_run", thread_id + " line received  \" " + line + "\"");

				// analyzing client input
				if (line != null) {

					if (line.contains("UsernameIs")) {
						Logger.debug("ClientThread_run", "USERNAMEIS command");
						Logger.debug("ClientThread_run", "input: " + line);
						wanted_username = line.split(" ")[1];
						Boolean status = EchoServerMultiThreaded.addUser(wanted_username, clientSocket);
						output = "error";
						if (status) {
							output = "success";
							EchoServerMultiThreaded.notifyConnection(wanted_username);
						}
						Logger.debug("ClientThread_run", "output through Socket: " + output);
						socOut.println(output);

					} else if (line.contains("GetUsers")) {
						output = EchoServerMultiThreaded.getConnectedUsers();
						socOut.println(output);

					} else if (line.contains("ConnectRoom")) {
						Logger.debug("ClientThread_run", "CONNECT ROOM command");
						Logger.debug("ClientThread_run", "input: " + line);
						String wantedReceiver = line.split(" ")[1];
						output = EchoServerMultiThreaded.connectRoom(wanted_username, wantedReceiver);
						// Break ? todo : on garde le thread du main ou non?
						
						break;

					} else if (line.contains("ConnectGroup")) {
						Logger.debug("ClientThread_run", "CONNECTGROUP command");
						Logger.debug("ClientThread_run", "input: " + line);
						String wanted_group = line.split(" ")[1];
						output = EchoServerMultiThreaded.connectGroup(wanted_group, wanted_username);
						// Break ? todo : on garde le thread du main ou non?
						
						break;

					} else if (line.contains("joinConversation")) {
						Logger.debug("ClientThread_run", "JOINCONVERSATION command");
						Logger.debug("ClientThread_run", "input: " + line);
						String wantedReceiver = line.split(" ")[1];
						output = EchoServerMultiThreaded.manageRoom(wanted_username, wantedReceiver);
						// On renvoie le port
						socOut.println(output);
						Logger.debug("ClientThread_run", "output through Socket: " + output);
						// socOut.println(output);

					} else if (line.contains("joinGroup")) {
						Logger.debug("ClientThread_run", "JOINGROUP command");
						Logger.debug("ClientThread_run", "input: " + line);
						String group_name = line.split(" ")[1];
						output = EchoServerMultiThreaded.manageGroup(group_name);
						// On renvoie le port
						socOut.println(output);
						Logger.debug("ClientThread_run", "output through Socket: " + output);
						// socOut.println(output);

					} else if (line.contains("MESSAGE")) {
						Logger.debug("ClientThread_run", "MESSAGE command");
						Logger.debug("ClientThread_run", "input through MulticastSocket: " + line);

						Logger.debug("ClientThread_run", "t_id: " + thread_id);

						String[] splitted = line.split(" ");
						String str_message = String.join(" ", Arrays.copyOfRange(splitted, 1, splitted.length));

						Logger.debug("ClientThread_run", "str_message: " + str_message);

						if (multicast == null) {
							Logger.warning("ClientThread_run", "multicast is null!");
						} else {
							LocalDateTime now = LocalDateTime.now();
							// Utilisation de la classe Message qui s'occupe de la mise en forme
							// et de la persistance des messages
							Message message = new Message(now,str_message,wanted_username,roomName);
							multicast.send(message.getMessage());
							// Utilisation d'un thread pour la persistance des données
							message.run();
						}
						// socOut.println(line);
					}
				}
			}

			if(line == null){
				Boolean removed = EchoServerMultiThreaded.removeUser(wanted_username);
				if(removed){
					Logger.warning("ClientThread_run", "Remove user from connected user list "+ wanted_username);
					EchoServerMultiThreaded.notifyDisconnection(wanted_username);
				}
			}

			Logger.warning("ClientThread_run", "Loop in thread " + thread_id + " ended");

		} catch (Exception e) {
			Logger.error("ClientThread_run: thread_id " + thread_id, e.getMessage());
		}
	}

}
