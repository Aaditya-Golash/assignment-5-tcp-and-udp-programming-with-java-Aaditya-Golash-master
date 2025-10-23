package udp_client_server;
import java.io.*;
import java.net.*;

/*
 * Setup a 'welcome' UDP connection on port 9876.  This will listen for incoming requests 
 * from a remote machine.  Remember that a UDP connection is defined by a 2-tuple of IP address
 * and port number.  This means that any number of computers can send data to the port 
 * at the same time and it is not connection orientated.
 * 
 */
class UDPServer {
	public static void main(String args[]) throws Exception
	{
		@SuppressWarnings("resource")
		/*
		 * Create a UDP socket on port 9876
		 */
		DatagramSocket serverSocket = new DatagramSocket(9876);

		byte[] receiveData = new byte[1024];
		byte[] sendData  = new byte[1024];

		while(true)
		{
			/*
			 * Create object to receive datagram
			 */
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			/*	
			 * Receive datagram from socket and extract data
			 */
			serverSocket.receive(receivePacket);
			
			/*
			 * Reads in the entire buffer
			 */
			String sentence = new String(receivePacket.getData());

			/*
			 * Get the IP address and port of the sender from the 
			 */
			InetAddress IPAddress = receivePacket.getAddress();
			int port = receivePacket.getPort();
			System.out.println("Sending response to " + IPAddress + ":"+port);

		  	String capitalizedSentence = sentence.toUpperCase()+"\0";
		  	
			sendData = capitalizedSentence.getBytes();
			/*
			 * Constuct a UDP datagram to send back to sender
			 */
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);

			System.out.println("Recevied: "+sentence+"\nSent: "+capitalizedSentence);

			serverSocket.send(sendPacket);
		}
	}
}