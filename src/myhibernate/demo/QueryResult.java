package myhibernate.demo;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.List;

public class QueryResult {
    public ResultSet rs;
    public List<Field> referencianMismaTabla;
    public String table;

    public QueryResult(ResultSet rs, String table, List<Field> referencianMismaTabla) {
        this.rs = rs;
        this.referencianMismaTabla = referencianMismaTabla;
        this.table = table;
    }
}
