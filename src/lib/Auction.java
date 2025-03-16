package lib;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.List;

public interface Auction extends Remote {

  enum Type {
    DOUBLE,
    REVERSE
  }

  enum Status {
    ACTIVE,
    CLOSED
  }

  String viewAllAuctions() throws RemoteException;

  String createAuction(String title, String ownerId,
      Type type, double initialPrice) throws RemoteException;

  Map<String, Object> getAuction(String auctionId) throws RemoteException;

  boolean updateAuctionStatus(String auctionId, Status newStatus, String userId)
      throws RemoteException;

  String placeBid(String auctionId, String userId, double price,
      boolean isBuyer) throws RemoteException;

  List<Map<String, Object>> getBids(String auctionId) throws RemoteException;

  List<Map<String, Object>> matchOrders(String auctionId) throws RemoteException;

  Map<String, Object> getWinningBids(String auctionId) throws RemoteException;

  Map<String, Object> closeAuction(String auctionId, String userId)
      throws RemoteException;
}
