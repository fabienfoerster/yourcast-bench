package yourcast.mongodb.extractor;

import com.mongodb.BasicDBList;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created with IntelliJ IDEA.
 * User: fabien foerster
 * Date: 09/08/13
 * Time: 10:19
 */
public class CollectdDataWritorMonitoring extends CollectdDataWritor {

    private int lastColumn ;
    private Sheet sheet ;

    public CollectdDataWritorMonitoring(String outputName) throws IOException, InvalidFormatException {
        super(outputName);
        lastColumn = 1 ;
        sheet = getSheet(xssfWorkbook,"probes");
    }


    @Override
    public void writeToExcel(DBCursor cursor, CollectdQuery query) throws IOException, ParseException, InvalidFormatException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        GregorianCalendar cal =new GregorianCalendar();
        DBObject data ;
        Row r ;
        Cell c ;
        createColumnName(cursor.copy(),query);
        int number_values = 0 ;
        try{
            for(int i = 1 ; cursor.hasNext(); i++){
                data = cursor.next();
                cal.setTime((Date)data.get("time"));
                r = getRow(sheet,i);
                c = getCell(r,0);
                c.setCellValue(simpleDateFormat.format(cal.getTime()));
                BasicDBList values = (BasicDBList) data.get("values");
                number_values = values.size() ;
                for(int j = 0 ; j < values.size() ; j++){
                    c = getCell(r ,lastColumn+j);
                    double value =  (Double)values.get(j);
                    c.setCellValue(value);

                }

            }
        }finally {
            cursor.close();
            lastColumn += number_values ;
        }
    }


    private void createColumnName(DBCursor cursor , CollectdQuery query ){

        if(cursor.hasNext()){
            Row r = getRow(sheet,0);
            Cell c = getCell(r,0);
            c.setCellValue("time");
            BasicDBList names = (BasicDBList) cursor.next().get("dsnames");
            String columnName = query.getQueryName();
            if(names.size() > 1){
                for(int i = 0 ; i < names.size() ; i++){
                    c = getCell(r,lastColumn+i);
                    columnName += "."+names.get(i).toString();
                    c.setCellValue(columnName);
                }
            } else {
                c = getCell(r,lastColumn);
                c.setCellValue(columnName);
            }

        }
    }

    private void setAutoSizeColum(){
        for(int i = 0 ; i < lastColumn ; i++){
            sheet.autoSizeColumn(i);
        }
    }

    @Override
    public void close() throws IOException {
        setAutoSizeColum();
        super.close();
        lastColumn = 1 ;
    }
}
