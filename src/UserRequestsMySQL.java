import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class UserRequestsMySQL extends UserRequestsDataSource
{
  private String name = UserRequests.name;
  private String[][] recentrs = null;

  protected static final Logger log = Logger.getLogger("Minecraft");
  private static String sqlTruncateTable = "TRUNCATE `" + UserRequests.table + "`";

  private static String sqlMakeTable = "CREATE TABLE IF NOT EXISTS  `" + UserRequests.table + "` (" +
  	"`username` VARCHAR( 32 ) NOT NULL," +
  	"`email` VARCHAR( 255 ) NOT NULL," +
  	"`status` INT( 11 ) NOT NULL," +
  	"`date` DATETIME NOT NULL DEFAULT  ''," +
  	"`comment` VARCHAR( 255 ) NOT NULL," +
  	"`accepted` DATETIME DEFAULT NULL," +
  	"`acceptedBy` varchar(32) NOT NULL," +
  	"PRIMARY KEY (  `username` )" +
  ") ENGINE=MyISAM DEFAULT CHARSET=latin1";
  private static String sqlCheckTableExist = "SHOW TABLES LIKE '" + UserRequests.table + "'";
  private static String sqlNewRequest = "REPLACE INTO `" + UserRequests.table + "` (`username`, `email`, `status`, `date`) VALUES (?, ?, 0, NOW())";
  private static String sqlAcceptRequest = "UPDATE `" + UserRequests.table + "` SET `status`=3, `accepted`=NOW(),`acceptedBy`=? WHERE `username`=?";
  private static String sqlRequestStatus = "SELECT * FROM  `" + UserRequests.table + "` WHERE  `username`=?";
  private static String sqlRequestWithStatus = "SELECT * FROM  `" + UserRequests.table + "` WHERE  `status`=?";
  private static String sqlRequestExists = "SELECT username FROM  `" + UserRequests.table + "` WHERE  `username`=?";

/* DATABASE INFO
 * username - username of user, also unique key
 * email - email of user, for email when they are added, possibly marketing etc...
 * status - 0 not checked, 1 looked at?!?, 2 means ?!?, 3 means accepted
 * date - date they made the request
 * commend - unused, for database manager
 * accepted - date they were accepted
 */
  
  ////// LISTENERS //////
  
  /** Prepare the database */
  public boolean init() {
    return createTable();
  }
  
  /** Create/replace a new request */
  public boolean newRequest(String username, String email) {
	return execute(sqlNewRequest, username, email);
  }
  
  /** Accept a request from a user */
  public boolean acceptRequest(String username, String acceptedBy) {	 
	  if(requestStatus(username)!=3&&requestStatus(username)>=0) {
		  return execute(sqlAcceptRequest, acceptedBy, username);
	  } else {
		  return false;
	  }
  }
  
  /** Check a request exists in the DB */
  public boolean requestExists(String username) {
	if(execute(sqlRequestExists, username)) {
		if(recentrs[0][0]!=null) {
			return true;
		}
	}
	return false;
  }
  
  /** Return the request status from the DB */
  public int requestStatus(String username) {
	  if(requestExists(username)) {
		  if(execute(sqlRequestStatus, username)) {
				if(UserRequests.debug) log.info(UserRequests.name + " Debug: " + recentrs[0][0] + ", " +  recentrs[0][1] + ", " + recentrs[0][2] + ", " + recentrs[0][3] + ", " + recentrs[0][4] + ", " + recentrs[0][5] + ".");
				  
				return Integer.parseInt(recentrs[0][2]); //2 = column 3 = status...
		  }
	  }
	
	  return -1; //error
  }
  
  /** Return the request's information from the DB */
  public String[][] requestInfo(String username) {
	  if(requestExists(username)) {
		  if(execute(sqlRequestStatus, username)) {
				if(UserRequests.debug) log.info(UserRequests.name + " Debug: " + recentrs[0][0] + ", " +  recentrs[0][1] + ", " + recentrs[0][2] + ", " + recentrs[0][3] + ", " + recentrs[0][4] + ", " + recentrs[0][5] + ".");
				  
				return recentrs;
		  }
	  }
	
	  return null; //error
  }
  
  /** Request status from DB as English */
  public String requestStatusText(String username) {
	  return requestStatusToText(requestStatus(username));
  }
  
  /** Requests with a certain status */
  public String[][] requestsWithStatus(int status) {
	if(execute(sqlRequestWithStatus, "" + status)) {
		if(UserRequests.debug) log.info(UserRequests.name + " Debug: " + recentrs[0][0] + ", " +  recentrs[0][1] + ", " + recentrs[0][2] + ", " + recentrs[0][3] + ", " + recentrs[0][4] + ", " + recentrs[0][5] + ".");
		return recentrs;
	} else {
		return null;
	}		  
  }
  
  /** Delete all the data in the table?!? */
  public boolean truncateData() {
	  return execute(sqlTruncateTable);
  }  

  //////SQL MANAGEMENT BELOW///
  
  /* Connect to the DB */
  private Connection getConnection() throws SQLException {
    Connection conn = null;
	  try {
	    File file = new File(System.getProperty("user.dir") + File.pathSeparator + UserRequests.connectorJar);
	    URL jarfile = new URL("jar", "", "file:" + file.getAbsolutePath() + "!/");
	    URLClassLoader cl = URLClassLoader.newInstance(new URL[] { jarfile });
	    cl.loadClass(UserRequests.driver);
	    conn = DriverManager.getConnection(UserRequests.db, UserRequests.user, UserRequests.pass);
	  } catch (Exception e) {
	    log.severe(this.name + ": " + e.getMessage());
	  }
    checkConnection(conn);
    return conn;
  }

  /* Are we connected to the db */
  private boolean checkConnection(Connection conn) throws SQLException {
    if (conn == null) {
        log.severe(UserRequests.name + ": Could not connect to the database. Check your credentials in UserRequests.properties");
      throw new SQLException();
    }
    if (!conn.isValid(5)) {
      log.severe(UserRequests.name + ": Could not connect to the database.");
      throw new SQLException();
    }
    return true;
  }

  /* Alias */
  private boolean execute(String sql) {
	return execute(sql, null, null);
  }
  
  /* Alias */
  private boolean execute(String sql, String player) {
	return execute(sql, player, null);
  }

  /* Execute a string, returns whether it worked
   * and it sets recentrs with an array of the data
   */
  private boolean execute(String sql, String var1, String var2) {
    Connection conn = null;
    PreparedStatement ps = null;
    
    //Should probably clean everytime, to save confusion
    recentrs = null;
    
    try {
      conn = getConnection();
      ps = conn.prepareStatement(sql);
      if ((var1!=null) && (!var1.equalsIgnoreCase(""))) {
        ps.setString(1, var1);
      }
      if ((var2!=null) && (!var2.equalsIgnoreCase(""))) {
    	  ps.setString(2, var2);
      }
      
      //Run the SQL
      ps.execute();
      
      if(ps.getUpdateCount()>0) { //It's true/false
    	  if(UserRequests.debug) log.info(UserRequests.name + " Debug: Request worked, updated " + ps.getUpdateCount());
    	  return true;
      } else if(ps.getUpdateCount()<0) { //It's returning results
    	  if(UserRequests.debug) log.info(UserRequests.name + " Debug: Request worked, returned data");
    	  recentrs = resultsettoarray(ps.getResultSet());
    	  if(UserRequests.debug) log.info(UserRequests.name + " Debug: " + recentrs);
    	  return true;
      } else { //Nothing...?!?
    	  return false;
      }
    }
    catch (SQLException ex) {
      log.severe(this.name + ": " + ex.getMessage());
      String msg = this.name + ": could not execute the sql \"" + sql + "\"";
      if (var1 != null) {
        msg = msg + " - ? is " + var1;
      }
      if (var2 !=null) {
    	  msg = msg + " or ? is " + var2;
      }
      log.severe(msg);
    } finally {
      try {
        if (ps != null) {
          ps.close();
        }
        if (conn != null)
          conn.close();
      }
      catch (SQLException ex) {
        log.severe(this.name + ": " + ex.getMessage());
      }
    }
    try
    {
      if (ps != null) {
        ps.close();
      }
      if (conn != null)
        conn.close();
    }
    catch (SQLException ex) {
      log.severe(this.name + ": " + ex.getMessage());
    }
    return false;
  }
  
  /* Create the tables from SQL */
  private boolean createTable() {
    Connection conn = null;
    Statement s = null;
    try {
      conn = getConnection();
      s = conn.createStatement();
      s.executeUpdate(sqlMakeTable);
      ResultSet rs = s.executeQuery(sqlCheckTableExist);
      if (rs.first())
        return true;
    }
    catch (SQLException ex) {
      log.severe(this.name + ": " + ex.getMessage());
    } finally {
      try {
        if (s != null) {
          s.close();
        }
        if (conn != null)
          conn.close();
      }
      catch (SQLException ex) {
        log.severe(this.name + ": " + ex.getMessage());
      }
    }
    try
    {
      if (s != null) {
        s.close();
      }
      if (conn != null)
        conn.close();
    }
    catch (SQLException ex) {
      log.severe(this.name + ": " + ex.getMessage());
    }

    return false;
  }
  
  ////// ARRAY MANAGMENT ///////
  
  /*
   * Convert a results set to an array
   * Coded because I hate java... Pez
   */
  private String[][] resultsettoarray(ResultSet results) {
	try {
	  ResultSetMetaData columns = results.getMetaData();
	  
	  String[][] array;
	
	  array = new String[5][columns.getColumnCount()];
	  
	  int y = 1;
	  while(results.next()) {
		  //Make array bigger if needed, I hate java...
		  if(y==array.length) {
			  String[][] newarray = new String[(y+5)][columns.getColumnCount()];
			  for(int i = 0; i < array.length; i++) {
				  newarray[i] = array[i];
			  }
			  array = newarray;
 			  //Need bigger array, FUCK java, dumb language
		  }
		  
		  for(int x = 1; x<=columns.getColumnCount(); x++) {
			  array[y-1][x-1] = results.getString((x)); //results start at 1
			  //anyway to have x as name?!? like keys in php!
		  }
		  y++;
	  }
	  
	  //YAY!
	  return array;
	  
	} catch (SQLException ex) {
		//Oh no...
		log.severe(this.name + ": " + ex.getMessage());
	}
	return null;
  }
}