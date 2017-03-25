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
	//List of stop words
	private static ArrayList<String> SWords;
	//Regex for URL detection
	private static String LinkRegex = "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
	//Map of terms to be inserted in the database Map<Term,{stem,Map<DocID,{Tf,Position}>}>
	private HashMap<String, StemPost> Terms;
	//Map of stop words to be inserted in the database (Mainly for phrase queries) Map<StopWord,map<DocumentID,{Tf,Position}>>
	private HashMap<String, HashMap<Integer,Posting>> StopWords;
	//Map of <DocumentID,Document>
	private HashMap<Integer,String> Documents;
	//Constructor
	public Indexer(HashMap<Integer,String> docs)
	{
	Terms = new HashMap<String, StemPost>();
	Documents = docs;
	StopWords = new HashMap<String, HashMap<Integer,Posting>>();
	}
	//return map of terms
	public HashMap<String,StemPost> getTerms()
	{
		return Terms;
	}
	//return map of stop words
	public HashMap<String, HashMap<Integer,Posting>> getStopWords()
	{
		return StopWords;
	}
	//read stop words from file and put them into Swords list
	public static void readStopWords(){
		int i=0;
		SWords = new ArrayList<String>(174);
		try{    
			BufferedReader in = new BufferedReader(new FileReader("D:/java/project/src/StopWords.txt"));
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
	//Add a term to stop words map
	public void addStopWord(String k,int docid,int f,int p)
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
	//Get the stem of the term
	public String stem(String word)
	{
		Stemmer s = new Stemmer();
		s.add(word.toCharArray(),word.length());
		s.stem();
        return s.toString();
	}
	//Add a term to the terms map
	public void addToken(String k,int docid,int f,int p)
	{
		//check if stop word return
		if(SWords.contains(k))
		{
			this.addStopWord(k,docid,f,p);
			return;
		}
		//String s = this.stem(k);
		
		StemPost rec = Terms.get(k);
		//New Term
		if(rec == null)
		{
			rec = new StemPost();
			//put stem
			rec.DocMap.put(docid, new Posting(f,p));
			rec.Stem=this.stem(k);
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
	//split string of tokens into separate tokens, handle hyphens, numbers and token length to convert it to a valid term to be put in the database
	public void addTokens(String[] s, int docId,int pos)
	{
		for(int i=0;i<s.length;i++)
		{
			s[i] = s[i].toLowerCase();
			if(s[i].trim().equals(""))
				continue;
			//handling hyphens
			if(s[i].contains("-"))
			{
				//save words with hyphens as separate terms
				String[] dashed = s[i].split("-");
				
				for(int j=0;j<dashed.length;j++)
				{
					if(dashed[j].length()>50 || dashed[j].length()<2 || dashed[j].matches("[0-9]+"))
						continue;
					this.addToken(dashed[j],docId,1,pos);
				}
				//save words with hyphens as one term
				s[i] = s[i].replace("-","");
				if(s[i].length()>50 || s[i].length()<2 || s[i].matches("[0-9]+"))
					continue;
				this.addToken(s[i],docId,1,pos);
			}
			else{
				if(s[i].length()>50 || s[i].length()<2 || s[i].matches("[0-9]+"))
					continue;
				this.addToken(s[i],docId,1,pos);
			}
		}
	}
	
	public void index()
	{
		System.out.println("In Index");
		this.tokenizer();
	}
	//Parsing the document and categorizing them according to their position in the document
	public void tokenizer()
	{
		System.out.println("In tokenizer");
		int j;
		String input;
		Document doc;
		
		for (Map.Entry<Integer, String> entry : Documents.entrySet())
		{
		j = entry.getKey();
		input = entry.getValue();
		doc = Jsoup.parse(input);
		
		//remove links in the text of the page, then remove {.,'} then split on any special char other than letters and numbers	
		String[] titles =Jsoup.parse(input).select("title").text().replaceAll(LinkRegex, "").replaceAll("[.']", "").split( "[^a-zA-Z0-9-]" );
		
		String[] hTags = doc.select("h1, h2, h3, h4, h5, h6, a").text().replaceAll(LinkRegex, "").replaceAll("[.']", "").split("[^a-zA-Z0-9-]");
		
		doc.select("h1, h2, h3, h4, h5, h6, a, title").remove();
	
		String[] others = doc.text().replaceAll(LinkRegex, "").replaceAll("[.']", "").split( "[^a-zA-Z0-9-]" );
		this.addTokens(titles, j, 0);	//Title
		this.addTokens(hTags, j, 1);	//Heading
		this.addTokens(others, j, 2);	//Content
		
		}
		
		//Writing the terms in a file for double checking with the database
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
		   // do something
			System.out.println("writer exception");
		}
		
	}

}
