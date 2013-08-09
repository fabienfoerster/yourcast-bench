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
    private CollectdDataWritor writor ;


    public CollectdDataExtractor(String host, String outputName, long start, long end, int serie_number , boolean monitoring) throws IOException, InvalidFormatException {
        mongoClient = new MongoClient(new ServerAddress(host,27017));
        this.outputName = outputName;
        db = mongoClient.getDB("collectd");
        this.start = start ;
        this.end = end ;
        queries = new ArrayList<CollectdQuery>();
        loadProperties(PROPERTIE_FILE);
        col_offset += serie_number ;
        loadQueries(CollectdDataExtractor.class.getClassLoader().getResourceAsStream(queryFile));
        ensureIndex();
        writor = monitoring ? new CollectdDataWritorMonitoring(this.outputName) :new CollectdDataWritorStress(this.outputName,row_offset,col_offset);
    }

    private void loadProperties(String propertieFile) throws IOException {
        Properties prop = new Properties();
        prop.load(CollectdDataExtractor.class.getClassLoader().getResourceAsStream(propertieFile));
        row_offset = Integer.parseInt(prop.getProperty("row_offset"));
        col_offset = Integer.parseInt(prop.getProperty("col_offset"));
        queryFile = prop.getProperty("query_file");

    }

    private void loadQueries(InputStream querieStream) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(querieStream));
        String currentLine ;
        while((currentLine = in.readLine()) != null){
            if(!currentLine.startsWith("#")){
                createQuery(currentLine);
            }
        }
    }

    private void createQuery(String query){
        String[] queryParams = query.split("\\.");
        String coll = queryParams[0].isEmpty() ? null : queryParams[0];
        String plugin_instance = queryParams[1].isEmpty() ? null : queryParams[1];
        String type = queryParams[2].isEmpty() ? null : queryParams[2];
        String type_instance = queryParams[3].isEmpty() ? null : queryParams[3];
        queries.add(new CollectdQuery(coll,plugin_instance,type,type_instance));
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
        return coll.find(query);

    }

    public void writeToExcel() throws IOException, ParseException, InvalidFormatException {
        DBCursor cursor ;
        writor.open();
        for(CollectdQuery query : queries){
            cursor = find(query);
            writor.writeToExcel(cursor,query);

        }
        writor.close();

    }

}
