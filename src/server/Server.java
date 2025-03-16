package server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import lib.Auction;

public class Server extends UnicastRemoteObject implements Auction {

  private final Map<String, Map<String, Object>> auctions = new ConcurrentHashMap<>();
  private final Map<String, List<Map<String, Object>>> bids = new ConcurrentHashMap<>();

  public Server() throws RemoteException {
  }

  public String viewAllAuctions() throws RemoteException {
    if (auctions.isEmpty()) {
      return "---------------------\n" + "No active auctions yet!\n" + "---------------------\n";
    }

    StringBuilder sb = new StringBuilder();
    for (String id : auctions.keySet()) {
      Map<String, Object> tmp = auctions.get(id);
      if (tmp.get("status") == Status.ACTIVE) {
        sb.append("---------------------\n");
        sb.append("ID: " + tmp.get("id") + "\n");
        sb.append("Name: " + tmp.get("name") + "\n");
        sb.append("Owner: " + tmp.get("owner") + "\n");
        sb.append("Type: " + tmp.get("type") + "\n");
        sb.append("Price: " + tmp.get("price") + "\n");
        sb.append("Status: " + tmp.get("status") + "\n");
        sb.append("---------------------\n");
      }
    }

    return sb.toString();
  }

  public String createAuction(String name, String ownerId, Type type, double initialPrice) throws RemoteException {
    String auctionId = UUID.randomUUID().toString();

    Map<String, Object> auction = new HashMap<>();

    auction.put("id", auctionId);
    auction.put("name", name);
    auction.put("owner", ownerId);
    auction.put("type", type);
    auction.put("price", initialPrice);
    auction.put("status", Status.ACTIVE);

    auctions.put(auctionId, auction);
    bids.put(auctionId, new ArrayList<>());

    return auctionId;
  }

  public Map<String, Object> getAuction(String auctionId) throws RemoteException {
    if (!auctions.containsKey(auctionId)) {
      throw new RemoteException("Auction not found: " + auctionId);
    }

    return new HashMap<>(auctions.get(auctionId));
  }

  public boolean updateAuctionStatus(String auctionId, Status newStatus, String userId) throws RemoteException {
    if (!auctions.containsKey(auctionId)) {
      throw new RemoteException("Auction not found: " + auctionId);
    }

    Map<String, Object> auction = auctions.get(auctionId);
    String ownerId = auction.get("owner").toString();

    if (!ownerId.equals(userId)) {
      return false;
    }

    auction.put("status", newStatus);
    return true;
  }

  public String placeBid(String auctionId, String userId, double price, boolean isBuyer) throws RemoteException {
    if (!auctions.containsKey(auctionId)) {
      throw new RemoteException("Auction not found: " + auctionId);
    }

    Map<String, Object> auction = auctions.get(auctionId);
    Status status = (Status) auction.get("status");

    if (status == Status.CLOSED) {
      throw new RemoteException("Auction closed, bid rejected");
    }

    Type auctionType = (Type) auction.get("type");

    if (auctionType == Type.REVERSE && isBuyer) {
      throw new RemoteException("Buyers cannot place bid in reverse auction...");
    }

    String bidId = UUID.randomUUID().toString();

    Map<String, Object> bid = new HashMap<>();
    bid.put("id", bidId);
    bid.put("auctionId", auctionId);
    bid.put("userId", userId);
    bid.put("price", price);
    bid.put("isBuyer", isBuyer);
    bid.put("matched", false);

    List<Map<String, Object>> auctionBids = bids.get(auctionId);
    auctionBids.add(bid);

    if (auctionType == Type.DOUBLE) {
      matchOrders(auctionId);
    }

    return bidId;
  }

  public List<Map<String, Object>> getBids(String auctionId) throws RemoteException {
    if (!auctions.containsKey(auctionId)) {
      throw new RemoteException("Auction not found..." + auctionId);
    }

    List<Map<String, Object>> auctionBids = bids.get(auctionId);
    List<Map<String, Object>> result = new ArrayList<>();

    for (Map<String, Object> bid : auctionBids) {
      Map<String, Object> bidCopy = new HashMap<>(bid);
      result.add(bidCopy);
    }

    return result;
  }

