import org.apache.commons.lang3.time.StopWatch;

public class ThreadController {

    Thread [] myThreads;
    int size;

    public ThreadController( int s){
        size=s;
        myThreads = new Thread[size];

    }
    public void crawl(Crawler c)
    {
        StopWatch stopwatch = new StopWatch();
        stopwatch.start();
        ThreadedCrawler a = new ThreadedCrawler(c);
        //crawl websites
        for (int i = 0; i < size; i++) {
            myThreads[i] = new Thread(a);
            myThreads[i].start();
        }

        //wait for them to finish
        for (int i = 0; i < size; i++) {
            try {
                myThreads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("all threads finished crawling ");
        stopwatch.stop();
        System.out.println("time (minutes) elapsed in crawling : ");
        long timeTaken = stopwatch.getTime();
        System.out.println((timeTaken/1000)/60);


    }

    public void update(Crawler c)
    {
        StopWatch stopwatch = new StopWatch();
        stopwatch.start();
        ThreadedUpdater a = new ThreadedUpdater(c);
        //update crawled websites
        for (int i = 0; i < size; i++) {
            myThreads[i] = new Thread(a);
            myThreads[i].start();
        }

        //wait for them to finish
        for (int i = 0; i < size; i++) {
            try {
                myThreads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("all threads finished updating ");
        stopwatch.stop();
        System.out.println("time (minutes) elapsed in updating : ");
        long timeTaken = stopwatch.getTime();
        System.out.println((timeTaken/1000)/60);
    }

}
