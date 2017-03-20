import ch.sentric.URLNORM;
import com.trigonic.jrobotx.RobotExclusion;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;


import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;


public class Trial {

    public static void main(String[] args) {

        Queue<String> f = new LinkedList<String>();

        Scanner in = null;
        try {
            in = new Scanner(new File("frontier.txt"));
        } catch (FileNotFoundException e) {
            System.out.println("error loading frontier");
        }
        while (in.hasNext()) {
            String s= in.next();
            if(f.equals(s))
                System.out.println("duplicate : " +s );
            else
                f.add(s);

        }
        in.close();
    }
}
