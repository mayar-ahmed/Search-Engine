import org.apache.commons.io.FileUtils;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.*;

import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import com.trigonic.jrobotx.RobotExclusion;
import ch.sentric.URLNORM;
import sun.awt.image.ImageWatched;


public class Crawler {
    private Queue<String> frontier;
    private HashSet<String> visited;
    private int docs;
    private RobotExclusion robots;
    private int refreshed;
    private int stop;


    public Crawler(int stop) { //initialize crawler

        frontier = new LinkedList<String>();
        visited = new HashSet<String>();
        docs = 0;
        refreshed = 0;
        this.stop = stop;

        loadHistory();
        System.out.println("finished loading history");
    }

    public void crawl() {


        robots = new RobotExclusion();
        while (true) {

            String url = "";
            synchronized (this) {

                if (docs > stop) // if stopping condition is true, stop crawling
                    break;
                while (frontier.isEmpty()) {
                    System.out.println("empty frontier");
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                url = frontier.remove();
                if (visited.contains(url))
                    continue;

                visited.add(url);
                System.out.println(url);

            }
            try {

                //if page isn't allowed by robots don't access it
                if (!robots.allows(new URL(url), "Mozilla"))
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
                    int doc = downloadPage(response, url); //download page and extract links
                    if (doc == -1) {
                        //an error happend in downloading, get another page
                        continue;
                    }


                } else //there's an error in response, display it and continue crawling
                {
                    System.out.println("Error in: " + url + " " + statusCode + " " + response.statusMessage());

                }


            } catch (IOException e) { //exception occured while sending request to page

                System.err.println("Error in'" + url + "': " + e.getMessage());
                String m = e.getMessage();
                if (m.contains("connect timed out") || m.contains("Read timed out")) //this means internet might be down
                    Wait(); //checks connection ans wait until it's susccessful

            }
        }


    }

    public int downloadPage(Response response, String url) {
        //connection is not thread safe so each thread creates it local connection in crawl and passes it
        //fetch page
        DB crawlerDB = new DB();

        Document doc = null;

        try {
            //fetch page content from responce
            doc = response.parse();

        } catch (IOException e) {
            Wait(); //to check if internet is down, wit for it before proceeding with rest of code
            System.out.println("couldn't download " + url);
            return -1;
        }

        //get html file
        String s = doc.body().html();
        String txt = doc.body().text();


        //check language before saving by cehcking the tilte and first header in page
        String title = doc.title();
        Element h1 = doc.getElementsByTag("h1").first();
        String txt2 = "";
        if (h1 != null) {
            txt2 = h1.text();
        }


        if (!(checkLanguage(title) && checkLanguage(txt2))) {
            System.out.println(url + " page might not be english, exclude it");
            return -1; //if not a valid lang return null
        }

        //download page here (add it to database)
        String sql = "insert into documents (url,content,text,rank,title) VALUES (?,?,?,?,?);";
        int generatedId = 0;

        if (s == null && s.isEmpty())
            s = " ";
        if (txt == null && txt.isEmpty())
            txt = " ";
        if (title == null && title.isEmpty())
            title = " ";
        try {

            PreparedStatement stmt = (PreparedStatement) crawlerDB.conn2.prepareStatement(sql);
            if (stmt == null)
                return -1;

            stmt.setString(1, url);
            stmt.setString(2, s);
            stmt.setString(3, txt);
            stmt.setDouble(4, (1.0 / visited.size()));
            stmt.setString(5, title);
            stmt.execute();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                generatedId = rs.getInt(1);
                System.out.println("generetaed key is " + generatedId);
            }
        } catch (SQLException e) {
            System.out.println("database error in downloading page");
            System.out.println(e.getMessage());
            return -1; //can't save document
        }


        // no two can access docs at the same time, lock it and incement number of downloaded documents
        synchronized (this) {

            // visited.add(url);
            docs++;
            System.out.println(url);

        }

