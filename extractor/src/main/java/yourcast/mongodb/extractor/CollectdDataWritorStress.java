package yourcast.mongodb.extractor;

import com.mongodb.BasicDBList;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.io.IOException;
import java.text.ParseException;

/**
 * Created with IntelliJ IDEA.
 * User: fabien foerster
 * Date: 08/08/13
 * Time: 23:01
 */
public class CollectdDataWritorStress extends CollectdDataWritor {

    private int row_write ;
    private int row_offset;
    private int col_offset ;

    public CollectdDataWritorStress(String outputName,int row_offset , int col_offset) throws IOException, InvalidFormatException {
        super(outputName);
        this.row_offset = row_offset ;
        this.col_offset = col_offset ;
    }

    @Override
    public void writeToExcel(DBCursor cursor , CollectdQuery query ) throws IOException, ParseException, InvalidFormatException {
        Sheet[] sheets = createSheets(cursor,query);
        row_write =  writeMultipleSheet(sheets,cursor);
        for(int i = 0 ; i < xssfWorkbook.getNumberOfSheets() ; i++){
            createFormulas(xssfWorkbook.getSheetAt(i),row_write);
            setDefaultText(xssfWorkbook.getSheetAt(i));
        }
    }


    private Sheet[] createSheets(DBCursor cursor , CollectdQuery query){
        Sheet[] sheets = new Sheet[1] ;
        if(cursor.hasNext()){
            BasicDBList names = (BasicDBList) cursor.next().get("dsnames");
            if(names.size() > 1){
                sheets = new Sheet[names.size()];
                for(int i = 0 ; i < names.size() ; i++ ){
                    String sheetName = query.getQueryName() + "."+ names.get(i).toString();
                    sheets[i] = getSheet(sxssfWorkbook, sheetName);
                }
                return sheets;
            }

        }
        sheets[0] = getSheet(sxssfWorkbook, query.getQueryName());
        return sheets;
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

    private int writeMultipleSheet(Sheet[] sheets , DBCursor cursor){
        Row[] rows = new Row[sheets.length];
        Cell[] cells = new Cell[sheets.length];
        DBObject data ;
        int i = 0;
        try{

            for(i = 0 ; cursor.hasNext(); i++){
                data = cursor.next();

                for(int j = 0 ; j < rows.length ; j++){
                    rows[j] = getRow(sheets[j],i+ row_offset);
                    cells[j] = getCell(rows[j],0);
                    cells[j].setCellStyle(cellStyle);
                    cells[j].setCellValue(i);
                    cells[j] = getCell(rows[j], col_offset);
                    BasicDBList values = (BasicDBList) data.get("values");
                    for(int k = 0 ; k < sheets.length ; k++){
                        Double value = (Double)values.get(k);
                        cells[j].setCellType(Cell.CELL_TYPE_NUMERIC);
                        cells[j].setCellValue(value);
                    }
                }
            }
        }finally {
            cursor.close();
        }
        return i;
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
}