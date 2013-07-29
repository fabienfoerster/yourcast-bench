package yourcast.mongodb.extractor;

import com.mongodb.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created with IntelliJ IDEA.
 * User: binou
 * Date: 24/07/13
 * Time: 09:50
 * To change this template use File | Settings | File Templates.
 */
public class CollectdDataExtractor {

    private CollectdQuery[] queries =  {
            new CollectdQuery("cpu","0",null,"user"),
            new CollectdQuery("cpu","0",null,"system"),
            new CollectdQuery("cpu","0",null,"idle"),
            new CollectdQuery("cpu","0",null,"wait"),
            new CollectdQuery("cpu","0",null,"interrupt"),
            new CollectdQuery("cpu","0",null,"softirq"),
            new CollectdQuery("cpu","0",null,"steal"),
            new CollectdQuery("cpu","0",null,"nice"),
            new CollectdQuery("memory",null,null,"used"),
            new CollectdQuery("memory",null,null,"free"),
            new CollectdQuery("memory",null,null,"buffered"),
            new CollectdQuery("memory",null,null,"cached"),
            new CollectdQuery("interface","lo","if_octets",null),
            new CollectdQuery("interface","lo","if_errors",null),
            new CollectdQuery("interface","lo","if_packets",null),
            new CollectdQuery("interface","eth1","if_octets",null),
            new CollectdQuery("interface","eth1","if_errors",null),
            new CollectdQuery("interface","eth1","if_packets",null),
            new CollectdQuery("GenericJMX","memory_pool-",null,"committed"),
            new CollectdQuery("GenericJMX","memory_pool-",null,"init"),
            new CollectdQuery("GenericJMX","memory_pool-",null,"max"),
            new CollectdQuery("GenericJMX","memory_pool-",null,"used"),
            new CollectdQuery("GenericJMX","memory-heap",null,"committed"),
            new CollectdQuery("GenericJMX","memory-heap",null,"init"),
            new CollectdQuery("GenericJMX","memory-heap",null,"max"),
            new CollectdQuery("GenericJMX","memory-heap",null,"used"),
            new CollectdQuery("GenericJMX","memory-nonheap",null,"committed"),
            new CollectdQuery("GenericJMX","memory-nonheap",null,"init"),
            new CollectdQuery("GenericJMX","memory-nonheap",null,"max"),
            new CollectdQuery("GenericJMX","memory-nonheap",null,"used"),
            new CollectdQuery("GenericJMX",null,null,"loaded_classes"),
            new CollectdQuery("GenericJMX","gc-","invocations",null),
            new CollectdQuery("GenericJMX","gc-",null,"collection_time"),
            new CollectdQuery("mongo","27017",null,"query"),
            new CollectdQuery("mongo","27017",null,"delete"),
            new CollectdQuery("mongo","27017",null,"update"),
            new CollectdQuery("mongo","27017",null,"insert"),
            new CollectdQuery("mongo","27017-collectd",null,"object_count"),
            new CollectdQuery("load",null,null,null)
    };

    private MongoClient mongoClient ;
    private String outputName ;
    private DB db ;
    private long start ;
    private long end ;


    public CollectdDataExtractor(String host, String outputName , long start , long end) throws UnknownHostException {
        mongoClient = new MongoClient(host);
        this.outputName = outputName;
        db = mongoClient.getDB("collectd");
        this.start = start ;
        this.end = end ;
    }

    private DBCursor find(CollectdQuery collectdQuery){
        DBCollection coll = db.getCollection(collectdQuery.getCollectionName());
        coll.createIndex(new BasicDBObject("time",new Integer(1)));
        BasicDBObject query = collectdQuery.buildQuery(this.start,this.end);
        DBCursor cursor = coll.find(query);
        return cursor ;

    }

    private Sheet[] createSheets(SXSSFWorkbook wb , CollectdQuery query){
        DBCursor cursor = find(query);
        Sheet[] sheets = new Sheet[1] ;
        if(cursor.hasNext()){
            BasicDBObject names = (BasicDBObject) cursor.next().get("dsnames");
            if(names.size() > 1){
                sheets = new Sheet[names.size()];
                for(int i = 0 ; i < names.size() ; i++ ){
                    sheets[i] = wb.createSheet(query.queryName()+names.get(i).toString());
                }
            }
            return sheets;
        }
        sheets[0] = wb.createSheet(query.queryName());
        return sheets;
    }

    public void writeToExcel() throws IOException, ParseException {
        FileOutputStream out = new FileOutputStream(this.outputName);
        SXSSFWorkbook wb = new SXSSFWorkbook(100);
        DBCursor cursor ;
        for(int q = 0 ; q < queries.length ; q++ ){
            cursor = find(queries[q]);
            Sheet s = wb.createSheet();
            wb.setSheetName(q, queries[q].queryName());
            Row r ;
            Cell c ;
            try {
                for(int i = 0 ; cursor.hasNext(); i++ ){
                    DBObject data = cursor.next();
                    r = s.createRow(i);
                    c = r.createCell(0);

                    GregorianCalendar cal=new GregorianCalendar();
                    cal.setTime((Date)data.get("time"));
                    c.setCellValue(cal.getTimeInMillis()/1000);
                    BasicDBList values = (BasicDBList) data.get("values");
                    for(int j = 0 ; j < values.size(); j++){
                         c = r.createCell(j+1);
                         c.setCellValue(values.get(j).toString());
                    }



                }
            }finally {
                cursor.close();
            }
        }
        wb.write(out);
        out.close();
        wb.dispose();

    }
}
