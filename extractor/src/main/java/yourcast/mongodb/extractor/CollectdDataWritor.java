package yourcast.mongodb.extractor;

import com.mongodb.DBCursor;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.*;

import java.io.*;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: fabien foerster
 * Date: 08/08/13
 * Time: 21:59
 */
public abstract class CollectdDataWritor {
    protected SXSSFWorkbook sxssfWorkbook ;
    protected XSSFWorkbook xssfWorkbook ;
    protected XSSFCellStyle cellStyle ;
    protected String outputName ;
    protected FileOutputStream out ;
    protected Map<String,DBCursor> cursors ;

    public CollectdDataWritor(String outputName) throws IOException, InvalidFormatException {
        this.outputName = outputName ;
        File f = new File(this.outputName);
        if(f.exists()){
            f.delete();
        }
        sxssfWorkbook = new SXSSFWorkbook(100);
        xssfWorkbook = sxssfWorkbook.getXSSFWorkbook();
        out = null ;
        createCellStyle();
        cursors = new LinkedHashMap<String, DBCursor>();
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

    protected Sheet getSheet(SXSSFWorkbook wb, String name){
        Sheet s = wb.getSheet(name);
        if(s == null){
            s = wb.createSheet(name);
        }
        return s;
    }


    public abstract void writeToExcel() throws IOException, ParseException, InvalidFormatException;

    public void addCursor(String queryName,DBCursor cursor){
        cursors.put(queryName,cursor);
    }

    public void resetCursor(){
        cursors = new LinkedHashMap<String, DBCursor>();
    }

    public void open() throws FileNotFoundException {
        out = new FileOutputStream(this.outputName);
    }

    public void close() throws IOException {
        sxssfWorkbook.write(out);
        out.close();
        sxssfWorkbook.dispose();
    }

}
