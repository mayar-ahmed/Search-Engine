/**
 * Created by mayar on 3/17/17.
 */
public class ThreadedCrawler implements Runnable {

    private Crawler c;

    public ThreadedCrawler(Crawler cr) {
        c = cr;

    }

    public void run() {

        /*if (c.getDocs() <2000) {*/
            c.crawl();
            System.out.println("thread with id " + Thread.currentThread().getId() + " finished crawling");
//        } else {
//            c.updateVisited();
//            System.out.println("thread with id " + Thread.currentThread().getId() + " finished updating");
//        }

    }
}

