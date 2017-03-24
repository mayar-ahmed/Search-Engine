/**
 * Created by mayar on 3/20/17.
 */
public class ThreadedUpdater implements Runnable {
    private Crawler c;

    public ThreadedUpdater(Crawler cr) {
        c = cr;

    }

    public void run() {

            c.updateVisited();
            System.out.println("thread with id " + Thread.currentThread().getId() + " finished updating");

    }
}

