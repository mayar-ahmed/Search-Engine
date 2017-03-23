

public class Main {

    public static void main(String[] args) {


        int size=10; //number of threads
        int stop=8000; //stopping condition (no. of download docs)
        Crawler c = new Crawler(stop);
        ThreadController controller = new ThreadController(size);
        controller.crawl(c); //add pages
        controller.update(c); //update


        System.out.println("all threads finished updating");




    }

}
