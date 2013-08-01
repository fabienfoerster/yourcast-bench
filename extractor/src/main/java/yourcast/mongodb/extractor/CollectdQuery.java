package yourcast.mongodb.extractor;

import com.mongodb.BasicDBObject;

import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: binou
 * Date: 25/07/13
 * Time: 12:44
 * To change this template use File | Settings | File Templates.
 */
public class CollectdQuery {

    private String collectionName;
    private String plugin_instance ;
    private String type ;
    private String type_instance ;
    private String queryName ;

    public CollectdQuery(String collectionName,String plugin_instance,String type, String type_instance) {
        this.collectionName = collectionName;
        this.plugin_instance = plugin_instance;
        this.type = type ;
        this.type_instance = type_instance;
        queryName = buildQueryName() ;
    }


    public BasicDBObject buildQuery(long start , long end){
        BasicDBObject query = new BasicDBObject();
        Date fromDate = new Date(start);
        Date toDate = new Date(end);
        query.put("time",new BasicDBObject("$gte",fromDate).append("$lt",toDate));
        if(plugin_instance != null){
            query.put("plugin_instance",plugin_instance);
        }
        if(type != null){
            query.put("type",type);
        }
        if(type_instance != null){
            query.put("type_instance",type_instance);
        }
        return query;
    }

    public String getCollectionName(){
        return collectionName;
    }

    private String buildQueryName(){
        StringBuilder res = new StringBuilder();
        res.append(collectionName);
        if(this.plugin_instance != null){
            res.append(".");
            res.append(plugin_instance);
        }
        if(this.type != null){
            res.append(".");
            res.append(type);
        }
        if(this.type_instance != null){
            res.append(".");
            res.append(type_instance);
        }
        String name = res.toString();
        name = name.replaceAll("27017-?","");
        name = name.replaceAll("GenericJMX","jmx");
        return name;
    }


    public String getQueryName(){
        return queryName ;
    }


}
