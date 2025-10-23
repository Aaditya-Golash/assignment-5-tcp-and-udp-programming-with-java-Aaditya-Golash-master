package tcp_client_server;
import java.io.*;
import java.net.*;

/**
 * 
 * Sample socket server code in Java that will accept a new connection and 
 * capitalize the string and return through the socket.  This will close the 
 * socket at the end of the message 
 * 
 * Use with TCPClient.java
 * 
 */
class TCPServer {
	public static void main(String []argv) throws Exception
	{
		String clientSentence;
		String capitalizedSentence;
		
		/*
		 * Setup a 'welcome' socket on port 6789.  This will listen for incoming requests 
		 * from a remote machine.  Remember that a socket is defined by a 4-tuple, being 
		 * <SRC IP, SRC Port, DEST IP, DEST Port>.  This means that we can accept multiple
		 * incoming requests to the same port as long as they originate from different src 
		 * ports.
		 */
		@SuppressWarnings("resource")
		ServerSocket welcomeSocket = new ServerSocket(6789);

		while (true)
		{
			/*	
			 * Accept the incoming request
			 */
			Socket connectionSocket = welcomeSocket.accept();
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
			DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

			clientSentence = inFromClient.readLine();
			capitalizedSentence = clientSentence.toUpperCase() + '\n';
			outToClient.writeBytes(capitalizedSentence);
			connectionSocket.close();
		}
		
	}
}