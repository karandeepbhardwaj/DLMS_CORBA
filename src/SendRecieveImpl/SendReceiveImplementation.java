package SendRecieveImpl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import ConstantValue.Constants;

public class SendReceiveImplementation {

	public String sendMessage(String userID, String itemID, int method, String itemName, String newItemID) {
		
		DatagramSocket aSocket = null;
		String responseString = null;
		int serverPort;
		try {
			aSocket = new DatagramSocket();
			String string = userID + "-" +itemID + "-" +  method+"-"+ itemName+ "-"+ newItemID;
			byte[] message = string.getBytes();
			if(!itemID.isEmpty()) {
				serverPort = serverCheck(itemID);
			}else {
				serverPort = serverCheck(newItemID);
			}
			 
			InetAddress aHost = InetAddress.getByName("localhost");
			DatagramPacket request = new DatagramPacket(message, string.length(), aHost, serverPort);
			aSocket.send(request);
			System.out.println("Request message sent from the client to server with port number " + serverPort + " is: "
					+ new String(request.getData()));
			byte[] buffer = new byte[1000];
			DatagramPacket reply = new DatagramPacket(buffer, buffer.length);

			aSocket.receive(reply);
			System.out.println("Reply received from the server with port number " + serverPort + " is: "
					+ new String(reply.getData()));
			responseString = new String(reply.getData());
		} catch (SocketException e) {
			System.out.println("Socket: " + e.getMessage());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("IO: " + e.getMessage());
		} finally {
			if (aSocket != null)
				aSocket.close();
		}return responseString.trim();
	}
	public int serverCheck(String itemID) {
		int port =0;
		if(itemID.substring(0, 3).contains("CON")) {
			port = Constants.CONCORDIA_SERVER_PORT;
		}else if(itemID.substring(0, 3).contains("MCG")) {
			port = Constants.MCGILL_SERVER_PORT;
		}else if(itemID.substring(0, 3).contains("MON")) {
			port = Constants.MONTREAL_SERVER_PORT;
		}
		return port;
	}
}