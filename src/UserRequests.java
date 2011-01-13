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
 * See source or below for todo list
 * 
 * @author Pez Cuckow - email@pezcuckow.com
 *
 */

/*
 * To do:
 *  - Ask mod to check email address
 *  - Allow flatfile storage?!?
 *  - Get fancy and make this mod send emails instead of needing cron?
 *  - Allow choosing of the plugins color
 *  - Timer that messages mods and above how many requests there are
 *  - Request status method
 *  - Email users if not online when accepted
 *  - Include updatr support
 *
 *
 * Done: 
 *  - Messaging mods when request made 0.4
 *  - It's own properties file 0.4
 *  - Fix problem with server not saving changes sometimes 0.5
 *  - Prevent user requesting more than once 0.5
 *  - Prevent user requesting once accepted 0.5
 *  - Useful server output (mod name accepted bla bla's request, bla bla requested) 0.5
 *  - Request list to list current requests 0.5 
 *  
 * Changes:
 *  0.5b:
 *  	Fix save problems, prevent user requesting more than once, optimisations, useful server output, listrequests
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
  //General
  private UserRequests.Listener l = new UserRequests.Listener(this);
  protected static final Logger log = Logger.getLogger("Minecraft");
  public static String name = "UserRequests";
  public static String version = "0.6";
  public static String propFile = "UserRequests.properties";
  public static PropertiesFile props;
  private static UserRequestsDataSource ds;
  public static String connectorJar = "mysql-connector-java-bin.jar";
  public static String pluginColor = Colors.Red; //"\u00a74"
  public static String pluginAltColor = Colors.Gold; //"\u00a74"
  private static DataSource datasource = etc.getDataSource();
   
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
  public static String messagerepeat = "You've already made a request, the status is:";
  
  //Other
  public static String buildgroup = "builder";
  public static boolean debug = false;
  public static boolean serverinfo = true;
  
  /** Get the plugin ready for use */
  public void enable() {
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

  /** Disable the plugin */
  public void disable() {
	etc.getInstance().removeCommand("/request");
	etc.getInstance().removeCommand("/requestaccept");
    log.info(name + " " + version + " disabled");
  }

  /** Initialize the plugin (listeners) */
  public void initialize() {
	etc.getLoader().addListener(PluginLoader.Hook.COMMAND, this.l, this, PluginListener.Priority.MEDIUM);
  }

  /** Load the properties file */
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
    messagerepeat = props.getString("message-alredyexists", messagerepeat);
        
    //Group to Add
    buildgroup = props.getString("awarded-group", buildgroup);
    
    //Other
    debug = props.getBoolean("debug-mode", debug);
    serverinfo = props.getBoolean("server-output", serverinfo);
    
    File file = new File(propFile);
    return file.exists();
  }
  
  /* Debug Messages */
  private void debugmsg(String message, Player player) {
	  if(debug) player.sendMessage("[DEBUG]: " + message);
  }

  /* Listener for updates */
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
	        	  if(emailValidator.validate(email)) {
	        		  //Already made a request?!?
	        		  if(ds.requestExists(player.getName())) {
	        			  player.sendMessage(pluginColor + messagerepeat + " " + UserRequests.ds.requestStatusText(player.getName()));	        			  
	        		  } else {
	        			//Make request
		        		  ds.newRequest(player.getName(), email);
	        			  
	        			//Tell Player
	        			  player.sendMessage(pluginColor + messagesubmitted);
	        			  
	        			//Tell Server
	        			  if(serverinfo) log.info(UserRequests.name + ": " + player.getName() + " requested build access");
	        			  
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
		    	  //Users name (in theory)
		    	  String username = split[1].toLowerCase();
		    	  
		    	  //Is their email realistic human based check... here...
		    	  
	        	  if(ds.acceptRequest(username)) {
	        		  //player.sendMessage(pluginColor + "There are X more requests to consider");
	        		  
	        		  //New Group
	        		  setPlayerGroup(username, buildgroup);
	        		  Player requester = getPlayerByName(username);
		        	  
		        	  //Message them
	        		  requester.sendMessage("" + pluginColor + messageawarded);
		        	  
	        		  //Tell Mod
	        		  player.sendMessage(pluginColor + "Player build request accepted and user updated");
	        		  
	        		  //Tell Server
	        		  if(serverinfo) log.info(UserRequests.name + ": " + player.getName() + " granted build access to " + username);
	        	  } else {
	        		  player.sendMessage(pluginColor + "Accept failed, have they made a request? Check /requestlist");
	        		  debugmsg("It could be a problem with the MySQL query, check server", player);
	        	  }
	        	  
	        	  return true;
	          } else {
	        	  player.sendMessage(pluginColor + "Expected: " + split[0] + " <Username>");
	        	  return true;
	          }
	      } else if(split[0].equalsIgnoreCase("/requestlist")||split[0].equalsIgnoreCase("/rl")) {
	    	  //They allowed?
	    	  if (!player.canUseCommand("/requestlist")) return false;
	    	  
	    	  if (variables==1) {
	    		  //Nothing given...
	    		  player.sendMessage(pluginAltColor + "Expected: " + split[0] + " <waiting|accepted> [Page], Default shown");
	    		  String[][] response = ds.requestsWithStatus(0);
	    		  if(response!=null&&response[0]!=null&&response[0][0]!=null) {
		    		  try {
		    			  for(int i = 0; i < response.length||i < 7; i++) {
			    			  player.sendMessage(pluginColor + "Request: " + response[i][0] + " - " + ds.requestStatusToText(response[i][2]));
			    		  }
		    		  } catch(NumberFormatException e) {
		    			  //Just cause the array is too big
		    		  }
		    		  if(response.length>7) player.sendMessage(pluginAltColor + "Use: " + split[0] + " <wait|acc> [Page] for other page(s)");
		    		  player.sendMessage(pluginAltColor + "Use: /requestinfo <Username> for further info on a request");
	    		  } else {
	    			player.sendMessage(pluginColor + "No requests");  
	    		  }
	    		  return true;
	    	  } else if (variables==2&&(split[1].equalsIgnoreCase("waiting")||split[1].equalsIgnoreCase("accepted")||
	    		  						split[1].equalsIgnoreCase("wait")||split[1].equalsIgnoreCase("acc"))) {
	    		  //Waiting or accepted
	    		  int qStatus = 0;
	    		  if(split[1].equalsIgnoreCase("waiting")||split[1].equalsIgnoreCase("wait")) qStatus = 0;
	    		  else if(split[1].equalsIgnoreCase("accepted")||split[1].equalsIgnoreCase("acc")) qStatus = 3;
	    		  String[][] response = ds.requestsWithStatus(qStatus);
	    		  
	    		  if(response!=null&&response[0]!=null&&response[0][0]!=null) {
		    		  try {
		    			  for(int i = 0; i < response.length||i < 7; i++) {
			    			  player.sendMessage(pluginColor + "Request: " + response[i][0] + " - " + ds.requestStatusToText(response[i][2]));
			    		  }
		    		  } catch(NumberFormatException e) {
		    			  //Just cause the array is too big
		    		  }
		    		  if(response.length>7) player.sendMessage(pluginAltColor + "Use: " + split[0] + " <wait|acc> [Page] for other page(s)");
		    		  player.sendMessage(pluginAltColor + "Use: /requestinfo <Username> for further info on a request");
	    		  } else {
	    			player.sendMessage(pluginColor + "No requests");  
	    		  }
	    		  return true;
	    	  } else if (variables==3&&(split[1].equalsIgnoreCase("waiting")||split[1].equalsIgnoreCase("accepted")||
	    		  						split[1].equalsIgnoreCase("wait")||split[1].equalsIgnoreCase("acc"))
	    		  		 &&isNumeric(split[2])) {
	    		  //Waiting or accepted
	    		  int qStatus = 0;
	    		  if(split[1].equalsIgnoreCase("waiting")||split[1].equalsIgnoreCase("wait")) qStatus = 0;
	    		  else if(split[1].equalsIgnoreCase("accepted")||split[1].equalsIgnoreCase("acc")) qStatus = 3;
	    		  String[][] response = ds.requestsWithStatus(qStatus);
	    		  
	    		  if(response!=null&&response[0]!=null&&response[0][0]!=null) {
		    		  int page = Integer.parseInt(split[2]);
		    		  if(page<0) page = 1;
		    		  
		    		  try {
			    		  for(int i = ((page)*7)-8; i < response.length||i < page*7; i++) {
			    			  player.sendMessage(pluginColor + response[i][0] + " - " + ds.requestStatusToText(response[i][2]));
			    		  }
		    		  } catch(NumberFormatException e) {
		    			  //Just cause the array is too big
		    		  }
		    		  
		    		  if(response.length>7) player.sendMessage(pluginAltColor + "Use: " + split[0] + " <wait|acc> [Page] for other page(s)");
		    		  player.sendMessage(pluginAltColor + "Use: /requestinfo <Username> for further info on a request");
	    		  } else {
	    			  player.sendMessage(pluginColor + "No requests"); 
	    		  }
	    		  return true;
	    	  } else {
	        	  player.sendMessage(pluginColor + "Expected: " + split[0] + " <waiting|accepted> [Page]");
	        	  return true;
	          }
	    	  //To be implemented...
	    	  //Print list of current requests and states...
	      } else if(split[0].equalsIgnoreCase("/requestinfo")||split[0].equalsIgnoreCase("/ri")) {
	    	  //They allowed?
	    	  if (!player.canUseCommand("/requestinfo")) return false;
	    	  return true;
	      } else {
	    	  return false;
	      }
	  }
  }
  
  //// Methods ///////
  /** Get a player reference from name */
  public static Player getPlayerByName(String name) {
	//Loop over live players atm
    for (Object cPlayers = etc.getServer().getPlayerList().iterator(); ((Iterator)cPlayers).hasNext(); ) {
      Player localPlayer = (Player)((Iterator)cPlayers).next();
      if (localPlayer.getName().equalsIgnoreCase(name)) {
        return localPlayer;
      }
    }
    
    //Check data source then...
    Player dbPlayer = datasource.getPlayer(name);
    if(dbPlayer!=null) {
		if (dbPlayer.getName().equalsIgnoreCase(name)) {
	        return dbPlayer;
	    }
    }

    //Try another match
    Player localObject = etc.getServer().matchPlayer(name);

    if (localObject != null) {
      return localObject;
    }

    //Can't find user...
    return null;
  }
  
  /** Set a players group by name */
  public static boolean setPlayerGroup(String playerName, String groupName) {
    if (getPlayerByName(playerName) != null) {
      //Get reference from name
      Player localPlayer = getPlayerByName(playerName);
      etc.getInstance(); //was told to do this..?
      Group localGroup = datasource.getGroup(groupName);
      if (localGroup != null) {
        String[] gName = { localGroup.Name };
        localPlayer.setGroups(gName); //Replace current groups, rather than add
        localPlayer.setIgnoreRestrictions(localGroup.IgnoreRestrictions);
        localPlayer.setAdmin(localGroup.Administrator);

        if (!datasource.doesPlayerExist(localPlayer.getName())) {
        	datasource.addPlayer(localPlayer);  
        	return true;
        } else {
        	datasource.modifyPlayer(localPlayer);
        	return true;
        }
      } else {
        return false;
      }
    } else {
      return false;
    }
  }
  
  /** Validates if input String is a number */
  public boolean isNumeric(String in) {
      try {
          Integer.parseInt(in); 
      } catch (NumberFormatException ex) {
          return false;
      }
      
      return true;
  }
}