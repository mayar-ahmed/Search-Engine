import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import ch.sentric.URLNORM;

public class Crawler {
	private Queue<String> frontier;
	private Queue<String> visited; 
	private HashMap <String, ArrayList<String>> robotsCache;
	private int docs;
	
	//private String lastHost;//last host that we got robots.tx for
	//private ArrayList<String> rules ; //rules for last host
	
	public Crawler(){
		
		frontier = new LinkedList<String>();
		visited = new LinkedList<String>();
		docs=0;
		//initial seeds
		frontier.add("https://www.wikipedia.org/"); 
		frontier.add("https://www.dmoz.org/"); //open directory
		//lastHost="https://www.wikiapedia.org";
		robotsCache = new HashMap<String,ArrayList<String>>();
		
		
	}
	
	public void crawlPage(){
		
		while (docs <= 20)
		{
			String url = frontier.remove();
			try{
			
				Response response = Jsoup.connect(url).userAgent("Mozilla").timeout(10000).ignoreHttpErrors(true).execute();
				int statusCode = response.statusCode();
				
				//check content type before processing
				
				URL c = new URL(url);
				URLConnection u = c.openConnection();
				String type = u.getHeaderField("Content-Type");
				
				if(statusCode == 200 && type.contains("text/html")) //successful response
				{
					//fetch page
					Document doc = Jsoup.connect(url).get();
					
					//download page here (add it to database)
					
					//add to visited pages
					visited.add(url);
					System.out.println(url);
					docs++;
					
					//extract links from page
		            Elements linksOnPage = doc.select("a[href]");
		            
		            String currentHost = c.getProtocol() + c.getHost();
		            //if( currentHost!=lastHost)
		            //loop through extracted urls
		            for (Element page : linksOnPage) 
		            {
		            	String link = page.attr("abs:href");
		            	if(! frontier.contains(link) && !visited.contains(link)) //add some constraints 
		            		{
		            			// if it satisfies all conditions add it to frontier
		            			frontier.add(link); 
		            			//System.out.println(link);
		            			
		            		}
		            }
				 }
				else
				{
				    System.out.println("received error: " + statusCode + " "+response.statusMessage());
				}
		               
				
			}catch (IOException e) {
	            System.err.println("For '" + url + "': " + e.getMessage());
			}
		}

		
		
	}

//	public ArrayList<String> parseRobots(String hostName)
//	{
//		ArrayList<String> disallowed = new ArrayList<String>();
//		 
//		   try {
//               URLNORM robotsFileUrl =
//                       new URLNORM("http://" + host + "/robots.txt");
//                
//               // Open connection to robot file URLNORM for reading.
//               BufferedReader reader =
//                       new BufferedReader(new InputStreamReader(
//                       robotsFileUrl.openStream()));
//                
//               // Read robot file, creating list of disallowed paths.
//               String line;
//               while ((line = reader.readLine()) != null) {
//                   if (line.indexOf("Disallow:") == 0) {
//                       String disallowPath =
//                               line.substring("Disallow:".length());
//                        
//                       // Check disallow path for comments and 
//                       // remove if present.
//                       int commentIndex = disallowPath.indexOf("#");
//                       if (commentIndex != - 1) {
//                           disallowPath =
//                                   disallowPath.substring(0, commentIndex);
//                       }
//                        
//                       // Remove leading or trailing spaces from 
//                       // disallow path.
//                       disallowPath = disallowPath.trim();
//                        
//                       // Add disallow path to list.
//                       disallowList.add(disallowPath);
//                   }
//               }
//                
//               // Add new disallow list to cache.
//               disallowListCache.put(host, disallowList);
//           } catch (Exception e) {
//       /* Assume robot is allowed since an exception
//          is thrown if the robot file doesn't exist. */
//               
//           }
//       }
//		return disallowed;
//	}
}

