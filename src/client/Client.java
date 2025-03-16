package client;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import lib.Auction;
import java.util.UUID;

public class Client {
  private Auction auction;
  private String userId;
  private Scanner scanner;

  public Client() {
    scanner = new Scanner(System.in);
    userId = UUID.randomUUID().toString();
    System.out.println("Your user ID for this session: " + userId);
  }

  public void connect(String host, int port, String name) {
    try {
      Registry registry = LocateRegistry.getRegistry(host, port);
      auction = (Auction) registry.lookup(name);
      System.out.println("Connected to auction server");
    } catch (Exception e) {
      System.err.println("Client connection failed: " + e.toString());
      // TODO: remove stack trace after done debugging
      e.printStackTrace();
    }
  }

  public void displayMenu() {
    System.out.println("Select an action");
    System.out.println("1: View all auctions");
    System.out.println("2. Create a new auction");
    System.out.println("3. View auction details");
    System.out.println("4. Place a bid");
    System.out.println("5. View bids for an auction");
    System.out.println("6. Close an auction");
    System.out.println("7. View winning bids");
    System.out.println("7. Exit");
    System.out.print("Enter your choice: ");
  }

  public void start() {
    boolean running = true;
    while (running) {
      displayMenu();
      int choice = scanner.nextInt();
      scanner.nextLine();

      try {
        switch (choice) {
          case 1:
            viewAllAuctions();
            break;
          case 2:
            createAuction();
            break;
          case 3:
            viewAuction();
            break;
          case 4:
            placeBid();
            break;
          case 5:
            viewBids();
            break;
          case 6:
            closeAuction();
            break;
          case 7:
            viewWinningBids();
            break;
          case 8:
            running = false;
            break;
          default:
            System.out.println("Invalid option. Please try again.");
        }
      } catch (Exception e) {
        System.out.println(e.getMessage());
      }
    }
    System.out.println("Bye!");
  }

  private void viewAllAuctions() throws RemoteException {
    System.out.println(auction.viewAllAuctions());
  }

  private void createAuction() throws Exception {
    System.out.println("Create a New Auction");
    System.out.print("Enter auction name: ");
    String name = scanner.nextLine();

    System.out.println("Select auction type:");
    System.out.println("1. REVERSE (lowest bid wins)");
    System.out.println("2. DOUBLE (continuous matching of buy/sell orders)");
    System.out.print("Enter type (1 or 2): ");
    int typeChoice = scanner.nextInt();
    scanner.nextLine();

    Auction.Type type = null; // compiler complains if not initialised
    switch (typeChoice) {
      case 1:
        type = Auction.Type.REVERSE;
        break;
      case 2:
        type = Auction.Type.DOUBLE;
        break;
      default:
        System.out.println("This type of auction is not supported: " + typeChoice);
        break;
    }

    System.out.print("Enter initial price: ");
    double initialPrice = scanner.nextDouble();
    scanner.nextLine();

    String auctionId = auction.createAuction(name, userId, type, initialPrice);
    System.out.println("Auction created successfully! ID: " + auctionId);
  }

  private void viewAuction() throws Exception {
    System.out.println("View auction information");
    System.out.print("Enter auction ID: ");
    String auctionId = scanner.nextLine();

    Map<String, Object> auctionDetails = auction.getAuction(auctionId);

    System.out.println("[INFO] Auction information");
    System.out.println("ID: " + auctionDetails.get("id"));
    System.out.println("Name: " + auctionDetails.get("name"));
    System.out.println("Owner: " + auctionDetails.get("owner"));
    System.out.println("Type: " + auctionDetails.get("type"));
    System.out.println("Initial Price: " + auctionDetails.get("price"));
    System.out.println("Status: " + auctionDetails.get("status"));
  }

