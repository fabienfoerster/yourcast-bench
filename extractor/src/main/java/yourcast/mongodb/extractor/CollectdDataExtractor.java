package yourcast.mongodb.extractor;

import com.mongodb.*;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.*;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
    private SXSSFWorkbook wb ;
    private XSSFWorkbook xssfWorkbook ;
    private CollectdDataWritor writor ;
    private XSSFCellStyle cellStyle ;

    public CollectdDataExtractor(String host, String outputName, long start, long end, int serie_number) throws IOException {
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
        File f = new File(this.outputName);
        if(f.exists()){
            xssfWorkbook = new XSSFWorkbook(new FileInputStream(this.outputName));
            wb = new SXSSFWorkbook(xssfWorkbook);
        } else {
            wb = new SXSSFWorkbook(100);
            xssfWorkbook = wb.getXSSFWorkbook();
        }
        writor = new CollectdDataWritorStress(wb,outputName);
        createCellStyle();

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
        int row_write = 0;
        for(CollectdQuery query : queries){
            cursor = find(query);
            Sheet[] sheets = createSheets(wb,query);
            row_write =  writeMultipleSheet(sheets,cursor);

        }
        for(int i = 0 ; i < xssfWorkbook.getNumberOfSheets() ; i++){
            createFormulas(xssfWorkbook.getSheetAt(i),row_write);
            setDefaultText(xssfWorkbook.getSheetAt(i));
        }
        FileOutputStream out = new FileOutputStream(this.outputName);
        wb.write(out);
        out.close();
        wb.dispose();


    }

    private int  writeMultipleSheet(Sheet[] sheets , DBCursor cursor){
        Row[] rows = new Row[sheets.length];
        Cell[] cells = new Cell[sheets.length];
        DBObject data ;
        int i  = 0 ;
        try{
            for(i = 0 ; cursor.hasNext(); i++){
                data = cursor.next();
                GregorianCalendar cal =new GregorianCalendar();
                cal.setTime((Date)data.get("time"));
                for(int j = 0 ; j < rows.length ; j++){
                    rows[j] = getRow(sheets[j],i+ row_offset);
                    cells[j] = getCell(rows[j],0);
                    cells[j].setCellValue(i);
                    cells[j].setCellStyle(cellStyle);
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
            return i ;
        }
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



    private void createCellStyle(){
        cellStyle = xssfWorkbook.createCellStyle();
        Font f = xssfWorkbook.createFont();
        f.setBoldweight(Font.BOLDWEIGHT_BOLD);
        cellStyle.setFont(f);
        cellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
        cellStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(237, 237, 237)));
    }


    protected Row getRow(Sheet s , int i){
        Row r = s.getRow(i);
        if(r == null){
            r = s.createRow(i);
        }
        return r;
    }

    protected Cell getCell(Row r, int i){
        Cell c = r.getCell(i);
        if(c == null){
            c = r.createCell(i);
        }
        return c;
    }

    protected XSSFCell getXSSFCell(XSSFRow r, int i){
        XSSFCell c = r.getCell(i);
        if(c == null){
            c = r.createCell(i);
        }
        return c ;
    }

    protected XSSFRow getXSSFRow(XSSFSheet s, int i){
        XSSFRow r = s.getRow(i);
        if(r == null){
            r = s.createRow(i);
        }
        return r ;
    }

    protected Sheet getSheet(SXSSFWorkbook wb, String name){
        Sheet s = wb.getSheet(name);
        if(s == null){
            s = wb.createSheet(name);
        }
        return s;
    }

    private void createFormulas(XSSFSheet s , int row_count ){
        XSSFCell min , max , average ,stdev ;
        for(int i = 1+row_offset ; i <= row_count+row_offset ; i++){
            min = getXSSFCell(getXSSFRow(s,i-1),11);
            min.setCellType(Cell.CELL_TYPE_FORMULA);
            min.setCellFormula("MIN(B"+i+":K"+i+")");
            min.setCellStyle(cellStyle);
            max = getXSSFCell(getXSSFRow(s,i-1),12);
            max.setCellType(Cell.CELL_TYPE_FORMULA);
            max.setCellFormula("MAX(B"+i+":K"+i+")");
            max.setCellStyle(cellStyle);
            average = getXSSFCell(getXSSFRow(s,i-1),13);
            average.setCellType(Cell.CELL_TYPE_FORMULA);
            average.setCellFormula("AVERAGE(B"+i+":K"+i+")");
            average.setCellStyle(cellStyle);
            stdev = getXSSFCell(getXSSFRow(s,i-1),14);
            stdev.setCellType(Cell.CELL_TYPE_FORMULA);
            stdev.setCellFormula("STDEV(B"+i+":K"+i+")");
            stdev.setCellStyle(cellStyle);
        }

    }

    private void setDefaultText(XSSFSheet s){
        XSSFRow r = getXSSFRow(s,0);
        XSSFCell c ;
        c = getXSSFCell(r,0);
        c.setCellValue("T");
        c.setCellStyle(cellStyle);
        for(int i = 1 ; i <= 10 ; i++){
            c = getXSSFCell(r, i);
            c.setCellValue("#"+i);
            c.setCellStyle(cellStyle);
        }
        c = getXSSFCell(r, 11);
        c.setCellValue("MIN");
        c.setCellStyle(cellStyle);
        c = getXSSFCell(r, 12);
        c.setCellValue("MAX");
        c.setCellStyle(cellStyle);
        c = getXSSFCell(r, 13);
        c.setCellValue("AVG");
        c.setCellStyle(cellStyle);
        c = getXSSFCell(r, 14);
        c.setCellValue("STDEV");
        c.setCellStyle(cellStyle);
    }


//    private int writeOneSheet(DBCursor cursor,String queryName){
//        Row row ;
//        Cell cell ;
//        DBObject data ;
//        Sheet s = wb.createSheet("probes");
//        int i = 0 ;
//        try{
//            if(cursor.hasNext()){
//                data = cursor.next();
//                row = getRow(s,0);
//                BasicDBList names = (BasicDBList) cursor.next().get("dsnames");
//                cell = getCell(row,0);
//                cell.setCellStyle(cellStyle);
//                cell.setCellValue("Time");
//                for(int j = 1 ; j<= names.size() ; j++){
//                    cell = getCell(row,j);
//                    cell.setCellStyle(cellStyle);
//                    cell.setCellValue(queryName+"."+names.get(j-1).toString());
//                }
//                BasicDBList values = (BasicDBList) data.get("values");
//                for(int k = 0 ; k < values.size(); k++){
//                    Double value = (Double)values.get(k);
//                    cells[j].setCellType(Cell.CELL_TYPE_NUMERIC);
//                    cells[j].setCellValue(value.doubleValue());
//                }
//            }
//            for(i = 0 ; cursor.hasNext(); i++){
//                data = cursor.next();
//                GregorianCalendar cal =new GregorianCalendar();
//                cal.setTime((Date)data.get("time"));
//                r = getRow(s,0);
//
//            }
//        }
//    }
//
//
//
//
//
//    public void writeToExcelChoralies() throws IOException, ParseException, InvalidFormatException {
//        DBCursor cursor ;
//        for(CollectdQuery query : queries){
//            cursor = find(query);
//            Sheet[] sheets = createSheets(wb,query);
//            writeMultipleSheetChoralies(sheets,cursor);
//
//        }
//        FileOutputStream out = new FileOutputStream(this.outputName);
//        wb.write(out);
//        out.close();
//        wb.dispose();
//
//
//    }
//
//    private void writeMultipleSheetChoralies(Sheet[] sheets , DBCursor cursor){
//        Row[] rows = new Row[sheets.length];
//        Cell[] cells = new Cell[sheets.length];
//        DBObject data ;
//        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
//        GregorianCalendar cal =new GregorianCalendar();
//        try{
//            for(int i = 0 ; cursor.hasNext(); i++){
//                data = cursor.next();
//
//                cal.setTime((Date)data.get("time"));
//
//                for(int j = 0 ; j < rows.length ; j++){
//                    rows[j] = getRow(sheets[j],i);
//                    cells[j] = getCell(rows[j],0);
//                    cells[j].setCellValue(simpleDateFormat.format(cal.getTime()));
//                    cells[j] = getCell(rows[j], 1);
//                    BasicDBList values = (BasicDBList) data.get("values");
//                    for(int k = 0 ; k < sheets.length ; k++){
//                        double value = (Double)values.get(k);
//                        cells[j].setCellValue(value);
//                    }
//                }
//            }
//
//            for(int i = 0 ; i < sheets.length ; i++){
//                sheets[i].autoSizeColumn(0);
//                sheets[i].autoSizeColumn(1);
//            }
//        }finally {
//            cursor.close();
//        }
//    }



}
