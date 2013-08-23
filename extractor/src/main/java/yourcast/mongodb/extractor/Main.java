package yourcast.mongodb.extractor;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import java.io.IOException;
import java.text.ParseException;

/**
 * Created with IntelliJ IDEA.
 * User: fabien foerster
 * Date: 29/07/13
 * Time: 09:51
 */
public class Main {
    public static void main(String[] args) throws IOException, ParseException, InvalidFormatException {
        CollectdDataExtractor extractor ;
        if(args.length == 5){
            extractor = new CollectdDataExtractor(args[0],args[1],Boolean.parseBoolean(args[2]),Long.parseLong(args[3]),Long.parseLong(args[4]));
        } else {
            extractor = new CollectdDataExtractor(args[0],args[1],Boolean.parseBoolean(args[2]),args[3]);
        }
        extractor.writeToExcel();
    }
}
