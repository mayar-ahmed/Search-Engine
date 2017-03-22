import java.io.*;
import java.sql.*;
import java.util.*;


public class DB {
	private Connection conn;
	private String URL = "jdbc:mysql://localhost/search_engine?useSSL=false";
	private String USER = "root";
	private String PASS = "";
	private Statement stmt;
	
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
	public ResultSet execute(String Statement) throws SQLException{
		stmt=conn.createStatement();
		ResultSet r= stmt.executeQuery(Statement);
		return r;
		
	}
	public int exe(String Statement) throws SQLException{
	
			stmt = conn.createStatement();
			//execute sql query

			int rows = stmt.executeUpdate(Statement);
			
	        //ResultSet rs=stmt.executeQuery("select * from employee");
	        //process the result
	       /* while(rs.next())
	        {
	            System.out.println(rs.getString("name")+" = "+rs.getString(1));
	        }     
	        */     
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
	public void Filltables (HashMap<String, StemPost> m) 
	{	
		
		//query.append("stem");
		Iterator it = m.entrySet().iterator();
		//loop hashmap
		
		while (it.hasNext()) 
		{
			
			StringBuilder termsQ=new StringBuilder();
			
			
			termsQ.append("INSERT INTO terms (term,stem,df) VALUES (");
			
			
			
			Map.Entry pairs = (Map.Entry)it.next();
			String term= (String)pairs.getKey();
			long df= ((StemPost)pairs.getValue()).DocMap.size();
			String stem=((StemPost)pairs.getValue()).Stem;
			
			//fill terms
			
			termsQ.append("\""+term+"\""+", \""+stem+"\", "+df+")");
			long termID=-1;

				ResultSet r1;
				try {
					r1 = genKeyexe(termsQ.toString());
					while (r1.next())
						termID = r1.getLong(1);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			
			
			
			//loop to fill term_doc
		    HashMap<Integer,Posting> hm=((StemPost)pairs.getValue()).DocMap;
			Iterator it2 = hm.entrySet().iterator();
			while (it2.hasNext()) 
			{
				Map.Entry pairs2 = (Map.Entry)it2.next();
				StringBuilder tdQ=new StringBuilder();
				tdQ.append("INSERT INTO term_doc (term_id,doc_id,tf,location) VALUES (");
				long doc= (int)pairs2.getKey();
				int tf=((Posting)pairs2.getValue()).Tf;
				String location=((Posting)pairs2.getValue()).Pos;
				tdQ.append(termID+", "+doc+", "+tf+", \'"+PosToB(location)+"\')");
				
				try {
					int rows=exe(tdQ.toString());
					
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
	    }
		System.out.println("terms Inserted");
	}
	
	public void newFilltables(HashMap<String, StemPost> m)
	{
		//query.append("stem");
				Iterator it = m.entrySet().iterator();
				//loop hashmap
				StringBuilder termsQ=new StringBuilder();
				termsQ.append("INSERT INTO terms (term,stem,df) VALUES ");
				
				StringBuilder tdQ=new StringBuilder();
				tdQ.append("INSERT INTO term_doc (term,doc_id,tf,location) VALUES ");
				
				
				boolean firstTime=false;
				boolean firstTime2=false;
				while (it.hasNext()) 
				{

					
					Map.Entry pairs = (Map.Entry)it.next();
					String term= (String)pairs.getKey();
					long df= ((StemPost)pairs.getValue()).DocMap.size();
					String stem=((StemPost)pairs.getValue()).Stem;
					
					//fill terms
					if(!firstTime)
						firstTime=true;
					else termsQ.append(",");
					
					termsQ.append("(\""+term+"\""+", \""+stem+"\", "+df+")");

					//loop to fill term_doc
				    HashMap<Integer,Posting> hm=((StemPost)pairs.getValue()).DocMap;
					Iterator it2 = hm.entrySet().iterator();
					while (it2.hasNext()) 
					{
						Map.Entry pairs2 = (Map.Entry)it2.next();
						long doc= (int)pairs2.getKey();
						int tf=((Posting)pairs2.getValue()).Tf;
						String location=((Posting)pairs2.getValue()).Pos;
						if(!firstTime2)
							firstTime2=true;
						else tdQ.append(",");
						tdQ.append("(\""+term+"\", "+doc+", "+tf+", \'"+PosToB(location)+"\')");
						
					}
					
			    }
				int r1;
				try {
					PrintWriter writer = new PrintWriter("query.txt", "UTF-8");
				    writer.println(termsQ.toString());
				    writer.close();
					r1 = exe(termsQ.toString());
					//while (r1.next())
					//	termID = r1.getLong(1);
				} catch (SQLException e) {
					e.printStackTrace();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					PrintWriter writer = new PrintWriter("query2.txt", "UTF-8");
				    writer.println(tdQ.toString());
				    writer.close();
					int rows=exe(tdQ.toString());
					
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("terms Inserted");	
	}
	
	private char PosToB(String pos){
		//only one position		
		char temp='0';
			if(pos.equals("Title"))
				temp='1';
			else if(pos.equals("Heading"))
				temp='2';
			else if(pos.equals("Content"))
				temp='3';
		return temp;
	}
	public void stopWordsFillTables(HashMap<String, HashMap<Integer,Posting>> StopWords){
		
		Iterator it = StopWords.entrySet().iterator();
		StringBuilder stQ=new StringBuilder();
		stQ.append("INSERT INTO stop_words (stop_word,df) VALUES ");
		StringBuilder stdocQ=new StringBuilder();
		stdocQ.append("INSERT INTO stop_doc (doc_id,stop_word,tf,location) VALUES ");
		boolean firstTime=false;
		boolean firstTime2=false;
		while (it.hasNext()) 
		{
			Map.Entry pairs = (Map.Entry)it.next();
			String stw= (String)pairs.getKey();
			HashMap<Integer,Posting> m=(HashMap)pairs.getValue();
			long df=m.size();
			if(!firstTime)
				firstTime=true;
			else stQ.append(",");
			//fill stop_words
			stQ.append("(\""+stw+"\", "+ df+")");

				
				
			//loop to fill stop_doc

			Iterator it2 = m.entrySet().iterator();
			while (it2.hasNext()) 
			{
				
				Map.Entry pairs2 = (Map.Entry)it2.next();
				long doc= (int)pairs2.getKey();
				int tf=((Posting)pairs2.getValue()).Tf;
				String location=((Posting)pairs2.getValue()).Pos;
				if(!firstTime2)
					firstTime2=true;
				else stdocQ.append(",");
				stdocQ.append("("+doc+", \""+stw+"\", "+tf+", \'"+PosToB(location)+"\')");
				//System.out.print(stdocQ.toString());
			}
			
	    }
		try {
			int r = exe(stQ.toString());

			} catch (SQLException e) {
				
					e.printStackTrace();
			}
		
		try {
			int rows2=exe(stdocQ.toString());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("stop words inserted");
	}
	public void close(){
		try {
			conn.close();
		} catch (SQLException e) {
			
			e.printStackTrace();
		}
	}
	
	public void ClearDB() {
		String[] Queries=new String[4];
		Queries[0]="delete from stop_doc";
		Queries[1]="delete from term_doc";
		Queries[2]="delete from stop_words";
		Queries[3]="delete from terms";
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
		String Q="Select * from documents";
		HashMap<Integer,String>temp=new HashMap<Integer,String>();
		try {
			ResultSet r=execute(Q);
			int t;
			String ts;
			 PrintWriter writer = new PrintWriter("W3COut.txt", "UTF-8");
			while(r.next())
			{
			 t=r.getInt("id");
			 ts=r.getString("content");
			 writer.print(ts);
			 temp.put(t, ts);
			}
			writer.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	    
		return temp;
	}
	
}
