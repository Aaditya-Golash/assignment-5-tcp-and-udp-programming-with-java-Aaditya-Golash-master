package tcp_client_server;
// This version allows a client to enter multiple lines until a ... is entered.
// Simple error handling by closing socket.
// This version implements threads to allow mutliple client connections at the same time.

import java.io.*;
import java.net.*;

public class TCPServerThread {
	public static void main(String []argv) throws Exception
	{
		@SuppressWarnings("resource")
		ServerSocket welcomeSocket = new ServerSocket(6789);

		while (true)
		{
			/*
			 * When there is a request for a connection, accept the inbound connection
			 * but pass the connection object off into a separate thread.  This was the
			 * system will be able to keep on listening for more connections.
			 * 
			 * Use this with TCPClient2.java.
			 * 
			 */
			Socket connectionSocket = welcomeSocket.accept();
			UserRequest request = new UserRequest(connectionSocket);
			
			/* 
			 * Hava a look at the javadoc for Socket as there is a lot of additional
			 * informaton you can get from the Socket obj.
			 */
			System.out.println("Accepting an inbound request from:");
			System.out.println("Port:  " +connectionSocket.getPort());
			System.out.println("IP:  " +connectionSocket.getInetAddress());
			
		    // Create a new thread to process the request.
		    Thread thread = new Thread(request);
		    // Start the thread.
		    thread.start();
		}
	}
}

class UserRequest implements Runnable
{
	private Socket connectionSocket;
	private String goodByeString = "bye!\n";


	public UserRequest(Socket socket) throws Exception
	{
		this.connectionSocket = socket;
	}

	// Implement the run() method of the Runnable interface.
	public void run()
	{
		try {
			execute();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	/* 
	 * This will run in a separate thread 
	 */
	private void execute() throws Exception
	{
		String clientSentence;
		String capitalizedSentence;
		String terminator = "...";

		BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
		DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

		clientSentence = inFromClient.readLine();
		while (!clientSentence.equals(terminator))
		{
			capitalizedSentence = clientSentence.toUpperCase() + '\n';
			outToClient.writeBytes(capitalizedSentence);
			clientSentence = inFromClient.readLine();
		}
		outToClient.writeBytes(goodByeString);
		System.out.println("Closing socket connection to " + connectionSocket.getInetAddress() +":"+connectionSocket.getPort());
		connectionSocket.close();
		
	}
}