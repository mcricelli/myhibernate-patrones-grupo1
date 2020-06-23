package myhibernate;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.List;

public class QueryResult {
    public final ResultSet rs;
    public final List<Field> autoReferencias;

    public QueryResult(ResultSet rs, List<Field> autoReferencias) {
        // resultSet del query + algunos metadatos
        this.rs = rs;
        this.autoReferencias = autoReferencias;
    }
}
