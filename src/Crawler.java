import org.apache.commons.io.FileUtils;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.MalformedURLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import com.trigonic.jrobotx.RobotExclusion;
import ch.sentric.URLNORM;


public class Crawler {
    private Queue<String> frontier;
    private Queue<String> visited;
    private int docs;
    private RobotExclusion robots;
    private File front;
    private FileWriter frontWriter;
    int refreshed;
    int stop;


    public Crawler(int stop) { //initialize crawler

        frontier = new LinkedList<String>();
        visited = new LinkedList<String>();
        docs = 0;
        refreshed = 0;
        this.stop=stop;
        front = new File("frontier.txt"); //to save frontier
        try {
            frontWriter = new FileWriter(front, true);

        } catch (IOException e) {
            System.out.println("error opening frontier file");
        }

        loadHistory();
    }

    public void crawl() {

        DB crawlDB = new DB();
        robots = new RobotExclusion();
        while (true) {

            String url = "";
            synchronized (this) {

                if (docs > stop) // if stopping condition is true, stop crawling
                    break;
                while (frontier.isEmpty()) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                url = frontier.remove();
                if(visited.contains(url))
                    continue;

                visited.add(url);

            }
            try {

                //if page isn't allowed by robots don't access it
                if (!robots.allows(new URL(url), "mayar"))
                    continue;

                //send a request to the page
                Response response = Jsoup.connect(url).userAgent("Mozilla").timeout(12000).ignoreHttpErrors(true).execute();
                int statusCode = response.statusCode();

                //check content type & language before processing
                URL c = new URL(url);
                URLConnection u = c.openConnection();
                String type = u.getHeaderField("Content-Type");

                //don't know if it's html so ignore it
                if (type == null)
                    continue;

                if (statusCode == 200 && type.contains("text/html")) //successful response && file is html
                {
                    Document doc = downloadPage(response,url, crawlDB);
                    if (doc == null) {
                        //an error happend in downloading, get another page
                        continue;
                    }
                    //extract links from downloaded page
                    extractLinks(doc);

                } else //there's an error in response, display it and continue crawling
                {
                    System.out.println("Error in: " + url + " " + statusCode + " " + response.statusMessage());

                }


            } catch (IOException e) { //exception occured while sending request to page

                System.err.println("Error in'" + url + "': " + e.getMessage());
                String m = e.getMessage();
                if(m.contains("connect timed out") || m.contains("Read timed out")) //this means internet might be down
                    Wait(); //checks connection ans wait until it's susccessful

            }
        }


    }

    public Document downloadPage(Response response, String url, DB crawlerDB) {
        //connection is not thread safe so each thread creates it local connection in crawl and passes it
        //fetch page

        Document doc = null;

        try {
            //fetch page content from responce
            doc = response.parse();

        } catch (IOException e) {
            Wait(); //to check if internet is down, wit for it before proceeding with rest of code
            System.out.println("couldn't download " + url);
            return null;
        }

        //get html file
        String s = doc.html();


        //check language before saving by cehcking the tilte and first header in page
        String txt1 = doc.title();
        Element h1 = doc.getElementsByTag("h1").first();
        String txt2="";
        if(h1!=null)
        {
            txt2=h1.text();
        }


        if (! (checkLanguage(txt1) && checkLanguage(txt2)))
        {
            System.out.println(url + " page might not be english, exclude it");
            return null; //if not a valid lang return null
        }

        //download page here (add it to database)
        String sql = "insert into documents (url,content) VALUES ( ? , ?);";

        try {

            PreparedStatement stmt = (PreparedStatement) crawlerDB.conn.prepareStatement(sql);
            stmt.setString(1, url);
            stmt.setString(2, s);
            stmt.execute();
        } catch (SQLException e) {
            System.out.println("database error in downloading page");
            return null; //can't save document
        }


        // no two can access docs at the same time, lock it and incement number of downloaded documents
        synchronized (this) {

           // visited.add(url);
            docs++;
            System.out.println(url);

        }

        //successful fetch , if not null is returned
        return doc;


    }

