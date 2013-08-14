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
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: fabien foerster
 * Date: 09/08/13
 * Time: 10:19
 */
public class CollectdDataWritorMonitoring extends CollectdDataWritor {

    private int lastColumToWrite;
    private int lastColum ;
    private Sheet sheet ;

    public CollectdDataWritorMonitoring(String outputName) throws IOException, InvalidFormatException {
        super(outputName);
        lastColumToWrite = 1 ;
        lastColum = 1 ;
        sheet = getSheet(sxssfWorkbook,"probes");
    }

    @Override
    public void writeToExcel(DBCursor cursor, CollectdQuery query) throws IOException, ParseException, InvalidFormatException {
        throw new RuntimeException("Not implemented");
    }


    public void writeToExcel() throws IOException, ParseException, InvalidFormatException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        GregorianCalendar cal =new GregorianCalendar();
        DBObject data ;
        Row r ;
        Cell c ;
        int k = 0 ;
        DBObject[] oldData = new DBObject[cursors.size()];
        for(Map.Entry<String,DBCursor> entry : cursors.entrySet()){
            oldData[k] = entry.getValue().next();
            createColumnName(oldData[k],entry.getKey());
            k++;
        }
        k = 0 ;
        lastColumToWrite = 1 ;
        int number_values;
        int i = 1 ;
        boolean keepOnLooping = true ;
        while(keepOnLooping){
            keepOnLooping = false ;
            for(Map.Entry<String,DBCursor> entry : cursors.entrySet()){
                number_values = 0 ;
                keepOnLooping = keepOnLooping || entry.getValue().hasNext() ;
                if(entry.getValue().hasNext()){
                    if(oldData[k] == null){
                        data = entry.getValue().next();
                        oldData[k] = data ;
                    } else {
                        data = oldData[k];
                    }
                    cal.setTime((Date)data.get("time"));
                    String date = simpleDateFormat.format(cal.getTime());
                    r = getRow(sheet,i);
                    c = getCell(r,0);
                    if(c.getStringCellValue().isEmpty() || date.equals(c.getStringCellValue())){
                        c.setCellValue(date);
                        BasicDBList values = (BasicDBList) data.get("values");
                        number_values = values.size() ;
                        for(int j = 0 ; j < number_values ; j++){
                            c = getCell(r , lastColumToWrite +j);
                            double value =  (Double)values.get(j);
                            c.setCellValue(value);
                        }
                        oldData[k] = null ;
                    }
                }
                k++;
                lastColumToWrite += number_values ;
            }
            k = 0 ;
            i++;
            lastColum = lastColumToWrite > lastColum ? lastColumToWrite : lastColum ;
            lastColumToWrite = 1 ;
        }
        setAutoSizeColum();
    }


    private void createColumnName(DBObject data , String queryName ){
        Row r = getRow(sheet,0);
        Cell c = getCell(r,0);
        c.setCellValue("time");
        BasicDBList names = (BasicDBList) data.get("dsnames");
        String columnName = queryName;
        if(names.size() > 1){
            for(int i = 0 ; i < names.size() ; i++){
                c = getCell(r, lastColumToWrite +i);
                columnName += "."+names.get(i).toString();
                c.setCellValue(columnName);
            }
            lastColumToWrite += names.size();
        } else {
            c = getCell(r, lastColumToWrite);
            c.setCellValue(columnName);
            lastColumToWrite++ ;
        }

    }

    private void setAutoSizeColum(){
        for(int i = 0 ; i < lastColum; i++){
            sheet.autoSizeColumn(i);
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
        lastColumToWrite = 1 ;
    }
}
