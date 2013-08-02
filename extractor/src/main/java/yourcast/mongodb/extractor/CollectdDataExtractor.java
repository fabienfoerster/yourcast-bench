package yourcast.mongodb.extractor;

import com.mongodb.*;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.text.ParseException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: fabien foerster
 * Date: 24/07/13
 * Time: 09:50
 *
*/
public class CollectdDataExtractor {

//    private CollectdQuery[] queries =  {
//            new CollectdQuery("cpu","average",null,"user"),
//            new CollectdQuery("cpu","average",null,"system"),
//            new CollectdQuery("cpu","average",null,"idle"),
//            new CollectdQuery("cpu","average",null,"wait"),
//            new CollectdQuery("cpu","average",null,"interrupt"),
//            new CollectdQuery("cpu","average",null,"softirq"),
//            new CollectdQuery("cpu","average",null,"steal"),
//            new CollectdQuery("cpu","average",null,"nice"),
////            new CollectdQuery("memory",null,null,"used"),
////            new CollectdQuery("memory",null,null,"free"),
////            new CollectdQuery("memory",null,null,"buffered"),
////            new CollectdQuery("memory",null,null,"cached"),
////            new CollectdQuery("interface","lo","if_octets",null),
////            new CollectdQuery("interface","lo","if_errors",null),
////            new CollectdQuery("interface","lo","if_packets",null),
////            new CollectdQuery("interface","eth1","if_octets",null),
////            new CollectdQuery("interface","eth1","if_errors",null),
////            new CollectdQuery("interface","eth1","if_packets",null),
////            new CollectdQuery("mongo","27017",null,"query"),
////            new CollectdQuery("mongo","27017",null,"delete"),
////            new CollectdQuery("mongo","27017",null,"update"),
////            new CollectdQuery("mongo","27017",null,"insert"),
////            new CollectdQuery("mongo","27017-collectd",null,"object_count"),
////            new CollectdQuery("load",null,null,null)
//    };

    private static final String propertieFile = "extractor.properties";
    private String queryFile ;
    private MongoClient mongoClient ;
    private String outputName ;
    private DB db ;
    private long start ;
    private long end ;
    private int col_offset;
    private int row_offset;
    private List<CollectdQuery> queries ;


    public CollectdDataExtractor(String host, String outputName , long start , long end, int serie_number) throws IOException {
        mongoClient = new MongoClient(new ServerAddress(host,27017));
        this.outputName = outputName;
        db = mongoClient.getDB("collectd");
        this.start = start - 7200000 ;
        this.end = end - 7200000;
        loadProperties(propertieFile);
        col_offset += serie_number ;
        queries = new ArrayList<CollectdQuery>();
        loadQueries(CollectdDataExtractor.class.getClassLoader().getResourceAsStream(queryFile));
        ensureIndex();
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

    private Sheet[] createSheets(SXSSFWorkbook wb , CollectdQuery query){
        DBCursor cursor = find(query);
        Sheet[] sheets = new Sheet[1] ;
        if(cursor.hasNext()){
            BasicDBList names = (BasicDBList) cursor.next().get("dsnames");
            if(names.size() > 1){
                sheets = new Sheet[names.size()];
                for(int i = 0 ; i < names.size() ; i++ ){
                    String sheetName = query.getQueryName() + "."+ names.get(i).toString();
                    sheets[i] = getSheet(wb , sheetName);
                }
                return sheets;
            }

        }
        sheets[0] = getSheet(wb , query.getQueryName());
        return sheets;
    }

    private void writeMultipleSheet(Sheet[] sheets , DBCursor cursor){
        Row[] rows = new Row[sheets.length];
        Cell[] cells = new Cell[sheets.length];
        DBObject data ;
        try{
            for(int i = 0 ; cursor.hasNext(); i++){
                data = cursor.next();
                GregorianCalendar cal=new GregorianCalendar();
                cal.setTime((Date)data.get("time"));
                for(int j = 0 ; j < rows.length ; j++){
                    rows[j] = getRow(sheets[j],i+ row_offset);
                    cells[j] = getCell(rows[j],0);
                    cells[j].setCellValue(i);
                    cells[j] = getCell(rows[j], col_offset);
                    BasicDBList values = (BasicDBList) data.get("values");
                    for(int k = 0 ; k < sheets.length ; k++){
                        double value = (Double)values.get(k);
                        cells[j].setCellValue(value);
                    }
                }
            }
        }finally {
            cursor.close();
        }
    }

    private Row getRow(Sheet s , int i){
        Row r = s.getRow(i);
        if(r == null){
            r = s.createRow(i);
        }
        return r;
    }

    private Cell getCell(Row r , int i){
        Cell c = r.getCell(i);
        if(c == null){
            c = r.createCell(i);
        }
        return c;
    }

    private Sheet getSheet(SXSSFWorkbook wb , String name){
        Sheet s = wb.getSheet(name);
        if(s == null){
            s = wb.createSheet(name);
        }
        return s;
    }

//    public void writeToExcel() throws IOException, ParseException {
//
//        FileOutputStream out = new FileOutputStream(this.outputName);
//        SXSSFWorkbook wb = new SXSSFWorkbook(100);
//        DBCursor cursor ;
//        for(int q = 0 ; q < queries.length ; q++ ){
//            cursor = find(queries[q]);
//            Sheet[] sheets = createSheets(wb,queries[q]);
//            writeMultipleSheet(sheets,cursor);
//
//        }
//        wb.write(out);
//        out.close();
//        wb.dispose();
//
//    }

    public void writeToExcel() throws IOException, ParseException, InvalidFormatException {
        String fileToOpen = (new File(this.outputName)).exists() ? this.outputName :"template.xlsx";
        SXSSFWorkbook wb = new SXSSFWorkbook(new XSSFWorkbook(new FileInputStream(fileToOpen)));
        DBCursor cursor ;
        for(CollectdQuery query : queries){
            cursor = find(query);
            Sheet[] sheets = createSheets(wb,query);
            writeMultipleSheet(sheets,cursor);

        }
        FileOutputStream out = new FileOutputStream(this.outputName);
        wb.write(out);
        out.close();
        wb.dispose();


    }



}