    public void extractLinks(Document doc) {
        synchronized (this) {
            if (frontier.size() > 10000) //no need to add more to queue they won't be visited
                return;
        }
        //extract links from page
        Elements linksOnPage = doc.select("a[href]");

        List<String> myLinks = new LinkedList<String>();
        System.out.println("extracting links");

        for (Element page : linksOnPage) //for every link on page
        {
            //get absolute url
            String link = page.attr("abs:href");

            //normalize url
            URLNORM n = null; //normalized
            String normalized = ""; //normalized with http:// at the beginning
            try {
                n = new URLNORM(link);
                normalized = (n.getScheme() + "://" + n.getNormalizedUrl()).toLowerCase();


            } catch (MalformedURLException e) { //if url isn't in  a valid format , exception thrown
                if(!link.trim().equals(""))
                    System.out.println(link + " isn't a valid url");
                continue;
            }

            //check that url doesn't contain login, ads or not extracted already

            if (!normalized.contains("login") && !normalized.contains("signup") && !normalized.contains("signin") && !myLinks.contains(normalized))
                myLinks.add(normalized);
        }


        //add extracted links to frontier if they're not duplicated
        synchronized (this){
            try {

                //remove queued and visited from extracted links
                myLinks.removeAll(frontier);
                myLinks.removeAll(visited);

                frontier.addAll(myLinks); //add them to queue

                FileUtils.writeLines(new File("frontier.txt"), frontier, false); //write frontier to file to save state

            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }


    public void Wait() {
        if (!checkConnectivity()) {
            System.out.println("no internet connection in" + Thread.currentThread().getId());
        }

        //if there's no conenction wait till it gets back
        //else continue crawling and ignore url
        while (!checkConnectivity()) {}

    }

    public boolean checkConnectivity() {
        try {
            //send request to google with large timeout, if not successful there's probably a connection problem
            Response response = Jsoup.connect("https://google.com").userAgent("Mozilla").timeout(15000).ignoreHttpErrors(true).execute();
            int statusCode = response.statusCode();

            if (statusCode == 200)
                return true; //there's a connection
            else {
                return false;//there's no conenction
            }
        } catch (IOException e) {
            return false;
        }


    }

    public boolean checkLanguage(String txt) {
        //some extended ascii codes that migh be mistaken for another language
        int[] chars = {8220, 8221, 8226, 8211, 8212, 732, 8482, 169, 174, 175,8230,8217, 8216};
        char[] myNameChars = txt.toCharArray();

        for (int i = 0; i < txt.length(); i++) {
            int x = (int) myNameChars[i];
            for (int j = 0; j < chars.length; j++) {
                if (x == chars[j]) {
                    myNameChars[i] = ' ';
                }
            }

        }
        //remove extended ascii and replace them with spaces
        String mytxt = String.valueOf(myNameChars);


        String s = mytxt.replaceAll("!", "");
        String t = s.replaceAll("|", "");
        String r = t.replaceAll("\\P{InBasic_Latin}", "!"); //replce non english unicode with !
        if (r.contains("!")) //check if there are any resulting ! after replacing
            return false; //language is non english

        return true; //english language
    }


    public void loadHistory() { //to load list of visited and last frontier (queue) from database at start

        DB db = new DB();
        String query = "SELECT url FROM documents;"; //getting list of visited from database

        ResultSet result = null;

        Queue<String> s = new LinkedList<String>();
        try {
            result = db.runSql(query);

            while (result.next()) {
                s.add(result.getString("url"));
            }

        } catch (SQLException e) {
            System.out.print("error in fetching visited from database");
            return;
        }

        //load frontier from file
        Queue<String> f = new LinkedList<String>();

        Scanner in = null;
        try {
            in = new Scanner(new File("frontier.txt"));
        } catch (FileNotFoundException e) {
            System.out.println("error loading frontier");
        }
        while (in.hasNext()) {
            String m = in.nextLine();
            if(!f.contains(m))
                f.add(m);
        }
        in.close();

        //remove visited from frontier
        f.removeAll(s);

        //set my frontier and visited

        visited = s;
        frontier = f;

        docs = visited.size();// set number of downloaded documents to files in database



    }

    public void updateVisited() {
        //updating downloaded documents starting from oldest
        DB crawlerDB = new DB();

        String[] arr = null;
        synchronized (this) {
            arr = new String[visited.size()];
            arr = visited.toArray(arr);
        }

        while (true) {
            String url = "";
            synchronized (this) {
                if (refreshed == arr.length)
                    break;
                url = arr[refreshed];
                refreshed++;
            }


            Document doc = null;
            String s = "";

            try {
                Response response = Jsoup.connect(url).userAgent("Mozilla").timeout(10000).ignoreHttpErrors(true).execute();
                int statusCode = response.statusCode();
                if (statusCode == 200) {
                    doc = response.parse();
                    s = doc.html();
                    String sql = "UPDATE documents SET content=? WHERE url = ?;";

                    try {
                        PreparedStatement stmt = (PreparedStatement) crawlerDB.conn.prepareStatement(sql);
                        stmt.setString(1, s);
                        stmt.setString(2, url);
                        stmt.execute();
                    } catch (SQLException e) {
                        System.out.println("error in updating database");
                        continue;
                    }
                }

                System.out.println(url + " updated");

            } catch (IOException e) {
                System.err.println("Error in updating " + url + "': " + e.getMessage());
                if(e.getMessage().contains("connect timed out") || e.getMessage().contains("Read timed out"))
                    Wait();


            }

        }
    }


}

