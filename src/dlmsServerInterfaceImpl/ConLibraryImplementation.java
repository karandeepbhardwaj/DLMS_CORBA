package dlmsServerInterfaceImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.omg.CORBA.ORB;

import Library.LibraryInterfacePOA;
import SendRecieveImpl.SendReceiveImplementation;

public class ConLibraryImplementation extends LibraryInterfacePOA{

	private ORB orb;

	static Map<String, HashMap<String, Integer>> outerMap = new HashMap<>();
	static HashMap<String, List<String>> bookMap = new HashMap<>();
	static HashMap<String, List<String>> bookQueue = new HashMap<>();
	HashMap<String, Integer> innerMap = new HashMap<>();
	public static Logger Log = Logger.getLogger(ConLibraryImplementation.class.getName());

	public void setORB(ORB orb_val) {
		orb = orb_val;
	}

	@Override
	public String addItem(String managerID, String itemID, String itemName, int quantity) {
		initLogger(Log, managerID);
		int qty = 0;
		String responseString = "Invalid input";
		if(outerMap == null || !outerMap.containsKey(itemID)) {
			innerMap.put(itemName, quantity);
			outerMap.put(itemID, innerMap);
			innerMap = new HashMap<String, Integer>();
			responseString = "Item added succesfully";
		} else if(outerMap.containsKey(itemID)){
			innerMap = outerMap.get(itemID);
			qty = innerMap.get(itemName);
			qty = qty + quantity;
			innerMap.put(itemName,qty);
			outerMap.put(itemID,innerMap);
			Log.info("Added item for manager "+managerID+" "+itemID+" "+itemName+" "+ quantity);
			responseString = "Item Updated succesfully";
		}
		while(bookQueue.containsKey(itemID) && qty != 0) {
			List<String> userID = bookQueue.get(itemID);
			String userId=userID.get(0);
			borrowItem(userId, itemID);
			userID.remove(userId);
			qty--;
			if (userID.size() == 0) {
				bookQueue.remove(itemID);
			}
		}
		return responseString;
	}

	@Override
	public String removeItem(String managerID, String itemID, int quantity, int option) {
		initLogger(Log, managerID);
		HashMap<String, Integer> innerMap = new HashMap<>();
		int qty = 0;
		String itemN = null;
		if(option == 1) {
			outerMap.remove(itemID);
			Log.info("Book removed for manager "+managerID+" "+itemID+" "+quantity);
			return "Book removed succesfully";
		} else if(option == 2) {
			for(String str : outerMap.get(itemID).keySet()) {
				qty = outerMap.get(itemID).get(str);
				itemN = str;
			}
			qty-=quantity;
			innerMap.put(itemN, qty);
			outerMap.put(itemID, innerMap);
			Log.info("Book number decreased for manager "+managerID+" "+itemID+" "+quantity);
			return "Book number decreased successfully";
		}
		return " Invalid input";
	}

	@Override
	public String listItemAvailability(String managerID) {
		initLogger(Log, managerID);
		String list ="";

		for (String itemID : outerMap.keySet()) {
			String str = itemID + " : ";
			Log.info("List items for "+managerID+" "+outerMap);
			for (String itemName : outerMap.get(itemID).keySet()) {
				System.out.print(itemName + " " + outerMap.get(itemID).get(itemName));
				str +=  itemName + " / " + outerMap.get(itemID).get(itemName)+"\n";
			}
			list+=str; 
		}
		return list;
	}

	@Override
	public String findItem(String userID, String itemName, boolean fromOtherServer) {
		String responseString = "";
		String itemID = null;
		initLogger(Log, userID);
		Iterator<Entry<String, HashMap<String, Integer>>> mapData = outerMap.entrySet().iterator();
		while (mapData.hasNext()) {
			System.out.println(mapData);
			Entry<String, HashMap<String, Integer>> data = mapData.next();
			itemID = data.getKey();
			innerMap = data.getValue();
			if(innerMap.containsKey(itemName)) {
				responseString += itemID+" / "+innerMap.get(itemName);
			}
		}
		if(!fromOtherServer) {
			SendReceiveImplementation serverObj = new SendReceiveImplementation();
			responseString += serverObj.sendMessage(userID,"MCG", 2,itemName,"");
			responseString += serverObj.sendMessage(userID,"MON", 2,itemName,"");
		}
		Log.info("Find item for user "+ userID+ ", book name= "+itemName);
		return responseString;
	}

	@Override
	public String borrowItem(String userID, String itemID) {
		String responseString =null;
		int qty = 0;
		String name = null;
		initLogger(Log, userID);
		if(!userID.substring(0, 3).equals("CON")) {
			for(String str : bookMap.keySet()) {
				if (bookMap.get(str).contains(userID)) {
					return "Cannot take more than one book";
				}
			}
		}
		if(itemID.substring(0,3).contains("CON")) {
			if(bookMap.containsKey(itemID) && bookMap.get(itemID).contains(userID)) {
				return "You have already taken this book, cant be issued twice!";
			}
			if(outerMap.containsKey(itemID)) {
				innerMap = outerMap.get(itemID);
				for(String str : innerMap.keySet()) {
					qty = innerMap.get(str);
					name = str;
				}
				if(qty != 0) {
					qty--;
					innerMap.put(name,qty);
					outerMap.put(itemID, innerMap);
					if(bookMap.containsKey(itemID)) {
						bookMap.get(itemID).add(userID);
					}else {
						List<String> borredList = new ArrayList<String>();
						borredList.add(userID);
						bookMap.put(itemID, borredList);
					}
					Log.info("Borrowed succesfully for user "+ userID+ " "+itemID);
					responseString = "Book issued successfully";
				}  else {
					return "waitlist";
				}
			}else {
				responseString = "Book doesnot exist";
			}
		}else{
			SendReceiveImplementation serverObj = new SendReceiveImplementation();
			responseString = serverObj.sendMessage(userID, itemID, 1, "","");
		}
		return responseString.trim();
	}

