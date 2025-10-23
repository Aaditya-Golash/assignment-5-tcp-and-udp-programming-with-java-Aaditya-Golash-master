package IMClient;



import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class IMClient {
	
	public static String serverAddress = "localhost";
	public static int TCPServerPort = 1234;					
	
	

	public static int TCPMessagePort = 1248;				
	
	public static String onlineStatus = "100 ONLINE";
	public static String offlineStatus = "101 OFFLINE";

	private BufferedReader reader;							

	
	private String userId;
	private String status;

	
	private volatile boolean running = true;
	private Thread udpThread;
	private Thread tcpMessengerThread;
	private DatagramSocket udpSocket;
	private ServerSocket serverSocket; 

	
	private ArrayList<BuddyStatusRecord> buddyList = new ArrayList<BuddyStatusRecord>();

	
	private volatile Socket pendingIncomingSocket = null;
	private volatile Socket activeChatSocket = null;
	private volatile boolean chatMode = false;
	private BufferedReader activeChatIn = null;
	private DataOutputStream activeChatOut = null;
	private Thread chatReceiverThread = null;

	public static void main(String []argv) throws Exception
	{
		IMClient client = new IMClient();
		client.execute();
	}

	public IMClient()
	{
		
		userId = null;
		status = null;
	}


	public void execute()
	{
		initializeThreads();

		String choice;
		reader = new BufferedReader(new InputStreamReader(System.in));

		printMenu();
		choice = getLine().toUpperCase();

		while (!choice.equals("X"))
		{
			if (choice.equals("Y"))
			{	
				acceptConnection();
			}
			else if (choice.equals("N"))
			{	
				rejectConnection();
			}
			else if (choice.equals("R"))				
			{	registerUser();
			}
			else if (choice.equals("L"))		
			{	loginUser();
			}
			else if (choice.equals("A"))		
			{	addBuddy();
			}
			else if (choice.equals("D"))		
			{	deleteBuddy();
			}
			else if (choice.equals("S"))		
			{	buddyStatus();
			}
			else if (choice.equals("M"))		
			{	buddyMessage();
			}
			else
				System.out.println("Invalid input!");

			printMenu();
			choice = getLine().toUpperCase();
		}
		shutdown();
	}

	private void initializeThreads()
	{
		try {
			
			serverSocket = new ServerSocket(0);
			TCPMessagePort = serverSocket.getLocalPort();
			TCPMessenger messenger = new TCPMessenger(this, serverSocket);
			tcpMessengerThread = new Thread(messenger);
			tcpMessengerThread.setDaemon(true);
			tcpMessengerThread.start();

			
			udpThread = new Thread(new Runnable() {
				public void run() { udpLoop(); }
			});
			udpThread.setDaemon(true);
			udpThread.start();
		}
		catch (IOException e) {
			System.out.println(e);
		}
	}

	private void registerUser()
	{	
		System.out.print("Enter user id: ");
		String uid = getLine();
		if (uid == null || uid.trim().length() == 0) {
			System.out.println("201 INVALID");
			return;
		}
		uid = uid.trim().toLowerCase();
		System.out.println("Registering user id: "+uid);
		String response = tcpRequest("REG "+uid);
		if (response != null) System.out.println(response);
		if (response != null && response.startsWith("200")) {
			userId = uid;
			status = onlineStatus;
		}
	}

	private void loginUser()
	{	
		System.out.print("Enter user id: ");
		userId = getLine();
		System.out.println("User id set to: "+userId);
		status = onlineStatus;
	}

	private void addBuddy()
	{	
		if (userId == null || userId.trim().isEmpty()) {
			System.out.println("No user id. Please register or login first.");
			return;
		}
		System.out.print("Enter buddy id: ");
		String buddyId = getLine();
		if (buddyId == null || buddyId.trim().isEmpty()) {
			System.out.println("201 INVALID");
			return;
		}
		buddyId = buddyId.trim().toLowerCase();
		String response = tcpRequest("ADD "+userId.toLowerCase()+" "+buddyId);
		if (response != null) System.out.println(response);
	}

	private void deleteBuddy()
	{	
		if (userId == null || userId.trim().isEmpty()) {
			System.out.println("No user id. Please register or login first.");
			return;
		}
		System.out.print("Enter buddy id: ");
		String buddyId = getLine();
		if (buddyId == null || buddyId.trim().isEmpty()) {
			System.out.println("201 INVALID");
			return;
		}
		buddyId = buddyId.trim().toLowerCase();
		String response = tcpRequest("DEL "+userId.toLowerCase()+" "+buddyId);
		if (response != null) System.out.println(response);
	}

	private void buddyStatus()
	{	
		System.out.println("My buddy list:");
		ArrayList<BuddyStatusRecord> snapshot;
		synchronized(this) {
			snapshot = new ArrayList<BuddyStatusRecord>(buddyList);
		}
		for (BuddyStatusRecord b : snapshot) {
			System.out.println(b.toString());
		}
	}

	private void buddyMessage()
	{	
		
		if (userId == null || userId.trim().isEmpty()) {
			System.out.println("No user id. Please register or login first.");
			return;
		}
		if (activeChatSocket != null || chatMode) {
			System.out.println("Already in a chat. Finish current chat first.");
			return;
		}
		System.out.print("Enter buddy id: ");
		String bid = getLine();
		if (bid == null || bid.trim().isEmpty()) return;
		bid = bid.trim();
		BuddyStatusRecord target = findBuddy(bid);
		if (target == null) {
			System.out.println("Buddy not found in list. Try S to refresh.");
			return;
		}
		if (!target.isOnline() || target.IPaddress == null || target.IPaddress.startsWith("unknown")) {
			System.out.println("Buddy is not online.");
			return;
		}
		try {
			int port = Integer.parseInt(target.buddyPort);
			System.out.println("Attempting to connect...");
			Socket s = new Socket(target.IPaddress, port);
			BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			DataOutputStream out = new DataOutputStream(s.getOutputStream());
			
			out.writeBytes("HELLO "+userId+"\n");
			String reply = in.readLine();
			if (reply != null && reply.toUpperCase().startsWith("OK")) {
				System.out.println("Buddy accepted connection.");
				enterChatMode(s, in, out);
			} else {
				System.out.println("Buddy declined connection.");
				s.close();
			}
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	private void shutdown()
	{	
		running = false;
		
		try { sendUdp("SET "+(userId==null?"":userId)+" "+offlineStatus+" "+TCPMessagePort); } catch (Exception e) { }
		try { if (udpSocket != null) udpSocket.close(); } catch (Exception e) { }
		try { if (activeChatSocket != null) activeChatSocket.close(); } catch (Exception e) { }
		try { if (serverSocket != null) serverSocket.close(); } catch (Exception e) { }
	}

	private void acceptConnection()
	{	
		
		
		Socket s = pendingIncomingSocket;
		pendingIncomingSocket = null;
		if (s == null) {
			System.out.println("No incoming connection to accept.");
			return;
		}
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			DataOutputStream out = new DataOutputStream(s.getOutputStream());
			
			try { in.readLine(); } catch (Exception e) { }
			out.writeBytes("OK\n");
			System.out.println("Connection accepted.");
			enterChatMode(s, in, out);
		} catch (Exception e) {
			System.out.println(e);
			try { s.close(); } catch (Exception ex) { }
		}
	}

	private void rejectConnection()
	{	
		
		Socket s = pendingIncomingSocket;
		pendingIncomingSocket = null;
		if (s == null) {
			System.out.println("No incoming connection to reject.");
			return;
		}
		try {
			
			DataOutputStream out = new DataOutputStream(s.getOutputStream());
			out.writeBytes("NO\n");
			s.close();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	private String getLine()
	{	
		String inputLine = null;
		  try{
			  inputLine = reader.readLine();
		  }catch(IOException e){
			 System.out.println(e);
		  }
	 	 return inputLine;
	}

	private void printMenu()
	{	System.out.println("\n\nSelect one of these options: ");
		System.out.println("  R - Register user id");
		System.out.println("  L - Login as user id");
		System.out.println("  A - Add buddy");
		System.out.println("  D - Delete buddy");
		System.out.println("  M - Message buddy");
		System.out.println("  S - Buddy status");
		System.out.println("  X - Exit application");
		System.out.print("Your choice: ");
	}

	
	private String tcpRequest(String line)
	{
		Socket s = null;
		try {
			s = new Socket(serverAddress, TCPServerPort);
			DataOutputStream out = new DataOutputStream(s.getOutputStream());
			BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			out.writeBytes(line+"\r\n");
			String resp = in.readLine();
			try { in.close(); } catch (Exception e) {}
			try { out.close(); } catch (Exception e) {}
			try { s.close(); } catch (Exception e) {}
			return resp;
		} catch (Exception e) {
			System.out.println(e);
			try { if (s != null) s.close(); } catch (Exception ex) {}
			return null;
		}
	}

	
	private BuddyStatusRecord findBuddy(String bid)
	{
		ArrayList<BuddyStatusRecord> snapshot;
		synchronized(this) { snapshot = new ArrayList<BuddyStatusRecord>(buddyList); }
		for (BuddyStatusRecord b : snapshot) {
			if (b.buddyId.equalsIgnoreCase(bid)) return b;
		}
		return null;
	}

	
	private void udpLoop()
	{
		try {
			udpSocket = new DatagramSocket();
			udpSocket.setSoTimeout(3000);
			InetAddress serverIP = InetAddress.getByName(serverAddress);
			byte[] recvBuf = new byte[4096];
			while (running) {
				if (userId != null && userId.trim().length() > 0) {
					
					sendUdp(udpSocket, serverIP, 1235, "SET "+userId.toLowerCase()+" "+(status==null?onlineStatus:status)+" "+TCPMessagePort);
					
					sendUdp(udpSocket, serverIP, 1235, "GET "+userId.toLowerCase());
					try {
						DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
						udpSocket.receive(receivePacket);
						String payload = new String(receivePacket.getData(), 0, receivePacket.getLength());
						updateBuddyListFromPayload(payload);
					} catch (SocketTimeoutException te) {
						
					}
				}
				try { Thread.sleep(10000); } catch (InterruptedException ie) { }
			}
		} catch (Exception e) {
			
		}
	}

	private void updateBuddyListFromPayload(String payload)
	{
		ArrayList<BuddyStatusRecord> newList = new ArrayList<BuddyStatusRecord>();
		BufferedReader br = new BufferedReader(new StringReader(payload));
		try {
			String line = br.readLine();
			while (line != null) {
				line = line.trim();
				if (line.length() > 0) {
					StringTokenizer t = new StringTokenizer(line);
					if (t.countTokens() >= 4) {
						BuddyStatusRecord rec = new BuddyStatusRecord();
						rec.buddyId = t.nextToken();
						
						String s1 = t.hasMoreTokens()? t.nextToken() : "";
						String s2 = t.hasMoreTokens()? t.nextToken() : "";
						rec.status = (s1+" "+s2).trim();
						rec.IPaddress = t.hasMoreTokens()? t.nextToken() : "unknown";
						rec.buddyPort = t.hasMoreTokens()? t.nextToken() : "unknown";
						newList.add(rec);
					}
				}
				line = br.readLine();
			}
		} catch (IOException e) {
			
		}
		synchronized(this) {
			buddyList = newList;
		}
	}

	private void sendUdp(String payload) throws IOException
	{
		DatagramSocket s = new DatagramSocket();
		try {
			InetAddress serverIP = InetAddress.getByName(serverAddress);
			sendUdp(s, serverIP, 1235, payload);
		} finally {
			try { s.close(); } catch (Exception e) {}
		}
	}

	private void sendUdp(DatagramSocket sock, InetAddress ip, int port, String payload) throws IOException
	{
		byte[] buf = payload.getBytes();
		DatagramPacket p = new DatagramPacket(buf, buf.length, ip, port);
		sock.send(p);
	}

	private void enterChatMode(Socket s, BufferedReader in, DataOutputStream out)
	{
		activeChatSocket = s;
		activeChatIn = in;
		activeChatOut = out;
		chatMode = true;

		
		chatReceiverThread = new Thread(new Runnable() {
			public void run() {
				try {
					String line;
					while ((line = activeChatIn.readLine()) != null) {
						System.out.println("B: "+line);
					}
				} catch (IOException e) {
					
				} finally {
					System.out.println("Buddy connection closed.");
					chatMode = false;
					try { if (activeChatSocket != null) activeChatSocket.close(); } catch (Exception e) {}
					activeChatSocket = null;
				}
			}
		});
		chatReceiverThread.setDaemon(true);
		chatReceiverThread.start();

		System.out.println("Enter your text to send to buddy.  Enter q to quit.");
		try {
			String line;
			while (chatMode) {
				System.out.print("> ");
				line = getLine();
				if (line == null) break;
				if (line.equals("q")) {
					try { activeChatSocket.close(); } catch (Exception e) {}
					break;
				}
				try {
					activeChatOut.writeBytes(line+"\n");
				} catch (Exception e) {
					break;
				}
			}
		} finally {
			chatMode = false;
			try { if (activeChatSocket != null) activeChatSocket.close(); } catch (Exception e) {}
			activeChatSocket = null;
		}
	}

	
	void setPendingIncoming(Socket s)
	{
		if (activeChatSocket != null || chatMode) {
			
			try {
				DataOutputStream out = new DataOutputStream(s.getOutputStream());
				out.writeBytes("NO\n");
				s.close();
			} catch (Exception e) { }
			return;
		}
		pendingIncomingSocket = s;
	}

}


class BuddyStatusRecord
{	public String IPaddress;
	public String status;
	public String buddyId;
	public String buddyPort;

	public String toString()
	{	return buddyId+"\t"+status+"\t"+IPaddress+"\t"+buddyPort; }

	public boolean isOnline()
	{	return status.indexOf("100") >= 0; }
}




class TCPMessenger implements Runnable
{
	private IMClient client;
	private ServerSocket welcomeSocket;

	public TCPMessenger(IMClient c, ServerSocket s)
	{
		client = c;
		welcomeSocket = s;
	}

    public void run()
	{
		
		try
		{
			while (true)
			{
		    	
		    	Socket connection = welcomeSocket.accept();
		    	
		    	client.setPendingIncoming(connection);
		    	System.out.print("\nDo you want to accept an incoming connection (y/n)? ");
		    	
			}
	    }
		catch (SocketException se)
		{
			
		}
		catch (Exception e)
		{	System.out.println(e); }
	}
}