        extractLinks(doc, generatedId);

        //successful fetch , if not null is returned
        return 0;


    }

    public void extractLinks(Document doc, int docId) {


        //get all links from document
        List<String> myLinks = getLinks(doc);

       //save links to their source
        saveLinks(myLinks, docId);

        //add extracted links to frontier if they're not duplicated
        synchronized (this) {
            try {

                //remove queued and visited from extracted links
                //first option
                /*myLinks.removeAll(frontier);
                myLinks.removeAll(visited);*/

                //second option remove duplicates
                /*LinkedHashSet<String> temp=new LinkedHashSet<String>();
                temp.addAll(frontier);
                temp.addAll(myLinks);
                frontier.clear();
                frontier.addAll(temp);
                System.out.println(frontier.size());*/

                frontier.addAll(myLinks); //add them to queue

            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }

    public List<String> getLinks(Document doc) {

        Elements linksOnPage = doc.select("a[href]");
        HashSet<String> myLinks = new HashSet<String>();
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
                if (!link.trim().equals(""))
                    System.out.println(link + " isn't a valid url");
                continue;
            }

            //check that url doesn't contain login, ads or not extracted already

            if (filterLink(normalized))
                myLinks.add(normalized);
        }

        return new ArrayList<String>(myLinks);

    }

    public Boolean filterLink(String link)
    {
        if(link.contains("login") || link.contains("signup") || link.contains("signin"))
            return false;
        if(link.contains("ads") || link.contains("sponsor") || link.contains("search"))
            return false;
        if (link.contains("stanford.edu/site") || link.contains("cookies") || link.contains("facebook"))
            return false;
        if(link.contains("about") || link.contains("privacypolicy") || link.contains("contact"))
            return false;
        if(link.contains("emergency.stanford.edu") || link.contains("support.twitter.com") || link.contains("twitter.com/tos"))
            return false;
        if(link.contains("status.twitter.com") || link.contains("privacy_policy"))
            return false;

        return true;


    }
    public void saveLinks(List<String> links, int doc) {

        //save extracted links in frontier
        DB mydb = new DB();
        for (String s : links) {
            String result = "";
            try {
                MessageDigest crypt = MessageDigest.getInstance("SHA-1");
                crypt.reset();
                crypt.update(s.getBytes("UTF-8"));
                result = new BigInteger(1, crypt.digest()).toString();

                String sql = "insert into frontier (hash,url) VALUES ( ? ,?);";

                PreparedStatement stmt = mydb.conn2.prepareStatement(sql);
                stmt.setString(1, result);
                stmt.setString(2, s);
                stmt.execute();
                linkPage(doc, result, mydb);

            } catch (SQLException e) {
                if (e.getErrorCode() == 1062) {
                    //duplicate primary key, already exists
                    linkPage(doc, result, mydb);
                }
            } catch (NoSuchAlgorithmException b) {
                System.out.println("couldn't generate hash");
            } catch (UnsupportedEncodingException c) {
                System.out.println("error in encoding to utf-8 before hashing");
            }


        }
    }

    public void linkPage(int docId, String hash, DB myDB) {


        try {
            String sql = "insert into document_link (doc_id,link_id) VALUES ( ? ,?);";

            PreparedStatement stmt = myDB.conn2.prepareStatement(sql);
            stmt.setInt(1, docId);
            stmt.setString(2, hash);
            stmt.execute();

        } catch (SQLException e) {
            System.out.println("error linking page to url for doc_id "  + docId);
        }
    }


    public void Wait() {
        if (!checkConnectivity()) {
            System.out.println("no internet connection in" + Thread.currentThread().getId());
        }

        //if there's no conenction wait till it gets back
        //else continue crawling and ignore url
        while (!checkConnectivity()) {
        }

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
        int[] chars = {8220, 8221, 8226, 8211, 8212, 732, 8482, 169, 174, 175, 8230, 8217, 8216};
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

        HashSet<String> s = new HashSet<String>();
        try {
            result = db.runSql(query);

            while (result.next()) {
                s.add(result.getString("url"));
            }

        } catch (SQLException e) {
            System.out.print("error in fetching visited from database");
            return;
        }

        System.out.println("loaded visited docs");
        //load frontier from file
        Queue<String> f = new LinkedList<String>();
        String query2 = "select url from frontier where url not in (select url from documents);";
        ResultSet result2 = null;
        try {
            result2 = db.runSql(query2);

            while (result2.next()) {
                String x = result2.getString("url");
                f.add(x);
            }

        } catch (SQLException e) {
            System.out.print("error in fetching frontier from database");
            return;
        }


        //set my frontier and visited

        visited = s;
        frontier = f;

        docs = visited.size();// set number of downloaded documents to files in database


    }

    public void updateVisited() {
        //updating downloaded documents starting from oldest
        DB crawlerDB = new DB();

        int count =0;
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
            String s1, s2, html = "";


            try {
                Response response = Jsoup.connect(url).userAgent("Mozilla").timeout(5000).ignoreHttpErrors(true).execute();
                int statusCode = response.statusCode();
                if (statusCode == 200) {
                    doc = response.parse();
                    s1 = doc.body().text();//from interner
                    html = doc.body().html();

                    try {
                        //badal da a3ml query ycheck

                        //get url from database and check if text changed
                        String q2 = "select id from documents where url = ? and text =? ";
                        PreparedStatement stmt = crawlerDB.conn2.prepareStatement(q2);
                        if (stmt == null)
                           continue;

                        stmt.setString(1, url);
                        stmt.setString(2, s1);

                        ResultSet res = stmt.executeQuery();
                        if(res.next()) //text found in database, no need for update
                        {
                            continue;
                        }
                        else{ //text changed, update it and

                            System.out.println("changed content in "+url);
                            count++;
                            int docId=0;

                            String sql = "UPDATE documents SET content=?, text= ?, indexed= ?, title =? WHERE url = ?;";

                            PreparedStatement stmt2 = crawlerDB.conn2.prepareStatement(sql);
                            stmt2.setString(1, html); //new page content
                            stmt2.setString(2, s1); //new text
                            stmt2.setBoolean(3, false); //not indexed
                            stmt2.setString(4,doc.title());
                            stmt2.setString(5, url);
                            stmt2.execute(); //update document

                            String sql2 = "select id from documents where url = ?; ";
                            PreparedStatement stmt3 = crawlerDB.conn2.prepareStatement(sql2);
                            stmt3.setString(1,url);
                            ResultSet rs = stmt3.executeQuery();
                            ; //get id of updated document
                            if (rs.next()) {
                                docId = rs.getInt("id");
                                System.out.println("id of updated document is " + docId);

                            }



                            //delete old links
                            deleteLinks(docId, crawlerDB);
                            System.out.println("deleted links from document_link");

                            //extract new links
                            List<String> links = getLinks(doc);

                            //save new links
                            saveLinks(links,docId);
                            System.out.println("added new links links from document_link");


                        }


                    } catch (SQLException e) {
                        System.out.println("error in updating database");
                        continue;
                    }
                }

                System.out.println(url + " updated");

            } catch (IOException e) {
                System.err.println("Error in updating " + url + "': " + e.getMessage());
                if (e.getMessage().contains("connect timed out") || e.getMessage().contains("Read timed out"))
                    Wait();


            }

        }

        System.out.println("no. of updated pages is " + count);
    }

    public void deleteLinks (int docId, DB myDb)
    {
        try{
            String query = "Delete from document_link where doc_id= ?;";
            PreparedStatement s = myDb.conn2.prepareStatement(query);
            s.setInt(1,docId);
            s.execute();

        }catch(SQLException s)
        {
            System.out.println("error in deleting links");
        }
    }


}

