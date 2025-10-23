package udp_client_server;
import java.io.*;
import java.net.*;

/*
 * Creates a UDP datagram and sends to server (this assumes that client and server 
 * are running on local machine.
 */
class UDPClient {
	
	public static void main(String args[]) throws Exception
	{
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

		/* 
		 * Create new UDP datagram socket.   Constructs a datagram socket and binds it to any 
		 * available port on the local host machine.  This will pick a port for you.  
		 */
		DatagramSocket clientSocket = new DatagramSocket();
		/*
		 * Get IP address for locatlhost
		 */
		InetAddress IPAddress = InetAddress.getByName("localhost");

		byte[] sendData = new byte[1024];
		byte[] receiveData = new byte[1024];

		String sentence = inFromUser.readLine();
		/*
		 * Data must be in a byte stream
		 */
		sendData = sentence.getBytes();

		/*
		 * Build the UDP datagram to the IP address at port 9876
		 */
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
		/* 
		 * Send the byte stream from the created UDP socket
		 */
		clientSocket.send(sendPacket);
		
		/* 
		 * and as this is not a duplex connection, create a UDP connection to listen for the 
		 * data coming back from the server. 
		 */
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		/*
		 * wait for data
		 */
		clientSocket.receive(receivePacket);
		/* 
		 * Convert back from byte stream to string for pretty printing.
		 */
		String modifiedSentence = new String(receivePacket.getData());

		System.out.println("FROM SERVER:" + modifiedSentence);
		clientSocket.close();
	}
}