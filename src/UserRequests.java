import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

public class UserRequests extends Plugin {
  private UserRequests.Listener l = new UserRequests.Listener(this);
  protected static final Logger log = Logger.getLogger("Minecraft");
  public static String name = "UserRequests";
  public static String version = "0.2";
  public static String propFile = "UserRequests.properties";
  public static PropertiesFile props;
  private static UserRequestsDataSource ds;
  public static boolean debug = true;
  public static String connectorJar = "mysql-connector-java-bin.jar";
  
  //MySQL Settings
  public static String driver = "com.mysql.jdbc.Driver";
  public static String user = "root";
  public static String pass = "root";
  public static String db = "jdbc:mysql://localhost:3306/minecraft";
  public static String table = "userrequests";
  
  //Messages
  public static String messagesubmitted = "Your request was submitted to the server!";
  public static String messageemailinvalid = "Your email is invalid, please try again!";
  public static String messageawarded = "You have been awarded build access, play nice!";

  //Other
  public static String buildgroup = "builder";
  
  public void enable()
  {
    if (!initProps()) {
      log.severe(name + ": Could not initialise " + propFile);
      disable();
      return;
    }

    //Currently Just MySQL
    ds = new UserRequestsMySQL();
      
    if (!ds.init()) {
      log.severe(name + ": Could not init the datasource");
      disable();
      return;
    }

    //Add commands...
    etc.getInstance().addCommand("/request", "<Email> - Request a build privilages");
    etc.getInstance().addCommand("/requestaccept", "<Username> - Accept a request to build privilages");
    log.info(name + " " + version + " enabled");
  }

  public void disable() {
	etc.getInstance().removeCommand("/request");
	etc.getInstance().removeCommand("/requestaccept");
    log.info(name + " " + version + " disabled");
  }

  public void initialize() {
	etc.getLoader().addListener(PluginLoader.Hook.COMMAND, this.l, this, PluginListener.Priority.MEDIUM);
  }

  public boolean initProps() {
    props = new PropertiesFile(propFile);

    //MySQL
    driver = props.getString("driver", "com.mysql.jdbc.Driver");
    user = props.getString("user", "root");
    pass = props.getString("pass", "root");
    db = props.getString("db", "jdbc:mysql://localhost:3306/minecraft");
    table = props.getString("table", "userrequests");
    connectorJar = props.getString("mysql-connector-jar", connectorJar);
    
    //Messages
    messagesubmitted = props.getString("message-submitted", messagesubmitted);
    messageemailinvalid = props.getString("message-invalidemail", messageemailinvalid);
    messageawarded = props.getString("message-groupawarded", messageawarded);
    
    
    //Group to Add
    buildgroup = props.getString("awarded-group", buildgroup);
    
    File file = new File(propFile);
    return file.exists();
  }
  
  private void debugmsg(String message, Player player) {
	  if(debug) player.sendMessage("[DEBUG]: " + message);
  }

  public class Listener extends PluginListener {
	  UserRequests p;

	  public Listener(UserRequests plugin) {
	    this.p = plugin;
	  }
	    
	  public boolean onCommand(Player player, String[] split) {
	      if (!player.canUseCommand(split[0])) {
	          return false;
	      }
	
	      if (split[0].equalsIgnoreCase("/request")) {  
	    	  debugmsg("Request made, variables "+split.length, player);
	    	  int variables = split.length;
	          if (variables==2) {
	        	  EmailValidator emailValidator = new EmailValidator();
	        	  boolean valid = emailValidator.validate(split[1]);
	        	  if(valid) {
	        		  UserRequests.ds.newRequest(player.getName(), split[1]);
	        		  player.sendMessage(Colors.Red + messagesubmitted);
	        	  } else {
	        		  player.sendMessage(Colors.Red + messageemailinvalid);
	        	  }
	        	  return true;
	          } else {
	        	  player.sendMessage(Colors.Red + split[0] + " <Email>");
	        	  return true;
	          }
	      } else if(split[0].equalsIgnoreCase("/requestaccept")||split[0].equalsIgnoreCase("/ra")) {
	    	  int variables = split.length;
	          if (variables==2) {
	        	  debugmsg("Request accept: " + UserRequests.ds.acceptRequest(split[1]), player);
	        	  if(UserRequests.ds.acceptRequest(split[1])) {
	        		  //Tell Mod
	        		  player.sendMessage(Colors.Red + "Player build request accepted and user updated");
	        		  //player.sendMessage(Colors.Red + "There are X more requests to consider");
	        		  
		        	  //Lets do it
		        	  Player user = etc.getServer().matchPlayer(split[1]);
		        	  //Add Group
		        	  player.addGroup(buildgroup);
		        	  //Message them
		        	  player.sendMessage(messageawarded);
	        	  } else {
	        		  player.sendMessage(Colors.Red + "Player request failed, have they made a request?");
	        		  debugmsg("It could be a problem with the MySQL query, check server", player);
	        	  }
	        	  
	        	  return true;
	          } else {
	        	  player.sendMessage(Colors.Red + split[0] + " <Username>");
	        	  return true;
	          }
	      } else {
	    	  return false;
	      }
	  }
  }
}