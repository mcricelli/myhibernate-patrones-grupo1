package myhibernate.demo;

import myhibernate.ann.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class QueryBuilder {
    private final List<Field> referencianMismaTabla = new ArrayList<>();
    private final Field[] fields;
    private final String table;
    private final StringBuilder q;
    private String nombreVariable = "";
    public final StringBuilder joins;
    private final DatabaseManager db;
    private String nombreColumnaID;

    public QueryBuilder(Class<?> clazz, DatabaseManager db){
        assert clazz.isAnnotationPresent(Entity.class);
        this.db = db;
        fields = clazz.getFields();
        table = clazz.getAnnotation(Table.class).name();
        q = new StringBuilder(String.format("select * from %s", table));
        joins = new StringBuilder();
        analizarCampos();
    }

    private static QueryBuilder getFromString(String query, DatabaseManager db){
        QueryBuilder qb = null;
        try {
            qb = new QueryBuilder(Class.forName(query.split(" ")[1]), db);
            qb.setNombreVariable(query.split(" ")[2]);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return qb;
    }

    public void setNombreVariable(String nombreVariable) {
        this.nombreVariable = nombreVariable;
    }

    private void analizarCampos(){
        for (Field field : fields) {
            // recorro los annotations hasta que encuentro el Id
            if (field.isAnnotationPresent(Id.class))
                nombreColumnaID = field.getAnnotation(Column.class).name();

            // si encuentro un JoinColumn, agrego un join al query si face falta
            if (field.isAnnotationPresent(JoinColumn.class)) {
                String s;
                String nombreColumnaIDFK = field.getAnnotation(JoinColumn.class).name();
                Class<?> tipoField = field.getType();
                String tablaTipoField = tipoField.getAnnotation(Table.class).name();

                if (!table.equals(tablaTipoField)){
                    QueryBuilder qb = new QueryBuilder(tipoField, db);
                    // hay que ir a buscar a otra tabla
                    s = String.format(" JOIN %s ON %s.%s=%s.%s", tablaTipoField, table, nombreColumnaIDFK,
                            tablaTipoField, nombreColumnaIDFK);
                    joins.append(s);
                    joins.append(qb.joins);
                    referencianMismaTabla.addAll(qb.referencianMismaTabla);
                }
                // es una key en la misma tabla que ya tenemos
                else referencianMismaTabla.add(field);
            }
        }
    }

    public QueryResult getQueryFind(int id) {
        QueryResult qr;
        q.append(joins);
        if(id != -1)
            // si le paso id -1, trae la tabla entera (conviene para findAll)
            q.append(String.format(" WHERE %s=%s", nombreColumnaID, id));

        qr = new QueryResult(db.query(q.toString()), table, referencianMismaTabla);
        return qr;
    }
}
