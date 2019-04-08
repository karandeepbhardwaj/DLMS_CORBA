package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;

import ConstantValue.Constants;
import Library.LibraryInterface;
import Library.LibraryInterfaceHelper;

public class Client {

    public static void main(String[] args) {
        try {
            ORB orb = ORB.init(args, null);
            // -ORBInitialPort 1050 -ORBInitialHost local host
            Object objRef = orb.resolve_initial_references("NameService");
            NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

            LibraryInterface rconcordia = LibraryInterfaceHelper.narrow(ncRef.resolve_str(Constants.CONCORDIA_SERVER_NAME));
            LibraryInterface rmcgill = LibraryInterfaceHelper.narrow(ncRef.resolve_str(Constants.MCGILL_SERVER_NAME));
            LibraryInterface rmontreal = LibraryInterfaceHelper.narrow(ncRef.resolve_str(Constants.MONTREAL_SERVER_NAME));

            Client client = new Client();
            BufferedReader inp = new BufferedReader(new InputStreamReader(System.in));

            System.out.print("Please enter your ID: ");
            String inputId = inp.readLine();
            if (inputId.substring(3, 4).contains("U")) {
                String check = inputId.substring(0, 3);
                switch (check) {
                    case "CON":
                        client.allUsers(inputId, rconcordia);
                        break;
                    case "MCG":
                        client.allUsers(inputId, rmcgill);
                        break;
                    case "MON":
                        client.allUsers(inputId, rmontreal);
                        break;
                    default:
                        System.out.println("Invalid Input.");
                }
            } else if (inputId.substring(3, 4).contains("M")) {
                String check = inputId.substring(0, 3);
                switch (check) {
                    case "CON":
                        client.allManagers(inputId, rconcordia);
                        break;
                    case "MCG":
                        client.allManagers(inputId, rmcgill);
                        break;
                    case "MON":
                        client.allManagers(inputId, rmontreal);
                        break;
                    default:
                        System.out.println("Invalid Input.");
                }
            } else {
                System.out.println("Re-enter the Id. Entered id is invalid.");
            }

        } catch (Exception e) {
            System.out.println("Hello Client exception: " + e);
            e.printStackTrace();
        }
    }

    private Object responseString;

    private void allManagers(String inputId, LibraryInterface libraryInterface) throws NumberFormatException, IOException {
        int quantity = 0;
        int option = 0;
        String itemName = null;
        String itemID = null;
        System.out.println("Welcome to Library: " + inputId + " Please make a choice from below.");
        System.out.println("1. Add a book");
        System.out.println("2. Remove a book.");
        System.out.println("3. List available books in the Library.");
        System.out.println("4. Exit");

        BufferedReader inp = new BufferedReader(new InputStreamReader(System.in));
        int choice = Integer.parseInt(inp.readLine());

        switch (choice) {

            case 1:
                System.out.println("Please enter the name of the book");
                itemName = inp.readLine();

                do {
                    System.out.println("Please enter ID: ");
                    itemID = inp.readLine();
                } while (!inputId.substring(0, 3).equals(itemID.substring(0, 3)));

                System.out.println("Please enter quantity: ");
                quantity = Integer.parseInt(inp.readLine());

                String result = libraryInterface.addItem(inputId, itemID, itemName, quantity);
                System.out.println(result);
                break;

            case 2:
                System.out.println("Please specify action to perform : ");
                System.out.println("1. Remove item completely? ");
                System.out.println("2. Decrease the quantity of the item");
                option = Integer.parseInt(inp.readLine());


                System.out.println("Please enter book ID: ");
                itemID = inp.readLine();
                if (option != 1) {
                    System.out.println("Please enter quantity: ");
                    quantity = Integer.parseInt(inp.readLine());
                }

                String str = libraryInterface.removeItem(inputId, itemID, quantity, option);
                System.out.println(str);
                break;

                		case 3:
                			String listResult = libraryInterface.listItemAvailability(inputId);
                			System.out.println(listResult);
//                			String[] nameString;
//                			for(int i =0; i<=listResult.length()-1;i++) {
//                				if(listResult.matches("[0-9]+")) {
//                					nameString = listResult.split("[0-9]+");
//                					System.out.println(nameString);
//                				}
//                				System.out.println(listResult);
//                			}
//                			for(String string : listResult) {
//                				System.out.println(string);
//                			}
                			break;

            case 4:
                System.out.println("Exit");
                System.exit(0);
                break;

            default:
                System.out.println("Invalid number,Please try again.");
        }
    }

    private void allUsers(String inputId, LibraryInterface libraryInterface) {
        String itemName = null;
        String itemID = null;
        String oldItemID =null;
        String newItemID = null;
        System.out.println("Welcome to Library: " + inputId + "\n" + "Please make a choice from below :");
        System.out.println("1. Find a book.");
        System.out.println("2. Borrow a book.");
        System.out.println("3. Return a book.");
        System.out.println("4. Exchange a book.");
        System.out.println("5. Exit. \n");

        try {
            BufferedReader inp = new BufferedReader(new InputStreamReader(System.in));
            int choice = Integer.parseInt(inp.readLine());
            switch (choice) {

                case 1:
                    System.out.println("Enter the book name: ");
                    itemName = inp.readLine();
                    String str = libraryInterface.findItem(inputId, itemName, false);
                    System.out.println(str);
                    break;

                case 2:
                    System.out.println("Please enter book ID: ");
                    itemID = inp.readLine();
                    String responseString = libraryInterface.borrowItem(inputId, itemID);
                    if (responseString.trim().equals("waitlist")) {
                        System.out.println("Book not available in the library, Do you want to get in the waiting list ?");
                        System.out.println("1. Yes");
                        System.out.println("2. No");
                        int ch = Integer.parseInt(inp.readLine());

                        switch (ch) {
                            case 1:
                                libraryInterface.waitingList(inputId, itemID);
                                System.out.println("Successfully added to the list");
                                break;
                            case 2:
                                System.out.println("Have a nice day");
                        }
                    } else {
                        System.out.println(responseString);
                    }
                    break;
                case 3:
                    System.out.print("Please enter book ID: ");
                    itemID = inp.readLine();
                    responseString = libraryInterface.returnItem(inputId, itemID);
                    System.out.println(responseString);
                    break;
                case 4:
                	System.out.println("Please enter the item to exchange.");
                	oldItemID = inp.readLine();
                	System.out.println("Please enter the book id to exchange with.");
                	newItemID =inp.readLine();
                	responseString = libraryInterface.exchangeItem(inputId, oldItemID, newItemID, false);
                	System.out.println(responseString);
                case 5:
                    System.out.println("Exit");
                    System.exit(0);
                    break;

                default:
                    System.out.println("Please enter valid input" + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}