  public List<Map<String, Object>> matchOrders(String auctionId) throws RemoteException {
    if (!auctions.containsKey(auctionId)) {
      throw new RemoteException("Auction not found..." + auctionId);
    }

    Map<String, Object> auction = auctions.get(auctionId);
    Type auctionType = (Type) auction.get("type");

    if (auctionType != Type.DOUBLE) {
      throw new RemoteException("Order matching is only for double auctions!");
    }

    List<Map<String, Object>> auctionBids = bids.get(auctionId);
    List<Map<String, Object>> matches = new ArrayList<>();

    List<Map<String, Object>> buyOrders = new ArrayList<>();
    for (Map<String, Object> bid : auctionBids) {
      boolean isBuyer = (boolean) bid.get("isBuyer");
      boolean isMatched = (boolean) bid.get("matched");

      if (isBuyer && !isMatched) {
        buyOrders.add(bid);
      }
    }

    Collections.sort(buyOrders, (b1, b2) -> {
      double price1 = (double) b1.get("price");
      double price2 = (double) b2.get("price");

      return Double.compare(price2, price1);
    });

    List<Map<String, Object>> sellOrders = new ArrayList<>();
    for (Map<String, Object> bid : auctionBids) {
      boolean isBuyer = (boolean) bid.get("isBuyer");
      boolean isMatched = (boolean) bid.get("matched");

      if (!isBuyer && !isMatched) {
        sellOrders.add(bid);
      }
    }

    Collections.sort(sellOrders, (s1, s2) -> {
      double price1 = (double) s1.get("price");
      double price2 = (double) s2.get("price");

      return Double.compare(price1, price2);
    });

    for (Map<String, Object> buyOrder : buyOrders) {
      for (Map<String, Object> sellOrder : sellOrders) {
        if ((boolean) sellOrder.get("matched")) {
          continue;
        }

        double buyPrice = (double) buyOrder.get("price");
        double sellPrice = (double) sellOrder.get("price");

        if (buyPrice >= sellPrice) {
          Map<String, Object> match = new HashMap<>();
          match.put("buyOrderId", buyOrder.get("id"));
          match.put("sellOrderId", sellOrder.get("id"));
          match.put("buyerId", buyOrder.get("userId"));
          match.put("sellerId", sellOrder.get("userId"));

          double matchPrice = (buyPrice + sellPrice) / 2;
          match.put("matchPrice", matchPrice);

          buyOrder.put("matched", true);
          sellOrder.put("matched", true);

          matches.add(match);
          break;
        }
      }
    }

    return matches;
  }

  public Map<String, Object> getWinningBids(String auctionId) throws RemoteException {
    if (!auctions.containsKey(auctionId)) {
      throw new RemoteException("Auction not found..." + auctionId);
    }

    Map<String, Object> auction = auctions.get(auctionId);
    Type auctionType = (Type) auction.get("type");
    List<Map<String, Object>> auctionBids = bids.get(auctionId);
    Map<String, Object> results = new HashMap<>();

    switch (auctionType) {
      case DOUBLE:
        List<Map<String, Object>> matches = new ArrayList<>();
        Map<String, Object> match = new HashMap<>();
        for (Map<String, Object> bid : auctionBids) {
          boolean isMatched = (boolean) bid.get("macthed");
          if (isMatched) {
            matches.add(bid);
          }
        }

        results.put("matches", matches);
        break;

      case REVERSE:
        Map<String, Object> lowestSellBid = null;
        double lowestPrice = Double.MAX_VALUE;

        for (Map<String, Object> bid : auctionBids) {
          boolean isBuyer = (boolean) bid.get("isBuyer");
          if (!isBuyer) {
            double price = (double) bid.get("price");
            if (lowestSellBid == null || price < lowestPrice) {
              lowestSellBid = bid;
              lowestPrice = price;
            }
          }
        }

        if (lowestSellBid != null) {
          results.put("winningBid", lowestSellBid);
        }
        break;

      default:
        throw new RemoteException("This type of auction isn't supported: " + auctionType);
    }

    return results;
  }

  public Map<String, Object> closeAuction(String auctionId, String userId) throws RemoteException {
    if (!auctions.containsKey(auctionId)) {
      throw new RemoteException("Auction not found: " + auctionId);
    }

    boolean updated = updateAuctionStatus(auctionId, Status.CLOSED, userId);

    if (!updated) {
      Map<String, Object> auction = auctions.get(auctionId);
      String ownerId = auction.get("owner").toString();
      System.out.println("Attempted to close by: " + userId);
      System.out.println("Auction belongs to: " + ownerId);
      throw new RemoteException("Only the auction owner can close the auction");
    }

    Map<String, Object> result = new HashMap<>();
    result.put("auctionId", auctionId);
    result.put("closedBy", userId);
    result.put("winningBid", getWinningBids(auctionId));

    return result;
  }

  public static void main(String[] args) {
    try {
      Server auction = new Server();

      int rmiPort = 1099;
      String name = "myAuction";

      Registry registry = null;
      try {
        registry = LocateRegistry.createRegistry(rmiPort);
        System.out.println("RMI registry created at: " + rmiPort);
      } catch (RemoteException e) {
        System.out.println("RMI registry exists, attempting to get refference...");
        registry = LocateRegistry.getRegistry(rmiPort);
      }

      registry.rebind(name, auction);
      System.out.println("Auction server ready!");
    } catch (Exception e) {
      System.err.println("Auction server issue: " + e.toString());
      e.printStackTrace();
    }
  }

}
