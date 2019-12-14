package chatgroupmysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class dbOperations {
    private final String USERNAME = "root";
    private final String PASSWORD = "";
    private final String CON = "jdbc:mysql://localhost:3306/chatgroup";
    private Connection con = null;
    private Statement stmt = null;
    private PreparedStatement pStmt = null;
    private ResultSet rs = null;


    // Connecting to the DB
    public Connection openConnection(){        
        try{
            con = DriverManager.getConnection(CON, USERNAME, PASSWORD);
        }catch(SQLException e){
            System.out.println("opening connection ... "+e);

        }
        return con;     
    }
    
    // Utility to retrieve data from DB
    public ResultSet getResults(String query) throws SQLException{
        con = openConnection();
        try{
            stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.executeQuery(query);
        }
        catch(SQLException e){
            System.out.println(e);
        }
        return rs;
    }

    // Utility to transform retrieved rows to a list
    public List<Map<String, Object>> fetchRows(ResultSet result) throws SQLException{
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        try{
            ResultSetMetaData md = result.getMetaData();
            int columns = md.getColumnCount();
            while (result.next()){
                Map<String, Object> row = new HashMap<String, Object>(columns);
                for(int i = 1; i <= columns; ++i){
                    row.put(md.getColumnName(i), result.getObject(i));
                }
                rows.add(row);
            }
        }
        catch(SQLException e){
            System.out.println("fetching rows ... " + e);
        } finally{
            if(rs != null){
                rs.close();
            }
            if(stmt != null){
                stmt.close();
            }   
            if(con != null){
                con.close();
            }           
        }      
        return rows;
    }
    
    // Create account
    public int newAccount(String username, String password) throws SQLException{
        con = openConnection();
        int user_id = -1;
        try{
            pStmt = con.prepareStatement("insert into users (username, password)  values ( ?, ?)",Statement.RETURN_GENERATED_KEYS);
            pStmt.setString(1, username);
            pStmt.setString(2, password);
            pStmt.executeUpdate();
            rs = pStmt.getGeneratedKeys();
            if(rs.next()){
                user_id = rs.getInt(1);
            }
        }
        catch(SQLException e){
            System.out.println("creating a topic..." + e);
        }
        finally{
            if(pStmt != null){
                pStmt.close();
            }   
            if(rs != null){
                rs.close();
            }
            if(con != null){
                con.close();
            }            
        }
        return user_id;
    }
    
    // Create a new topic 
    public int createTopic(int userID, String topicName) throws SQLException{
        con = openConnection();
        int topic_id = -1;
        try{
            pStmt = con.prepareStatement("insert into topics (user_id, topic_name)  values ( ?, ?)",Statement.RETURN_GENERATED_KEYS);
            pStmt.setInt(1, userID);
            pStmt.setString(2, topicName);
            pStmt.executeUpdate();
            rs = pStmt.getGeneratedKeys();
            if(rs.next()){
                topic_id = rs.getInt(1);
                subscribe(userID, topic_id);                
            }
        }
        catch(SQLException e){
            System.out.println("creating a topic..." + e);
        }
        finally{
            if(pStmt != null){
                pStmt.close();
            }
            if(rs != null){
                rs.close();
            } 
            if(con != null){
                con.close();
            }            
        }
        return topic_id;
    }
    
    // subscribe to a topic
    public void subscribe(int userID, int topicID) throws SQLException{
        con = openConnection();
        try{
            pStmt = con.prepareStatement("insert into subscribers (user_id, topic_id)  values ( ?, ?)");
            pStmt.setInt(1, userID);
            pStmt.setInt(2, topicID);
            pStmt.executeUpdate();
        }
        catch(SQLException e){
            System.out.println("creating a topic..." + e);
        }
        finally{
            if(pStmt != null){
                pStmt.close();
            }            
            if(con != null){
                con.close();
            }            
        }
    }
    
    // save a message published in a topic
    public void saveMesssage(int userID, int topicID, String message) throws SQLException{
        con = openConnection();
        try{
            pStmt = con.prepareStatement("insert into messages (user_id, topic_id, message)  values ( ?, ?, ?)");
            pStmt.setInt(1, userID);
            pStmt.setInt(2, topicID);
            pStmt.setString(3, message);
            pStmt.executeUpdate();
        }
        catch(SQLException e){
            System.out.println("saving a message..." + e);
        }
        finally{
            if(pStmt != null){
                pStmt.close();
            }            
            if(con != null){
                con.close();
            }            
        }

    }
    
    // authenticate using username and pwd
    public List<Map<String, Object>> auth(String username, String password) throws SQLException{
        List<Map<String, Object>> user = new ArrayList<Map<String, Object>>();
        try{
            ResultSet rs = getResults("SELECT * FROM users WHERE username = '" + username + "' AND password = '" + password + "'");
            user = fetchRows(rs);
        }
        catch(SQLException e){
            System.out.println(e);
        }       
        return user;
    }
    
    // get a user by an id
    public List<Map<String, Object>> getUserByID(int userID){
        List<Map<String, Object>> user = new ArrayList<Map<String, Object>>();
        try{
            ResultSet rs = getResults("SELECT * FROM users WHERE user_id = " + userID);
            user = fetchRows(rs);
        }
        catch(SQLException e){
            System.out.println(e);
        }       
        return user;
    }
    
    // get all the users
    public List<Map<String, Object>> getUsers() throws SQLException{
        List<Map<String, Object>> users = new ArrayList<Map<String, Object>>();
        try{
            ResultSet rs = getResults("SELECT * FROM users");
            users = fetchRows(rs);
            
        }
        catch(SQLException e){
            System.out.println(e);
        }       
        return users;
    }
    
    // get all the topics
    public List<Map<String, Object>> getTopics() throws SQLException{
         List<Map<String, Object>> topics = new ArrayList<Map<String, Object>>();
        try{
            ResultSet rs = getResults("SELECT * FROM `topics` ");
            topics = fetchRows(rs);
        }
        catch(SQLException e){
            System.out.println("fetching topics ... " + e);
        }    
        return topics;

    }    
    
    // get all user's topics 
    public List<Map<String, Object>> getTopicsByUserID(int userID) throws SQLException{
         List<Map<String, Object>> topics = new ArrayList<Map<String, Object>>();
        try{
            ResultSet rs = getResults("SELECT * FROM `topics` WHERE user_id = " + userID + " ORDER BY date");
            topics = fetchRows(rs);
        }
        catch(SQLException e){
            System.out.println("fetching user's topics ... " + e);
        }    
        return topics;

    } 
    
    // get subscribers by topic id
    public List<Map<String, Object>> getSubscribersByTopic(int topicID) throws SQLException{
        List<Map<String, Object>> subscribers = new ArrayList<Map<String, Object>>();
        try{
            ResultSet rs = getResults("SELECT username FROM `topics` "
                    + "INNER JOIN subscribers ON subscribers.topic_id = topics.topic_id "
                    + "INNER JOIN users ON users.user_id = subscribers.user_id "
                    + "WHERE topics.topic_id = " + topicID + " ORDER BY subscribers.date");
            subscribers = fetchRows(rs);
        }
        catch(SQLException e){
            System.out.println(e);
        }
       
        return subscribers;
    }
    
    // get messages in a topic
    public List<Map<String, Object>> getMessagesByTopic(int topicID) throws SQLException{
        List<Map<String, Object>> messages = new ArrayList<Map<String, Object>>();
        try{
            ResultSet rs = getResults("SELECT username, message, messages.date FROM `topics` "
                    + "INNER JOIN messages ON messages.topic_id = topics.topic_id "
                    + "INNER JOIN users ON users.user_id = messages.user_id "
                    + "WHERE topics.topic_id = " + topicID  + " ORDER BY messages.date");
            messages = fetchRows(rs);
        }
        catch(SQLException e){
            System.out.println("fetching messages ... " + e);
        }
       
        return messages;
    }
    
    // get topic's owner
    public List<Map<String, Object>> getTopicOwner(int topicID) throws SQLException{        
        List<Map<String, Object>> owner = new ArrayList<Map<String, Object>>();
        try{
            ResultSet rs = getResults("SELECT users.user_id, username FROM `topics` "
                    + "INNER JOIN users ON users.user_id = topics.user_id "
                    + "WHERE `topic_id` = '" + topicID + "'");
            owner = fetchRows(rs);
        }
        catch(SQLException e){
            System.out.println("fetching owner ... " + e);
        }    
        return owner;
    }  
}
