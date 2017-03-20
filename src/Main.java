import ch.sentric.URLNORM;
import com.trigonic.jrobotx.RobotExclusion;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;


import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;


public class Main {

    public static void main(String[] args) {


        int size=10;
        Crawler c = new Crawler();
        ThreadController controller = new ThreadController(size);
        controller.crawl(c);
        controller.update(c);

      /*  StopWatch stopwatch = new StopWatch();
        stopwatch.start();
        int size=10;
        Crawler c = new Crawler();
        threadedCrawler a = new threadedCrawler(c);
        Thread[] threads = new Thread[size];
        //crawl websites
        for (int i = 0; i < size; i++) {
            threads[i] = new Thread(a);
            threads[i].start();
        }

        //wait for them to finish
        for (int i = 0; i < size; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("all threads finished crawling ");
        stopwatch.stop();
        System.out.println("time (minutes) elapsed in crawling : ");
        long timeTaken = stopwatch.getTime();
        System.out.println((timeTaken/1000)/60);

        //update crawled pages
        for (int i = 0; i < size; i++) {
            threads[i] = new Thread(a);
            threads[i].start();
        }

        for (int i = 0; i < size; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
*/

        System.out.println("all threads finished updating");




    }

}