  private void placeBid() throws Exception {
    System.out.println("Place a bid");
    System.out.print("Enter auction ID: ");
    String auctionId = scanner.nextLine();

    Map<String, Object> auctionDetails = auction.getAuction(auctionId);
    Auction.Type auctionType = (Auction.Type) auctionDetails.get("type");

    boolean isBuyer = true;
    if (auctionType == Auction.Type.DOUBLE) {
      System.out.print("Are you buying or selling? (B/S): ");
      String bidType = scanner.nextLine().toUpperCase();
      isBuyer = bidType.startsWith("B");
    } else if (auctionType == Auction.Type.REVERSE) {
      isBuyer = false;
    }

    System.out.print("Enter your bid price: ");
    double price = scanner.nextDouble();
    scanner.nextLine();

    String bidId = auction.placeBid(auctionId, userId, price, isBuyer);
    System.out.println("Bid placed successfully! Bid ID: " + bidId);
  }

  private void viewBids() throws Exception {
    System.out.println("View bids for auction");
    System.out.print("Enter auction ID: ");
    String auctionId = scanner.nextLine();

    List<Map<String, Object>> bids = auction.getBids(auctionId);

    System.out.println("Bids for Auction " + auctionId + ":");
    System.out.println("\n");

    for (Map<String, Object> bid : bids) {
      System.out.println("------------------------------");
      System.out.println("ID: " + bid.get("id"));
      System.out.println("User ID: " + bid.get("userId"));
      System.out.println("Price: " + (double) bid.get("price"));
      System.out.println("Buyer? " + ((boolean) bid.get("isBuyer") ? "Yes" : "No"));
      System.out.println("Matched? " + ((boolean) bid.get("matched") ? "Yes" : "No"));
      System.out.println("------------------------------");
    }
  }

  private void closeAuction() throws Exception {
    System.out.println("Close an auction");
    System.out.print("Enter auction ID: ");
    String auctionId = scanner.nextLine();

    Map<String, Object> result = auction.closeAuction(auctionId, userId);

    System.out.println("Auction " + auctionId + " closed successfully!");
    System.out.println("Closed by: " + result.get("closedBy"));
    System.out.println("Results: " + result.get("winningBid"));
  }

  private void viewWinningBids() throws Exception {
    System.out.println("View valid bids");
    System.out.print("Enter auction ID: ");
    String auctionId = scanner.nextLine();

    Map<String, Object> winningBids = auction.getWinningBids(auctionId);

    System.out.println("Winning Bids for Auction " + auctionId + ":");

    Map<String, Object> auctionDetails = auction.getAuction(auctionId);
    Auction.Type auctionType = (Auction.Type) auctionDetails.get("type");

    if (auctionType == Auction.Type.DOUBLE) {
      List<Map<String, Object>> matches = (List<Map<String, Object>>) winningBids.get("matches");

      if (matches != null && !matches.isEmpty()) {
        System.out.println("Matched Orders:");
        for (Map<String, Object> match : matches) {
          System.out.println("------------------------------");
          System.out.println("Buy Order: " + match.get("buyOrderId"));
          System.out.println("------------------------------");
          System.out.println("Sell Order: " + match.get("sellOrderId"));
          System.out.println("------------------------------");
          System.out.println("Buyer: " + match.get("buyerId"));
          System.out.println("------------------------------");
          System.out.println("Seller: " + match.get("sellerId"));
          System.out.println("------------------------------");
          System.out.println("Match Price: $" + match.get("matchPrice"));
          System.out.println("------------------------------");
        }
      } else {
        System.out.println("No matches found for this auction.");
      }
    } else {
      Map<String, Object> winningBid = (Map<String, Object>) winningBids.get("winningBid");

      if (winningBid != null) {
        System.out.println("Winning Bid:");
        System.out.println("Bid ID: " + winningBid.get("id"));
        System.out.println("User ID: " + winningBid.get("userId"));
        System.out.println("Price: $" + winningBid.get("price"));
      } else {
        System.out.println("No winning bid found for this auction.");
      }
    }
  }

  public static void main(String[] args) {
    Client client = new Client();

    String host = "localhost";
    int port = 1099;
    String name = "myAuction";

    client.connect(host, port, name);
    client.start();
  }
}
