import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class Indexer {
	 
	private static ArrayList<String> SWords;		//stop words list
	//regular expression for link (URL) detection
	private static String LinkRegex = "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
	private HashMap<String, StemPost> Terms;
	private HashMap<String, HashMap<Integer,Posting>> StopWords;
	private HashMap<Integer,String> Documents;
	
	public Indexer(HashMap<Integer,String> docs)
	{
	Terms = new HashMap<String, StemPost>();
	Documents = docs;
	StopWords = new HashMap<String, HashMap<Integer,Posting>>();
	}
	public HashMap<String,StemPost> getTerms()
	{
		return Terms;
	}
	public HashMap<String, HashMap<Integer,Posting>> getStopWords()
	{
		return StopWords;
	}
	public static void readStopWords(){
		int i=0;
		SWords = new ArrayList<String>(174);
		try{    
			BufferedReader in = new BufferedReader(new FileReader("D:/Java/Indexer/src/StopWords.txt"));
			String s = new String();
		      while((s = in.readLine())!= null)
		      { 
		    	  SWords.add(i++, s.replaceAll("[^a-zA-Z]", "").toLowerCase());
		      }
		      in.close(); 
		      }
		  catch(IOException e) {
			  System.out.println("EXCEPTION STOPWORDS!!");
		  };
		  
	}
	public void addStopWord(String k,int docid,int f,String p)
	{
		HashMap<Integer,Posting> mp = StopWords.get(k);
		//New Term
		if(mp == null)
		{
			mp = new HashMap<Integer,Posting>();
			mp.put(docid, new Posting(f,p));
			StopWords.put(k, mp);
		}
		else
		{
			Posting post = mp.get(docid);
			//Term exists but Doc doesn't
			if(post == null)
			{
				post = new Posting(f,p);
				mp.put(docid,post);
			}
			//Term and Doc exist -> Inc Tf only
			else
			{
				post.Tf = post.Tf+1;
			}
			 
				}
	}
	public String stem(String word)
	{
		Stemmer s = new Stemmer();
		s.add(word.toCharArray(),word.length());
		s.stem();
        return s.toString();
	}
	public void addTerm(String k,int docid,int f,String p)
	{
		//check if stop word return
		if(SWords.contains(k))
		{
			this.addStopWord(k,docid,f,p);
			return;
		}
		String s = this.stem(k);
		
		StemPost rec = Terms.get(k);
		//New Term
		if(rec == null)
		{
			rec = new StemPost();
			//put stem
			rec.DocMap.put(docid, new Posting(f,p));
			rec.Stem=s;
	        Terms.put(k,rec);
		}
		else
		{
			Posting post = rec.DocMap.get(docid);
			//Term exists but Doc doesn't
			if(post == null)
			{
				post = new Posting(f,p);
				rec.DocMap.put(docid,post);
			}
			//Term and Doc exist -> Inc Tf only
			else
			{post.Tf = post.Tf+1;
			}
			 
		}
	}
	public void addTokens(String[] s, int docId,String pos)
	{
		for(int i=0;i<s.length;i++)
		{
			s[i] = s[i].toLowerCase();
			if(s[i].trim().equals(""))
				continue;
			
			if(s[i].contains("-"))
			{
				String[] dashed = s[i].split("-");
				
				for(int j=0;j<dashed.length;j++)
				{
					if(dashed[j].length()>50 || dashed[j].length()<2 || dashed[j].matches("[0-9]+"))
						continue;
					this.addTerm(dashed[j],docId,1,pos);
				}
				s[i] = s[i].replace("-","");
				if(s[i].length()>50 || s[i].length()<2 || s[i].matches("[0-9]+"))
					continue;
				this.addTerm(s[i],docId,1,pos);
			}
			else{
				if(s[i].length()>50 || s[i].length()<2 || s[i].matches("[0-9]+"))
					continue;
				this.addTerm(s[i],docId,1,pos);
			}
		}
	}
	public void index()
	{
		System.out.println("Indexing .. ");
		this.tokenizer();
	}
	public void tokenizer()
	{
		for (Map.Entry<Integer, String> entry : Documents.entrySet())
		{
		
		int j = entry.getKey();
		String input = entry.getValue();
		Document doc = Jsoup.parse(input);
		
		String title = doc.select("title").text();
		title = title.replaceAll(LinkRegex, "");
		title = title.replaceAll("[.']", "");
		String[] titles = title.split( "[^a-zA-Z0-9-]" );
		
		String hTag = doc.select("h1, h2, h3, h4, h5, h6, a").text();
		hTag = hTag.replaceAll(LinkRegex, "");
		hTag = hTag.replaceAll("[.']", "");
		String[] hTags = hTag.split("[^a-zA-Z0-9-]");
		
		doc.select("h1, h2, h3, h4, h5, h6, a, title").remove();
		String other = doc.text();
		
		System.out.println(j);
		System.out.println(other);
		other = other.replaceAll(LinkRegex, "");
		other = other.replaceAll("[.']", "");
		String[] others = other.split( "[^a-zA-Z0-9-]" );
		
		this.addTokens(titles, j, "Title");
		this.addTokens(hTags, j, "Heading");
		this.addTokens(others, j, "Content");
		
		System.out.printf("Doc #%d tokenized",j);
		
		}
		
		
		try{
		    PrintWriter writer = new PrintWriter("out.txt");
		    writer.println(Terms.size());
			for (Map.Entry<String, StemPost> entry : Terms.entrySet())
			{				
				writer.printf("Term:%s Stem:%s, Df: %d\n",entry.getKey(),entry.getValue().Stem,entry.getValue().DocMap.size());
			    
			    for(Map.Entry<Integer,Posting> entry2 : (entry.getValue()).DocMap.entrySet())
			    {
			    	writer.println(entry2.getKey() + "/" + entry2.getValue().Tf + "/" + entry2.getValue().Pos);
			    }
			    
			}
		    writer.close();
		} catch (IOException e) {
		   
		}
		
	}

}
