

public class CrawlerMain {

    public static void main(String[] args) {


        int size=10; //number of threads
        int stop=6000; //stopping condition (no. of download docs)
        Crawler c = new Crawler(stop);
        ThreadController controller = new ThreadController(size);
        controller.crawl(c); //add pages
        controller.update(c); //update



    }

}
