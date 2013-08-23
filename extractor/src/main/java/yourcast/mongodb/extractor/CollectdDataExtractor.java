package yourcast.mongodb.extractor;

import com.mongodb.*;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import java.io.*;
import java.text.ParseException;
import java.util.*;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: fabien foerster
 * Date: 24/07/13
 * Time: 09:50
 *
*/
public class CollectdDataExtractor {

    private static final String PROPERTIE_FILE = "extractor.properties";
    private String queryFile ;
    private MongoClient mongoClient ;
    private String outputName ;
    private DB db ;
    private long start ;
    private long end ;
    private int col_offset;
    private int row_offset;
    private List<CollectdQuery> queries ;
    private List<OverviewSheet> overviewSheets ;
    private CollectdDataWritor writor ;
    private boolean monitoring ;
    private String experimentsTimeFile;

    private CollectdDataExtractor(String host, String outputName, boolean monitoring) throws IOException, InvalidFormatException {
        mongoClient = new MongoClient(new ServerAddress(host,27017));
        this.outputName = outputName;
        db = mongoClient.getDB("collectd");
        queries = new ArrayList<CollectdQuery>();
        overviewSheets = new ArrayList<OverviewSheet>();
        loadProperties(PROPERTIE_FILE);
        loadQueries(CollectdDataExtractor.class.getClassLoader().getResourceAsStream(queryFile));
        ensureIndex();
        this.monitoring = monitoring ;

    }

    public CollectdDataExtractor(String host, String outputName, boolean monitoring, long start, long end) throws IOException, InvalidFormatException {
        this(host,outputName,monitoring);
        this.start = start ;
        this.end = end ;
        writor = new CollectdDataWritorMonitoring(this.outputName,start,end);
    }

    public CollectdDataExtractor(String host,String outputName,boolean monitoring,String experimentsTimeFile) throws IOException, InvalidFormatException {
        this(host,outputName,monitoring);
        this.experimentsTimeFile = experimentsTimeFile ;
        writor = new CollectdDataWritorStress(this.outputName,row_offset,col_offset,overviewSheets);
        System.out.println("Experience time are in : "+experimentsTimeFile);
    }

    private void loadProperties(String propertieFile) throws IOException {
        Properties prop = new Properties();
        prop.load(CollectdDataExtractor.class.getClassLoader().getResourceAsStream(propertieFile));
        row_offset = Integer.parseInt(prop.getProperty("row_offset"));
        col_offset = Integer.parseInt(prop.getProperty("col_offset"));
        queryFile = prop.getProperty("query_file");
        System.out.println("Load properties complete");

    }

    private void loadQueries(InputStream querieStream) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(querieStream));
        String currentLine ;
        OverviewSheet current = null ;
        while((currentLine = in.readLine()) != null){
            if(currentLine.startsWith("%%")){
                current = new OverviewSheet(currentLine.substring(2));
                overviewSheets.add(current);
            }
            else if(!currentLine.startsWith("#")){
                CollectdQuery query = createQuery(currentLine);
                queries.add(query);
                if(current != null){
                    current.addQuery(query.getQueryName());
                }
            }
        }
        System.out.println("Queries created");
    }

    private CollectdQuery createQuery(String query){
        String[] queryParams = query.split("\\.");
        int nbParams = queryParams.length -1;
        String coll = queryParams[0].isEmpty() ? null : queryParams[0];
        String plugin_instance = nbParams < 1 ? null : queryParams[1].isEmpty() ? null : queryParams[1];
        String type = nbParams < 2 ? null : queryParams[2].isEmpty() ? null : queryParams[2];
        String type_instance = nbParams < 3 ? null : queryParams[3].isEmpty() ? null : queryParams[3];
        return new CollectdQuery(coll,plugin_instance,type,type_instance);
    }

    private void ensureIndex(){
        DBCollection coll ;
        for(CollectdQuery query : queries){
            coll = db.getCollection(query.getCollectionName());
            coll.createIndex(new BasicDBObject("time", 1));
        }
    }

    private DBCursor find(CollectdQuery collectdQuery){
        DBCollection coll = db.getCollection(collectdQuery.getCollectionName());
        BasicDBObject query = collectdQuery.buildQuery(this.start, this.end);
        System.out.println("Find : "+query.toString());
        return coll.find(query);

    }

    private boolean setStartEnd(BufferedReader r) throws IOException {
        String currentLine  ;
        if((currentLine = r.readLine()) != null){
            String result[] = currentLine.split("-");
            start = Long.parseLong(result[0]);
            end = Long.parseLong(result[1]);
            col_offset++;
            System.out.println("For experience #"+col_offset);
            return true;
        }
        return false ;
    }

    public void writeToExcel() throws IOException, ParseException, InvalidFormatException {
        DBCursor cursor ;
        writor.open();
        if(!monitoring){
            System.out.println("Stress simulation mode");
            for(CollectdQuery query : queries){
                BufferedReader reader = new BufferedReader(new FileReader(experimentsTimeFile));
                System.out.println("Writing : "+query.getQueryName());
                while(setStartEnd(reader)){
                    cursor = find(query);
                    writor.addCursor(query.getQueryName()+"#"+col_offset,cursor);
                }
                writor.writeToExcel();
                writor.resetCursor();
                col_offset = 0 ;
            }
            ((CollectdDataWritorStress)writor).writeOverviewSheet();
        }
        else {
            System.out.println("Monitoring mode");
            for(CollectdQuery query : queries){
                cursor = find(query);
                writor.addCursor(query.getQueryName(), cursor);
            }
            writor.writeToExcel();
        }
        writor.close();
    }

}
