package IMServer;
/*
IMServer.java - Instant Message server using UDP and TCP communication.

For portability, server information is stored in flat-files although in practice this information would be in a database.
*/

import java.io.* ;
import java.net.* ;
import java.util.* ;

public class IMServer
{
	private HashMap<String, StatusRecord> userStatus;
	private static int TCPWelcomePort = 1234;
	public static String onlineStatus = "100 ONLINE";
	public static String offlineStatus = "101 OFFLINE";
	public static String awayStatus = "102 AWAY";

	public static void main(String argv[]) throws Exception
    {
		IMServer server = new IMServer();
		server.execute();
	}

	public IMServer()
	{	userStatus = new HashMap<String, StatusRecord>();
	}

	public void execute()
	{
		// Create thread for UDP processing and leave this thread for TCP processing
		UDPProcessor udpProc = new UDPProcessor(this);
		Thread udpThread = new Thread(udpProc);
		udpThread.start();
		ServerSocket socket = null
				;

		// This thread starts an infinite loop looking for TCP requests
		try
		{
			// Establish the listen socket.
			socket = new ServerSocket(TCPWelcomePort);

			// Process requests in an infinite loop.
			while (true)
			{
		    	// Listen for a TCP connection request.
		    	Socket connection = socket.accept();

		    	// Construct an object to process the HTTP request message.
		    	IMRequest request = new IMRequest(connection);

		    	// Create a new thread to process the request.
		    	Thread thread = new Thread(request);

			    // Start the thread.
			    thread.start();
			
			}
			
	    }
		catch (Exception e)
		{	System.out.println(e); }
		finally
		{
			/*
			 * Resource cleanup
			 */
		    try {
		    	if (socket != null)
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}

	public void updateUserStatus(String userId, StatusRecord rec)
	{	// Server would benefit from a timeout mechanism
		userStatus.put(userId, rec);
	}

	public StatusRecord getUserStatus(String userId)
	{
		return (StatusRecord) userStatus.get(userId);
	}

	public ArrayList<String> getBuddyList(String userId)
	{
		// Read buddy list file and look up status of known users.
		ArrayList<String> blist = readBuddyList(userId);
		if (blist != null)
		{	ArrayList<String> result = new ArrayList<String>(blist.size());
			for (int i=0; i < blist.size(); i++)
			{	String buddyId = (String) blist.get(i);
				StatusRecord buddyStatus = (StatusRecord) userStatus.get(buddyId);
				if (buddyStatus == null)
					result.add(buddyId+" "+offlineStatus);
				else
					result.add(buddyId+" "+onlineStatus+" "+buddyStatus.IPaddress);
			}
		}
		return blist;
	}

	public static ArrayList<String> readBuddyList(String userId)
	{
		try
		{	BufferedReader br = new BufferedReader(new FileReader(userId+".txt"));
			ArrayList<String> a = new ArrayList<String>();

			String line = br.readLine();
			while (line != null)
			{	a.add(line);
				line = br.readLine();
			}
			br.close();
			return a;
		}
		catch (Exception e)
		{	return null; }
	}

	public static boolean updateBuddyList(boolean isAdd, String userId, String buddyId)
    {
    	try
		{	// Read all names and make sure it is not already there, if not then add it
    		// Simple algorithm: Read whole file to determine if buddy exists.  Add at end if doesn't.
    		// Note that this version is not fail safe if a crash occurs during file update.
			BufferedReader br = new BufferedReader(new FileReader(userId+".txt"));
			StringBuffer buf = new StringBuffer(1000);

			String line = br.readLine();
			boolean foundBuddy = false;
			while (line != null)
			{
				if (buddyId.equals(line))
				{	if (isAdd)				// Do not copy over buddy if performing delete
					{	buf.append(line+"\n");
						foundBuddy = true;
					}
				}
				else
					buf.append(line+"\n");
				line = br.readLine();
			}
			if (isAdd && !foundBuddy)
				buf.append(buddyId+"\n");
			br.close();

			// Now write out buffer
			BufferedWriter bw = new BufferedWriter(new FileWriter(userId+".txt"));
			bw.write(buf.toString());
			bw.close();
			if (isAdd && foundBuddy)
				return false;
			return true;
		}
		catch (Exception e)
		{	return false;}
	}

	public static boolean existingUser(String userId)
    {	// Returns true only if user id exists on system
    	File userFile = new File(userId+".txt");
		return userFile.exists();
    }
}

class StatusRecord
{	public String IPaddress;
	public int port;
	public String status;
	public String buddyId;
}

class IMRequest implements Runnable
{
    private final static String CRLF = "\r\n";
	private final static String response200 = "200 OK"+CRLF;
	private final static String response201 = "201 INVALID"+CRLF;
	private final static String response202 = "202 NO SUCH USER"+CRLF;
	private final static String response203 = "203 USER EXISTS"+CRLF;
	private Socket socket;
	private InputStream is = null;
	private DataOutputStream os = null;
	private BufferedReader br = null;

    // Constructor
    public IMRequest(Socket socket) throws Exception
    {
		this.socket = socket;
    }

    // Implement the run() method of the Runnable interface.
    public void run()
    {
    	try{
    		processRequest();
    	}
    	finally
    	{
			try {
				System.out.println("Closing socket local:" + this.socket.getLocalPort()+ " Remote:" + this.socket.getPort());
				this.socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    }

    public boolean verifyUserId(String uid)
    {	return true;
    }

    private void sendResponse(String st)
    {	// Sends one line response and closes socket and input/output buffers
    	try {
    		os.writeBytes(st);
    	}
    	catch (Exception e)
    	{
    		System.out.println(e);
    	}
    	finally
    	{	try
    		{ br.close(); }
    		catch (Exception e) { System.out.println(e); }
    		try
    		{ os.close(); }
    		catch (Exception e) { System.out.println(e); }
    		try
    		{ socket.close(); }
    		catch (Exception e) { System.out.println(e); }
    	}
    }

    private void processRequest()
    {
		String requestLine = null;

    	try
    	{
    		is = socket.getInputStream();
    		os = new DataOutputStream(socket.getOutputStream());
    		br = new BufferedReader(new InputStreamReader(is));
    		// Retrieve client request
    		requestLine = br.readLine();
    		System.out.println(requestLine);
    	}
    	catch (Exception e)
    	{	System.out.println(e);
    		return;		// Ignore this exception
    	}

        // Extract the filename from the request line.
        StringTokenizer tokens = new StringTokenizer(requestLine);
		if (!tokens.hasMoreTokens())
			return;			// Must have be empty line - ignore

        String command = tokens.nextToken().toUpperCase();  // Determine command (should be REG, ADD, DEL)
		String userId = null;

		if (!tokens.hasMoreTokens())
		{	// Invalid command/request as every command has at least one parameter
			sendResponse(response201);
			return;
		} // end not enough parameters

		userId = tokens.nextToken().toLowerCase();
		if (!verifyUserId(userId))			// Determine if user id is valid (only characters and numbers)
		{	sendResponse(response201);
			return;
		}

		// Check command
		if (command.equals("REG"))
		{	// Register user id
			// Determine if user id exists
			if (IMServer.existingUser(userId))
			{	sendResponse(response203);	// User already exists
				return;
			}
			else
			{	if (registerUser(userId))
					sendResponse(response200);
				else
					sendResponse(response201);
				return;
			}
		}
		else if (command.equals("ADD") || command.equals("DEL"))
		{	// Check if valid existing user id
			if (!IMServer.existingUser(userId))
			{	sendResponse(response202);	// User does not exist
				return;
			}

			// Check if have valid existing buddy id
			if (!tokens.hasMoreTokens())
			{	sendResponse(response201);
				return;
			} // no buddy id parameter
			String buddyId = tokens.nextToken().toLowerCase();

			if (!IMServer.existingUser(buddyId))
			{	sendResponse(response202);
				return;
			}
			boolean isAdd = command.equals("ADD");
			if (buddyId.equals(userId))
			{	sendResponse(response201);
				return;
			}
			if (IMServer.updateBuddyList(isAdd, userId, buddyId))
				sendResponse(response200);
			else
				sendResponse(response201);
		}
		else
		{	// Invalid command/request
			sendResponse(response201);
		}
		
    }

    private boolean registerUser(String userId)
    {	// Returns true if successfully register a user
    	File userFile = new File(userId+".txt");
    	try {
			userFile.createNewFile();	// Create empty text file for this user
			return true;
		}
		catch (Exception e)
		{	return false; }
    }

    }

class UDPProcessor implements Runnable
{
	private static int UDPPort = 1235;
	private IMServer server;

	public UDPProcessor(IMServer s)
	{
		server = s;
	}

    public void run()
    {
    	try
    	{
	    	DatagramSocket serverSocket = new DatagramSocket(UDPPort);

			byte[] receiveData = new byte[1024];

			while(true)
			{
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				serverSocket.receive(receivePacket);

				// System.out.println("Length: "+receivePacket.getLength()+" "+receivePacket.getData());
				String request = new String(receivePacket.getData(), 0, receivePacket.getLength());
				System.out.println(request);

				// Get client IP and port
				InetAddress IPAddress = receivePacket.getAddress();
				int port = receivePacket.getPort();

				// Handle request
				StringTokenizer tokens = new StringTokenizer(request);
				if (tokens.countTokens() >= 2)
				{	String command = tokens.nextToken().toUpperCase();
					String userId = tokens.nextToken().toLowerCase();
					if (IMServer.existingUser(userId))
					{	// Must be valid user and command - ignore request otherwise
						if (command.equals("GET"))
						{	ArrayList<String> result = server.getBuddyList(userId);
							StringBuffer b = new StringBuffer();
							for (int i=0; i < result.size(); i++)
							{	String buddyId = (String) result.get(i);
								StatusRecord rec = server.getUserStatus(buddyId);
								if (rec == null)
									b.append(buddyId+" "+IMServer.offlineStatus+" unknown unknown\n");
								else
									b.append(buddyId+" "+rec.status+" "+rec.IPaddress+" "+rec.port+"\n");
							}
							System.out.println(b.toString());
							DatagramPacket sendPacket = new DatagramPacket(b.toString().getBytes(), b.length(), IPAddress, port);
							serverSocket.send(sendPacket);
						} // get command
						else if (command.equals("SET"))
						{	if (tokens.countTokens() == 3)	// Gives total of 5 tokens as already took out two
							{	String statusCode = tokens.nextToken();
								String statusMsg = tokens.nextToken();
								String portNum = tokens.nextToken();
								String statusMessage = statusCode+" "+statusMsg;

								if (statusMessage.equals(IMServer.onlineStatus) || statusMessage.equals(IMServer.offlineStatus)
										|| statusMessage.equals(IMServer.awayStatus) )
								{
									try {
										int msgport = Integer.parseInt(portNum);
										StatusRecord rec = new StatusRecord();
										rec.status = statusMessage;
										rec.IPaddress = IPAddress.getHostAddress();
										rec.port = msgport;
										server.updateUserStatus(userId, rec);
										// No response required from server
									}
									catch (NumberFormatException e)
									{ // Ignore exception but do not process invalid request
									}

								} // valid status message
							} // right number of tokens
						} // set command
					} // existing user
				} // minimum # of tokens for all commands
			}
		}
    	catch (Exception e)
    	{
    		System.out.println(e);
    	}
    }
}