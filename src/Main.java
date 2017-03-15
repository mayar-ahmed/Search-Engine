import java.net.MalformedURLException;
import java.net.URISyntaxException;
//import java.net.URLNORM;

import com.trigonic.jrobotx.RobotExclusion;
import ch.sentric.URLNORM;

public class Main {

    public static void main(String[] args) throws URISyntaxException {

        //DB m = new DB();

        RobotExclusion robotExclusion = new RobotExclusion();

        try {
            URLNORM url = new URLNORM("http://www.example.com:80/bar.html");
            String x = url.getNormalizedUrl();
            System.out.print(x);
        } catch (MalformedURLException e) {

        }


    }

}
