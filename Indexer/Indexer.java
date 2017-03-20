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
	private static ArrayList<String> SWords;
	private HashMap<String, StemPost> Tokens;
	private HashMap<String, HashMap<Integer,Posting>> StopWords;
	private HashMap<Integer,String> Documents;
	
	public Indexer(HashMap<Integer,String> docs)
	{
	Tokens = new HashMap<String, StemPost>();
	Documents = docs;
	StopWords = new HashMap<String, HashMap<Integer,Posting>>();
	//Stems = new HashSet<String>();
	}
	public HashMap<String,StemPost> getTerms()
	{
		return Tokens;
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
		  /*
		  for(int j=0;j<SWords.size();j++)
		  {
			 System.out.println(SWords.get(j));
		  }
		  */
		  
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
	public void addToken(String k,int docid,int f,String p)
	{
		//check if stop word return
		if(SWords.contains(k))
		{
			this.addStopWord(k,docid,f,p);
		//	System.out.printf("%s -> stopword\n",k);
			return;
		}
		
		/*if(p == "Title")
			System.out.println(k);
			*/
		String s = this.stem(k);
		
		StemPost rec = Tokens.get(k);
		//New Term
		if(rec == null)
		{
			rec = new StemPost();
			//put stem
			rec.DocMap.put(docid, new Posting(f,p));
			rec.Stem=s;
	        Tokens.put(k,rec);
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
			 
			//list.add(new Posting(docid,f,p));
		}
	}
	public void addTokens(String[] s, int docId,String pos)
	{
		//String temp;
		for(int i=0;i<s.length;i++)
		{
			//temp = s[i].toLowerCase();
			s[i] = s[i].replaceAll("[^a-zA-Z]", "").toLowerCase();
			if(s[i].trim().equals(""))
				continue;
			this.addToken(s[i],docId,1,pos);
		}
	}
	public void tokenizer()
	{
		for (Map.Entry<Integer, String> entry : Documents.entrySet())
		{
			int j = entry.getKey();
		String input = entry.getValue();
		Document doc = Jsoup.parse(input);
		//String link = doc.select("a").text();
		String title = doc.select("title").text();
		String[] titles = title.split( " " );
		String hTag = doc.select("h1, h2, h3, h4, h5, h6, a").text();
		String[] hTags = hTag.split( " " );
		doc.select("h1, h2, h3, h4, h5, h6, a, title").remove();
		String other = doc.body().text();
		String[] others = other.split( " " );
		
		this.addTokens(titles, j, "Title");
		this.addTokens(hTags, j, "Heading");
		this.addTokens(others, j, "Content");
		
		}
		
		
		try{
		    PrintWriter writer = new PrintWriter("out.txt");
		    writer.println(Tokens.size());
			for (Map.Entry<String, StemPost> entry : Tokens.entrySet())
			{
				//System.out.printf("Term: %s Stem: %s \n",entry.getKey(),entry.getValue().Stem);
				
				writer.printf("Term:%s Stem:%s, Df: %d\n",entry.getKey(),entry.getValue().Stem,entry.getValue().DocMap.size());
			    
			    for(Map.Entry<Integer,Posting> entry2 : (entry.getValue()).DocMap.entrySet())
			    {
			    	writer.println(entry2.getKey() + "/" + entry2.getValue().Tf + "/" + entry2.getValue().Pos);
			    }
			    
			}
		    writer.close();
		} catch (IOException e) {
		   // do something
		}
		
		
		/*
		System.out.println(Tokens.size());
		for (Map.Entry<String, StemPost> entry : Tokens.entrySet())
		{
			//System.out.printf("Term: %s Stem: %s \n",entry.getKey(),entry.getValue().Stem);
			
		    System.out.printf("Term: %s Stem: %s, Df: %d\n",entry.getKey(),entry.getValue().Stem,entry.getValue().DocMap.size());
		    
		    for(Map.Entry<Integer,Posting> entry2 : (entry.getValue()).DocMap.entrySet())
		    {
		    	System.out.println(entry2.getKey() + "/" + entry2.getValue().Tf + "/" + entry2.getValue().Pos);
		    }
		    
		}
		*/
	}

}
