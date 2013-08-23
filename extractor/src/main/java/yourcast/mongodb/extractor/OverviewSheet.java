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
    private List<String> queriesNames ;
    private int nbRow ;

    public OverviewSheet(String sheetName){
        this.sheetName = sheetName + " - Overview" ;
        queriesNames = new ArrayList<String>();
        nbRow = 0 ;
    }

    public void addQuery(String queryName){
        queriesNames.add(queryName);
    }

    public void removeQuery(String queryName){
        queriesNames.remove(queryName);
    }

    public boolean contains(String queryName){
        return queriesNames.contains(queryName);
    }

    public String getName(){
        return sheetName ;
    }

    public List<String> getQueryName(){
      return queriesNames;
    }

    public int getNbRow() {
        return nbRow;
    }

    public void setNbRow(int nbRow) {
        this.nbRow = nbRow;
    }
}
