import java.awt.Toolkit;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;



 


public class Main {
//handling hyphens
	
	
	public static void main(String[] args) {
		
		System.out.println("starting");
		printTime();
		HashMap<Integer,String> Docs = new HashMap<Integer,String>();
		DB db = new DB();
		//db.ClearDB();
		Docs=db.docRetrieve();
		Toolkit.getDefaultToolkit().beep();
		System.out.println("docs retrieved");
		printTime();
		Indexer.readStopWords();
		Indexer i = new Indexer(Docs);
		i.tokenizer();
		Toolkit.getDefaultToolkit().beep();
		System.out.println("indexer done");
		printTime();
		Thread t0 = Thread.currentThread();
		
        Thread t1 = new Thread(new DB(i.getTerms(),i.getStopWords()));
        Thread t2 = new Thread(new DB(i.getTerms(),i.getStopWords()));
   

        t0.setName("Main Thread");
        t1.setName("stop");
        t2.setName("term");
        
        t1.start();
        t2.start();
      
     
        try {
			t1.join();
			t2.join(); 
		
		} catch (InterruptedException e) {
			e.printStackTrace();
		} 
        
		System.out.println(i.getTerms().size()+" i am done");
		printTime();
		db.close();
		
	}
	public static void printTime(){
		Calendar cal = Calendar.getInstance();
	    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
	    System.out.println( sdf.format(cal.getTime()) );
		}
 
}
