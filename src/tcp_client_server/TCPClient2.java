package tcp_client_server;
// This version allows a client to send multiple requests to server until sending a terminator.

import java.io.*;
import java.net.*;

class TCPClient2 {
	public static void main(String []argv) throws Exception
	{
		String sentence;
		String modifiedSentence;
		String terminator = "...";

		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
		Socket clientSocket = new Socket("localhost", 6789);
		DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
		BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

	
		do
		{
			sentence = inFromUser.readLine();
			outToServer.writeBytes(sentence+'\n');
			modifiedSentence = inFromServer.readLine();
			System.out.println("FROM SERVER: "+modifiedSentence);
		}while (!sentence.equals(terminator));

		clientSocket.close();
	}
}