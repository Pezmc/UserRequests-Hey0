//This is implemented in case someone needs flatfile...
public abstract class UserRequestsDataSource
{
  public abstract boolean init();
  
  public abstract boolean newRequest(String player, String email);
  
  public abstract boolean acceptRequest(String player, String acceptor);
  
  public abstract boolean requestExists(String player);
  
  public abstract int requestStatus(String player);
 
  public abstract String requestStatusText(String player); 
  
  public abstract String[][] requestsWithStatus(int status); 
  
  public abstract String[][] requestInfo(String player);
  
  public abstract boolean truncateData();
  
  
 
  /** Convert a status to English Alias */
  public String requestStatusToText(String status) {
	  return requestStatusToText(Integer.parseInt(status));
  }
  
  /** Convert a status to English */
  public String requestStatusToText(int status) {
	  //ATM 1 & 2 are unused... (looked at/denied?)
	  switch (status) {
	  	case 0: return "Submitted"; 
	  	case 3: return "Accepted"; 
	  	default: return "Other";
	  }
  }
}