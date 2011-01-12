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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

public class UserRequestsMySQL extends UserRequestsDataSource
{
  private String name = UserRequests.name;

  protected static final Logger log = Logger.getLogger("Minecraft");
  private static String sqlTruncateTable = "TRUNCATE `" + UserRequests.table + "`";

  private static String sqlMakeTable = "CREATE TABLE IF NOT EXISTS  `" + UserRequests.table + "` (" +
  	"`username` VARCHAR( 32 ) NOT NULL," +
  	"`email` VARCHAR( 255 ) NOT NULL," +
  	"`status` INT( 11 ) NOT NULL," +
  	"`date` DATETIME NOT NULL DEFAULT  ''," +
  	"`comment` VARCHAR( 255 ) NOT NULL," +
  	"`accepted` DATETIME DEFAULT NULL," +
  	"PRIMARY KEY (  `username` )" +
  ") ENGINE=MyISAM DEFAULT CHARSET=latin1";
  private static String sqlCheckTableExist = "SHOW TABLES LIKE '" + UserRequests.table + "'";
  private static String sqlNewRequest = "REPLACE INTO `" + UserRequests.table + "` (`username`, `email`, `status`, `date`) VALUES (?, ?, 0, NOW())";
  private static String sqlAcceptRequest = "UPDATE `" + UserRequests.table + "` SET `status`=2, `accepted`=NOW() WHERE `username`=?";
  private static String sqlRequestStatus = "SELECT * FROM  `" + UserRequests.table + "` WHERE  `username`=?";

  public boolean init()
  {
    return createTable();
  }
  
  public boolean newRequest(String username, String email) {
	return execute(sqlNewRequest, username, email);
  }
  
  public boolean acceptRequest(String username) {
	  //Abandonned code is for checking user exists first...
	    /*ResultSet res = executeQuery(sqlRequestStatus, username);
		try {
			res.first();
			if(UserRequests.debug) {
				log.severe(UserRequests.name + " Debug: user status " + res.getString("status"));
			}
			
			if(res!=null&&(Integer.parseInt(res.getString("status"))==0)) {*/
	return execute(sqlAcceptRequest, username);
			/*}
		} catch (SQLException ex) {
			log.severe(UserRequests.name + ": " + ex.getMessage());
		}*/
  }

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

  private boolean checkConnection(Connection conn) throws SQLException {
    if (conn == null) {
        log.severe("Could not connect to the database. Check your credentials in UserRequests.properties");
      throw new SQLException();
    }
    if (!conn.isValid(5)) {
      log.severe("Could not connect to the database.");
      throw new SQLException();
    }
    return true;
  }

  private boolean execute(String sql) {
    return execute(sql, null, null);
  }
  
  private boolean execute(String sql, String player) {
	    return execute(sql, player, null);
  }

  private boolean execute(String sql, String var1, String var2) {
    Connection conn = null;
    PreparedStatement ps = null;
    try {
      conn = getConnection();
      ps = conn.prepareStatement(sql);
      if ((var1!=null) && (!var1.equalsIgnoreCase(""))) {
        ps.setString(1, var1);
      }
      if ((var2!=null) && (!var2.equalsIgnoreCase(""))) {
    	  ps.setString(2, var2);
      }

      if (ps.execute())
        return true;
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

  private ResultSet executeQuery(String sql) {
	  return executeQuery(sql, null, null);
  }
	  
  private ResultSet executeQuery(String sql, String player) {
	return executeQuery(sql, player, null);
  }  
  
  private ResultSet executeQuery(String sql, String var1, String var2) {
	  	Connection conn = null;
	  	PreparedStatement ps = null;
	    ResultSet rs = null;
	    try {
	        conn = getConnection();
	        ps = conn.prepareStatement(sql);
	        if ((var1!=null) && (!var1.equalsIgnoreCase(""))) {
	          ps.setString(1, var1);
	        }
	        if ((var2!=null) && (!var2.equalsIgnoreCase(""))) {
	      	  ps.setString(2, var2);
	        }

	      rs = ps.executeQuery();
	      return rs;
	    }
	    catch (SQLException ex) {
	      log.severe(this.name + ": " + ex.getMessage());
	      String msg = this.name + ": could not execute the sql query \"" + sql + "\"";
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

	    return null;
	  }
  
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
}