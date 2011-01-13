import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Plugin for hey0 that allows users to request build access
 * this is logged in a db until they are accepted, the db
 * is used so you can use a cron job to check on unanswered requests
 * and send out annoying emails to mods
 * 
 * Simply use /request email@address.com and /ra username
 * 
 * @author Pez Cuckow - email@pezcuckow.com
 *
 */

/*
 * To do:
 *  - Prevent user requesting more than once
 *  - Prevent user requesting once accepted
 *  - Ask mod to check email address
 *  - Fix problem with server not saving changes
 *  - Allow flatfile storage?!?
 *  - Get fancy and make this mod send emails instead of needing cron?
 *  - Allow choosing of the mods color
 *  - Useful server output (mod name accepted bla bla's request, bla bla requested)
 *  - Request list to list current requests
 *  - Timer that messages mods and above how many requests there are
 *
 *
 * Done: 
 *  - Messaging mods when request made 0.4
 *  - It's own properties file 0.4
 *  
 * Changes:
 *  0.4:
 *  	Messages mods when a request is made, uses own properties file
 *  0.3:
 *  	Fixed user permissions, more sql changes, accepted added to database, debug true/false added
 *  0.2:
 *  	Added alias', messages stored as strings, improved sql queries, added resultset to array
 *  0.1:
 *  	Initial script, buggy but just working
 */

public class UserRequests extends Plugin {
  private UserRequests.Listener l = new UserRequests.Listener(this);
  protected static final Logger log = Logger.getLogger("Minecraft");
  public static String name = "UserRequests";
  public static String version = "0.4";
  public static String propFile = "UserRequests.properties";
  public static PropertiesFile props;
  private static UserRequestsDataSource ds;
  public static boolean debug = true;
  public static String connectorJar = "mysql-connector-java-bin.jar";
  public static String pluginColor = Colors.Red; //"\u00a74"
  public static DataSource datasource = etc.getDataSource();
   
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
  public static String messagerequestwaiting = "A build request was just submitted by:";
  public static String messagerequestwaiting2 = "check them out then use /requestaccept";
  

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
    messagerequestwaiting = props.getString("message-requestwaiting", messagerequestwaiting);
    messagerequestwaiting2 = props.getString("message-requestwaiting2", messagerequestwaiting2);
        
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
	      //How many arguments?
	      int variables = split.length;
	      
	      if (split[0].equalsIgnoreCase("/request")) {  
	    	  //They allowed?
	    	  if (!player.canUseCommand("/request")) return false;
	    	  
	    	  debugmsg("Request made, variables "+split.length, player);	    	  
	    	  
	          if (variables==2) {
		    	  //Users email (in theory)
		    	  String email = split[1].toLowerCase();
		    	  
	        	  EmailValidator emailValidator = new EmailValidator();
	        	  boolean valid = emailValidator.validate(email);
	        	  if(valid) {
	        		  UserRequests.ds.newRequest(player.getName(), email);
	        		  
	        		  //Add user to mc database
	        		  if(datasource.doesPlayerExist(player.getName())) {
	        			  datasource.addPlayer(player);
	        		  }
	        		  
	        		  //Prevent request resubmit... currently just resets...
        			  
	        		  player.sendMessage(pluginColor + messagesubmitted);
	        		  
	        		  //Lets tell mods/above
	        		  List cPlayers = etc.getServer().getPlayerList();
	        		  Iterator itr = cPlayers.iterator();
	        		  while (itr.hasNext()) {
	        		    Player possibleMod = ((Player)itr.next());
	        		    if(possibleMod.canUseCommand("/requestaccept")) {
	        		    	possibleMod.sendMessage(pluginColor + messagerequestwaiting + " " + player.getName());
	        		    	possibleMod.sendMessage(pluginColor + messagerequestwaiting2 + " " + player.getName());	    	
	        		    }
	        		  }  	
	        		  
	        	  } else {
	        		  player.sendMessage(pluginColor + messageemailinvalid);
	        	  }
	        	  return true;
	          } else {
	        	  player.sendMessage(pluginColor + "Expected: " + split[0] + " <Email>");
	        	  return true;
	          }
	      } else if(split[0].equalsIgnoreCase("/requestaccept")||split[0].equalsIgnoreCase("/ra")) {
	    	  //They allowed?
	    	  if (!player.canUseCommand("/requestaccept")) return false;
	    	  
	          if (variables==2) {
		    	  //Users email (in theory)
		    	  String username = split[1].toLowerCase();
		    	  
		    	  //Is their email valid check here...
		    	  
	        	  if(UserRequests.ds.acceptRequest(username)) {
	        		  //player.sendMessage(pluginColor + "There are X more requests to consider");
	        		  
		        	  //Lets do it
	        		  if(datasource.doesPlayerExist(username)) {
			        	  Player user = datasource.getPlayer(username);
			        	  
			        	  //Add Group
			        	  user.addGroup(buildgroup); //- doesn't seem to work?!?
	        		  } else {
	        			  Player user = etc.getServer().matchPlayer(username);
	        			  //datasource.addPlayer(user); //Causes Hey0 crashes... -1 array?
	        			  //Player userdb = datasource.getPlayer(username);
	        			  user.addGroup(buildgroup); //- doesn't seem to work?!?
	        		  }
	        		  
	        		  //...?
	        		  Player user = etc.getServer().matchPlayer(username);
		        	  
		        	  //Message them
		        	  user.sendMessage("" + pluginColor + messageawarded);
		        	  
	        		  //Tell Mod
	        		  player.sendMessage(pluginColor + "Player build request accepted and user updated");
	        	  } else {
	        		  player.sendMessage(pluginColor + "Accept failed, have they made a request? Check /requestlist");
	        		  debugmsg("It could be a problem with the MySQL query, check server", player);
	        	  }
	        	  
	        	  return true;
	          } else {
	        	  player.sendMessage(Colors.Red + "Expected: " + split[0] + " <Username>");
	        	  return true;
	          }
	      } else if(split[0].equalsIgnoreCase("/requestlist")||split[0].equalsIgnoreCase("/rl")) {
	    	  //They allowed?
	    	  if (!player.canUseCommand("/requestlist")) return false;
	    	  
	    	  //To be implemented...
	    	  //Export list of current requests and states...
	    	  return false;
	      } else {
	    	  return false;
	      }
	  }
  }
}