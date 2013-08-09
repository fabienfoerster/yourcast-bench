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
        CollectdDataExtractor extractor = new CollectdDataExtractor(args[0],args[1],Long.parseLong(args[2]),Long.parseLong(args[3]),Integer.parseInt(args[4]),Boolean.parseBoolean(args[5]));
        extractor.writeToExcel();
    }
}
