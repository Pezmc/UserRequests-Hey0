//This is implemented incase someone wants to add flatfile...
public abstract class UserRequestsDataSource
{
  public abstract boolean init();
  
  public abstract boolean newRequest(String player, String email);
  
  public abstract boolean acceptRequest(String player);

}