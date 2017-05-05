import ch.sentric.URLNORM;
import com.trigonic.jrobotx.RobotExclusion;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;


import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;


public class Trial {

    public static void main(String[] args) {

        String s = "http://stanford.edu";
        try {
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.reset();
            crypt.update(s.getBytes("UTF-8"));
            String result = new BigInteger(1, crypt.digest()).toString();
            System.out.println(result);
            System.out.println(result.length());
        } catch (Exception e) {
            System.out.println("error in encrypting");
        }



        DB crawlerDB = new DB();
        int docId = 0;
        try {
            String sql = "UPDATE documents SET content=? ,text=? ,new=? ,rank=?  ,title =? WHERE url = ?;";

            PreparedStatement stmt2 = crawlerDB.conn2.prepareStatement(sql);
            stmt2.setString(1, "weeeasasae,,eeeee"); //new page content
            stmt2.setString(2, "blaaa,,,alsllsaaaa"); //new text
            stmt2.setBoolean(3, false); //not indexed
            stmt2.setDouble(4, 1.0 / 5);//new rank
            stmt2.setString(5, "updated doc");
            stmt2.setString(6, "https://makeagift.stanford.edu?olc=06301");
            stmt2.executeUpdate(); //update document

           String sql2 = "select id from documents where url = ?; ";
           PreparedStatement stmt3 = crawlerDB.conn2.prepareStatement(sql2);
           stmt3.setString(1,"https://makeagift.stanford.edu?olc=06301");
           ResultSet rs = stmt3.executeQuery();
           ; //get id of updated document
            if (rs.next()) {
                docId = rs.getInt("id");
                System.out.println("id of updated document is " + docId);

            }

        } catch (SQLException m) {
            System.out.println(m.getMessage());

        }
    }
}