	@Override
	public String returnItem(String userID, String itemID) {
		String responseString;
		initLogger(Log, userID);
		if(itemID.substring(0,3).contains("CON")) {
			if(bookMap.containsKey(itemID) && !bookMap.get(itemID).contains(userID)) {
				String s = "Book from the borrower needs to be returned";
				return s;
			}
			int qty = 0;
			String itemName = "";
			innerMap = outerMap.get(itemID);
			for(String str : innerMap.keySet()) {
				qty = innerMap.get(str);
				itemName = str;
			}
			innerMap.put(itemName, qty + 1);
			outerMap.put(itemID, innerMap);
			bookMap.get(itemID).remove(userID);
			Log.info("Book returned succesfully for user "+ userID + " "+ itemID + " "+ itemName);
			responseString = "Book returned succesfully.";
		}else {
			SendReceiveImplementation serverObj = new SendReceiveImplementation();
			responseString = serverObj.sendMessage(userID, itemID, 3, "","");
		}
		if(bookQueue.containsKey(itemID)) {
			String waitingListUser = bookQueue.get(itemID).get(0);
			borrowItem(waitingListUser, itemID);
			bookQueue.get(itemID).remove(waitingListUser);
			if(bookQueue.get(itemID).size() == 0) {
				bookQueue.remove(itemID);
			}
		}
		return responseString;
	}

	@Override
	public String waitingList(String userID, String itemID) {
		List<String> waitingList;
		String responseString;
		if(itemID.substring(0,3).contains("CON")) {
			if(bookQueue.containsKey(itemID)) {
				waitingList = bookQueue.get(itemID);
				if(waitingList.contains(userID)) {
					responseString = "User already in the Waiting list";
				}else {
					waitingList.add(userID);
					responseString = "Added Successfully";
				}
			}else {
				waitingList = new ArrayList();
				waitingList.add(userID);
				bookQueue.put(itemID,waitingList);
				responseString = "Added Successfully";
			}
		}else {
			SendReceiveImplementation serverObj = new SendReceiveImplementation();
			responseString = serverObj.sendMessage(userID, itemID, 4, "","");
		}
		return responseString;
	}

	public String bookPresent(String userID, String newItemID, String oldItemID) {
		
		if(!userID.substring(0, 3).equals("CON")) {
			List<String> booksBorrowedByUserList = new ArrayList<String>();
			for(String str : bookMap.keySet()) {
				if (bookMap.get(str).contains(userID)) {
					booksBorrowedByUserList.add(str);
				}
			}
			if(booksBorrowedByUserList.isEmpty() ||
					(!booksBorrowedByUserList.isEmpty() && booksBorrowedByUserList.contains(oldItemID))) {
				return "true";
			}
			return "Cannot borrow more than 1 books";
			
		}
		
		if(newItemID.substring(0, 3).equals("CON")) {
			Boolean checkBoolean = outerMap.containsKey(newItemID);
			return checkBoolean == true ? "true" : "false";
		}else {
			SendReceiveImplementation serverObj = new SendReceiveImplementation();
			String responseString = serverObj.sendMessage(userID, oldItemID, 6, "",newItemID);
			return responseString.equals("true") ? "true" : "false";
		}
		
	}
	
	public String bookBorrowed(String userID, String oldItemID) {
		if(oldItemID.substring(0, 3).equals("CON")) {
			if(bookMap.containsKey(oldItemID) && bookMap.get(oldItemID).contains(userID) 
					&& oldItemID.substring(0,3).contains("CON")) {
						return "true";
					}else {
						return "false";
					}
		}else {
			SendReceiveImplementation serverObj = new SendReceiveImplementation();
			String responseString = serverObj.sendMessage(userID, oldItemID, 7, "","");
			return responseString.equals("true") ? "true" : "false";
		}
		
	}
	
	@Override
	public String exchangeItem(String userID, String oldItemID, String newItemID, boolean fromOtherServer) {
		String responseString ="";
		String Result = bookPresent(userID, newItemID, oldItemID);
		String Result1 = bookBorrowed(userID, oldItemID);
		initLogger(Log, userID);
		
		if(Result1.equals("true") && Result.equals("true")) {
			
				returnItem(userID, oldItemID);
				borrowItem(userID, newItemID);
				Log.info("Book exchanged for user " +userID+" "+ oldItemID + " exchanged with "+newItemID);
				responseString = "Book exchanged";
		}else {
				responseString = "Book can not be exchanged!";
			}
		return responseString;
	}
	
	private void initLogger(Logger log, String userID) {
		FileHandler fileHandler;
		try {
			fileHandler = new FileHandler(System.getProperty("user.dir") + "/LogFiles/" + userID + ".log", true);
			fileHandler.setFormatter(new SimpleFormatter());
			log.addHandler(fileHandler);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}