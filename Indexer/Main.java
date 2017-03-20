import java.util.HashMap;


public class Main {
//handling hyphens
	
	public static void main(String[] args) {
		HashMap<Integer,String> Docs = new HashMap<Integer,String>();
		DB db = new DB();
		Docs=db.docRetrieve();
		Indexer.readStopWords();
		Indexer i = new Indexer(Docs);
		i.tokenizer();
		//db.Filltables(i.getTerms());
		//db.stopWordsFillTables(i.getStopWords());
		System.out.println(i.getTerms().size());
		db.close();
	}
 
}
