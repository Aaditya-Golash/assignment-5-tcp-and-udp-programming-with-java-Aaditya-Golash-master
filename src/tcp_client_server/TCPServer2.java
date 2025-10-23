package tcp_client_server;
// This version allows a client to enter multiple lines until a ... is entered.
// Simple error handling by closing socket
import java.io.*;
import java.net.*;

/**
 * Unlike TCPServer.java, this socket server leaves the socket open until a terminator
 * is sent (send three dots ... on a single line) which will then close the socket.  This code works with 
 * TCPClient2.java
 *
 */
class TCPServer2 {
	public static void main(String []argv) throws Exception
	{
		String clientSentence;
		String capitalizedSentence;
		String terminator = "...";
		String goodByeString = "bye!\n";
		
		/*
		 * Setup a 'welcome' socket on port 6789.  This will listen for incoming requests 
		 * from a remote machine.  Remember that a socket is defined by a 4-tuple, being 
		 * <SRC IP, SRC Port, DEST IP, DEST Port>.  This means that we can accept multiple
		 * incoming requests to the same port as long as they originate from different src 
		 * ports.
		 * 
		 * This code will only accept one inbound connection.
		 */
		@SuppressWarnings("resource")
		ServerSocket welcomeSocket = new ServerSocket(6789);

		while (true)
		{
			Socket connectionSocket = welcomeSocket.accept();
			
			System.out.println("Accepting an inbound request from:");
			System.out.println("Port:  " +connectionSocket.getPort());
			System.out.println("IP:  " +connectionSocket.getInetAddress());
			
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
			DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

			try 
			{
				clientSentence = inFromClient.readLine();
				/*
				 * Look and wait for the terminator 
				 */
				while (!clientSentence.equals(terminator))
				{
					capitalizedSentence = clientSentence.toUpperCase() + '\n';
					outToClient.writeBytes(capitalizedSentence);
					clientSentence = inFromClient.readLine();
				}
				outToClient.writeBytes(goodByeString);
				System.out.println("Closing socket.");
			}
			catch (Exception e)
			{	// Close client socket on any errors
				connectionSocket.close();
			}
		}
	}
}