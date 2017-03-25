import java.awt.Toolkit;
import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
 

public class DB implements Runnable{
	public static Connection conn;
	private static String URL = "jdbc:mysql://localhost/search_engine?useSSL=false";
	private static String USER = "root";
	private static String PASS = "root";
	private static Statement stmt;
	HashMap<String, HashMap<Integer,Posting>> StopWords;
	HashMap<String, StemPost> terms;
	public DB(){
		try{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			
			conn = DriverManager.getConnection(URL, USER, PASS);
			
			
			}
			catch(ClassNotFoundException ex) {
				   System.out.println("Error: unable to load driver class!");
				   System.exit(1);
			}
				catch(IllegalAccessException ex) {
				   System.out.println("Error: access problem while loading!");
				   System.exit(2);
				}
				catch(InstantiationException ex) {
				   System.out.println("Error: unable to instantiate driver!");
				   System.exit(3);
				} 
			catch (SQLException e) {
				 System.out.println("Error: sql error");
					e.printStackTrace();
				}
	}
	
	public DB(HashMap<String, StemPost> term,HashMap<String, HashMap<Integer,Posting>> stopwords){
		this.StopWords=stopwords;
		this.terms=term;
		try{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			
			conn = DriverManager.getConnection(URL, USER, PASS);
			
			
			}
			catch(ClassNotFoundException ex) {
				   System.out.println("Error: unable to load driver class!");
				   System.exit(1);
			}
				catch(IllegalAccessException ex) {
				   System.out.println("Error: access problem while loading!");
				   System.exit(2);
				}
				catch(InstantiationException ex) {
				   System.out.println("Error: unable to instantiate driver!");
				   System.exit(3);
				} 
			catch (SQLException e) {
				 System.out.println("Error: sql error");
					e.printStackTrace();
				}
			
	}
	public ResultSet execute(String Statement) throws SQLException{
		stmt=conn.createStatement();
		ResultSet r= stmt.executeQuery(Statement);
		return r;
	}
	public void run(){
		if(Thread.currentThread().getName().equals("term"))
			newFilltables(terms);
		else if(Thread.currentThread().getName().equals("stop"))
			stopWordsFillTables(StopWords);
		
	}
	public static synchronized int exe(String Statement) throws SQLException{
	
			stmt = conn.createStatement();
			//execute sql query
			int rows = stmt.executeUpdate(Statement); 
			return rows;

	}
	private ResultSet genKeyexe(String St) throws SQLException{

			stmt = conn.createStatement();
			//execute sql query
			
			 int rows=stmt.executeUpdate(St,stmt.RETURN_GENERATED_KEYS);
			 ResultSet rs=stmt.getGeneratedKeys();
	        //ResultSet rs=stmt.executeQuery("select * from employee");
	        //process the result
	       /* while(rs.next())
	        {
	            System.out.println(rs.getString("name")+" = "+rs.getString(1));
	        }     
	        */     
			return rs;

	}
	private void Filltables (HashMap<String, StemPost> m) 
	{	
		
		//query.append("stem");
		Iterator it = m.entrySet().iterator();
		//StringBuilder termsQ=new StringBuilder();
		//termsQ.append("INSERT INTO terms (term,stem,df) VALUES (?,?,?)");
		
		PreparedStatement statm = null,statm2=null;
		try {
			statm = conn.prepareStatement("INSERT INTO terms2 (term,stem,df) VALUES (?,?,?)");
			statm2 = conn.prepareStatement("INSERT INTO term_doc2 (term,doc_id,tf,location) VALUES (?,?,?,?)");
			Toolkit.getDefaultToolkit().beep();
			System.out.println("inside terms2");
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		while (it.hasNext()) 
		{
			
			Map.Entry pairs = (Map.Entry)it.next();
			String term= (String)pairs.getKey();
			long df= ((StemPost)pairs.getValue()).DocMap.size();
			String stem=((StemPost)pairs.getValue()).Stem;
			//fill terms
			
			//termsQ.append("\""+term+"\""+", \""+stem+"\", "+df+")");
			//long termID=-1;

				ResultSet r1;
				try {
					statm.setString(1, term);
					statm.setString(2,stem);
					statm.setLong(3, df);
					//r1 = genKeyexe(termsQ.toString());
					//while (r1.next())
						//termID = r1.getLong(1);
					statm.addBatch();
				} catch (SQLException e) {
					e.printStackTrace();
				}

			//loop to fill term_doc
		    HashMap<Integer,Posting> hm=((StemPost)pairs.getValue()).DocMap;
			Iterator it2 = hm.entrySet().iterator();
			
			
			while (it2.hasNext()) 
			{
				Map.Entry pairs2 = (Map.Entry)it2.next();
				//StringBuilder tdQ=new StringBuilder();
				//tdQ.append("INSERT INTO term_doc (term,doc_id,tf,location) VALUES (");
				int doc= (int)pairs2.getKey();
				int tf=((Posting)pairs2.getValue()).Tf;
				int location=((Posting)pairs2.getValue()).Pos;
				//tdQ.append(term+", "+doc+", "+tf+", "+location+")");
				
				try {
					statm2.setString(1, term);
					statm2.setInt(2, doc);
					statm2.setInt(3, tf);
					statm2.setInt(4, location);
					//int rows=exe(tdQ.toString());
					statm2.addBatch();
					
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
	    }
		try{
		statm.executeBatch();
		statm2.executeBatch();}
		 catch (SQLException e) {
				
				e.printStackTrace();
			}
		System.out.println("terms2 Inserted");
	}
	
	private static void newFilltables(HashMap<String, StemPost> m)
	{
		//function to insert the hashmap into terms and term_doc tables
		
		
				//query.append("stem");
				Iterator it = m.entrySet().iterator();
				
				StringBuilder termsQ=new StringBuilder();
				termsQ.append("INSERT INTO terms (term,stem,df) VALUES ");
				
				StringBuilder tdQ=new StringBuilder();
				tdQ.append("INSERT INTO term_doc (term,doc_id,tf,location) VALUES ");
				
				
				boolean firstTime=false;
				boolean firstTime2=false;
				Map.Entry pairs,pairs2;
				String term,stem;
				long df,doc;
				int tf,location;
				
				
				//loop hashmap
				while (it.hasNext()) 
				{
					 pairs= (Map.Entry)it.next();
					 term= (String)pairs.getKey();
					 df= ((StemPost)pairs.getValue()).DocMap.size();
					 stem=((StemPost)pairs.getValue()).Stem;
					 if(term.matches(".*\\d.*")&&df<5) //if contains numbers and in less than 5 docs
						 continue;
					
					if(!firstTime)
						firstTime=true;
					else termsQ.append(",");
					
					termsQ.append("(\""+term+"\""+", \""+stem+"\", "+df+")");

					//loop to fill term_doc
				    HashMap<Integer,Posting> hm=((StemPost)pairs.getValue()).DocMap;
					Iterator it2 = hm.entrySet().iterator();
					while (it2.hasNext()) 
					{
						pairs2 = (Map.Entry)it2.next();
						 doc= (int)pairs2.getKey();
						 tf=((Posting)pairs2.getValue()).Tf;
						 location=((Posting)pairs2.getValue()).Pos;
						if(!firstTime2)
							firstTime2=true;
						else tdQ.append(",");
						tdQ.append("(\""+term+"\", "+doc+", "+tf+", "+location+")");
						
					}
					
			    }
				int r1;
				try {
					//fill terms table
					System.out.println("terms string made");
					IndexerMain.printTime();
					r1 = exe(termsQ.toString());
					Toolkit.getDefaultToolkit().beep();
					System.out.println("terms inserted"+r1);
					IndexerMain.printTime();
					//while (r1.next())
					//	termID = r1.getLong(1);
				} catch (SQLException e) {
					e.printStackTrace();
				} 
				try {
					//fill term_doc table
					System.out.println("term_doc string made");
					IndexerMain.printTime();
					int rows=exe(tdQ.toString());
					Toolkit.getDefaultToolkit().beep();
					System.out.println("term_doc inserted"+rows);
					IndexerMain.printTime();
					
				} catch (SQLException e) {
					
					e.printStackTrace();
				} 
				Toolkit.getDefaultToolkit().beep();
				System.out.println("terms Inserted");
				IndexerMain.printTime();
	}

	private static void stopWordsFillTables(HashMap<String, HashMap<Integer,Posting>> StopWords)
	{
		//function to fill stop_words and stop_doc tables
		Iterator it = StopWords.entrySet().iterator();
		StringBuilder stQ=new StringBuilder();
		stQ.append("INSERT INTO stop_words (stop_word,df) VALUES ");
		StringBuilder stdocQ=new StringBuilder();
		stdocQ.append("INSERT INTO stop_doc (doc_id,stop_word,tf,location) VALUES ");
		boolean firstTime=false;
		boolean firstTime2=false;
		Map.Entry pairs,pairs2;
		String stw;
		long df,doc;
		int tf,location;
		//loop stop_words
		while (it.hasNext()) 
		{
			pairs = (Map.Entry)it.next();
			stw= (String)pairs.getKey();
			HashMap<Integer,Posting> m=(HashMap)pairs.getValue();
			df=m.size();
			if(!firstTime)
				firstTime=true;
			else stQ.append(",");
			
			stQ.append("(\""+stw+"\", "+ df+")");

				
				
			//loop to fill stop_doc

			Iterator it2 = m.entrySet().iterator();
			while (it2.hasNext()) 
			{
				
				 pairs2 = (Map.Entry)it2.next();
				 doc= (int)pairs2.getKey();
				 tf=((Posting)pairs2.getValue()).Tf;
				 location=((Posting)pairs2.getValue()).Pos;
				if(!firstTime2)
					firstTime2=true;
				else stdocQ.append(",");
				stdocQ.append("("+doc+", \""+stw+"\", "+tf+", "+location+")");
				//System.out.print(stdocQ.toString());
			}
			
	    }
		try {
			int r = exe(stQ.toString());
			Toolkit.getDefaultToolkit().beep();
			System.out.println("stop words inserted"+r);
			IndexerMain.printTime();
			} catch (SQLException e) {
				
					e.printStackTrace();
			}
		
		try {
			int rows2=exe(stdocQ.toString());
			Toolkit.getDefaultToolkit().beep();
			System.out.println("stop docs inserted"+rows2);
			IndexerMain.printTime();
		} catch (SQLException e) {
			
			e.printStackTrace();
		}
		System.out.println("stop words inserted");
		IndexerMain.printTime();
	}
	public void close(){
		//close database connection
		try {
			conn.close();
		} catch (SQLException e) {
			
			e.printStackTrace();
		}
	}
	
	public void ClearDB() {
		//function to empty tables
		String[] Queries=new String[4];
		Queries[0]="truncate stop_doc";
		Queries[1]="truncate term_doc";
		Queries[2]="truncate stop_words";
		Queries[3]="truncate terms";
		//Queries[4]="delete from documents";
		for(int i=0;i<4;i++)
		{try {
			int rows=exe(Queries[i]);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}
	}
	
	public HashMap<Integer,String> docRetrieve(){
		//function to  get documents from database
		HashMap<Integer,String>temp=new HashMap<Integer,String>();
		try {
			ResultSet r=execute("Select * from documents");
			while(r.next())
			{
			 temp.put(r.getInt("id"), r.getString("content"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		return temp;
	}


	public ResultSet runSql(String sql) throws SQLException {
		Statement sta = conn.createStatement();
		return sta.executeQuery(sql);
	}

	public boolean runSql2(String sql) throws SQLException {
		Statement sta = conn.createStatement();
		return sta.execute(sql);
	}

	@Override
	protected void finalize() throws Throwable {
		if (conn != null || !conn.isClosed()) {
			conn.close();
		}
	}

	
}

