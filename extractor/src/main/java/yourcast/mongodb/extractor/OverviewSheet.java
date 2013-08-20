package yourcast.mongodb.extractor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: fabien foerster
 * Date: 20/08/13
 * Time: 10:33
 */
public class OverviewSheet {
    private String sheetName ;
    private List<CollectdQuery> queries ;

    public OverviewSheet(String sheetName){
        this.sheetName = sheetName ;
        queries = new ArrayList<CollectdQuery>();
    }

    public void addQuery(CollectdQuery query){
        queries.add(query);
    }

    public String getName(){
        return sheetName ;
    }

    public List<String> getQueryName(){
        List<String> names = new ArrayList<String>();
        for(CollectdQuery query : queries){
            names.add(query.getQueryName());
        }
        return names;
    }

}
