package udp_client_server;
/* 
 * Client can send many sentences before terminating using the same deliminator.
 */

import java.io.*;
import java.net.*;

class UDPClient2 {
	public static void main(String args[]) throws Exception
	{
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

		/*
		 * Constructs a datagram socket and binds it to any available port on the local host machine.
		 * This will pick a port for you.
		 */
		DatagramSocket clientSocket = new DatagramSocket();
		InetAddress IPAddress = InetAddress.getByName("localhost");

		byte[] sendData = new byte[1024];
		byte[] receiveData = new byte[1024];
		String terminator = "...";

		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

		String sentence = inFromUser.readLine();
		while (!sentence.equals(terminator))
		{	// Send request
		
			sendData = sentence.getBytes();
			/*
			 * Send UDP datagram out from the created UDP socket
			 */
			DatagramPacket sendPacket = new DatagramPacket(sendData, sentence.length(), IPAddress, 9876);
			clientSocket.send(sendPacket);

			/*
			 * Receive a process reply, listening for a response from server.
			 */
			clientSocket.receive(receivePacket);
			String modifiedSentence = new String(receivePacket.getData(),0,receivePacket.getLength());
			System.out.println("FROM SERVER: "+modifiedSentence);
			sentence = inFromUser.readLine();
		}

		clientSocket.close();
	}
}