package myhibernate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.List;
import java.lang.System;

import myhibernate.demo.DatabaseManager;
import myhibernate.ann.*;

public class MyHibernate {
    private static final Class<?> tipoEntero = Integer.TYPE;
    private static final Class<?> tipoDouble = Double.TYPE;

    public static <T> T find(Class<T> clazz, int id) {
        String nombreColumnaID = "";
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        T objetoRetorno = null;
        DatabaseManager db = new DatabaseManager(System.getenv("DB_URL"), "sa", "");
        db.conectar();

        // generar una instancia nueva de la clase generica
        try {
            objetoRetorno = clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }

        // si no tiene la annotation Entity, no esta mapeada la clase
        assert clazz.isAnnotationPresent(Entity.class);

        Field[] fields = clazz.getDeclaredFields();
        String table = clazz.getAnnotation(Table.class).name();
        StringBuilder q = new StringBuilder(String.format("select * from %s", table));

        for (Field field : fields) {
            // recorro los annotations hasta que encuentro el Id
            if (field.isAnnotationPresent(Id.class))
                nombreColumnaID = field.getAnnotation(Column.class).name();

            // si encuentro un JoinColumn, agrego un join al query
            if (field.isAnnotationPresent(JoinColumn.class)) {
                String nombreColumnaIDFK = field.getAnnotation(JoinColumn.class).name();
                Class<?> tipoField = field.getType();
                String tablaTipoField = tipoField.getAnnotation(Table.class).name();

                q.append(String.format(" JOIN %s ON %s.%s=%s.%s", tablaTipoField, table, nombreColumnaIDFK, tablaTipoField, nombreColumnaIDFK));
            }
        }

        // voy a buscar a la db
        // String q = String.format("select * from %s where %s=%s", table, nombreColumnaID, id);
        q.append(String.format(" WHERE %s=%s", nombreColumnaID, id));
        ResultSet rs = db.query(q.toString());
        try {
            rs.next();
        } catch (SQLException throwables) {
            // si el resultset esta vacio, deberia fallar
            throwables.printStackTrace();
            db.cerrar();
            return null;
        }

        for (Field field : fields) {
            // TODO ver ManyToOne
            // recorro todos los campos
            Class<?> tipoField = field.getType();

            if (field.isAnnotationPresent(Column.class)) {
                // caso campo es un Column
                Object campoObjetoRetorno = obtenerValor(field, rs);
                try {
                    field.set(objetoRetorno, campoObjetoRetorno);
                } catch (IllegalAccessException | IllegalArgumentException e) {
                    e.printStackTrace();
                }

            } else if (field.isAnnotationPresent(JoinColumn.class)) {
                // caso campo es un JoinColumn
                Object objetoJoin = null;
                try {
                    objetoJoin = field.getType().newInstance();
                    for (Field fieldObjetoJoin : objetoJoin.getClass().getFields()) {
                        Object campoObjetoJoin = obtenerValor(fieldObjetoJoin, rs);
                        fieldObjetoJoin.set(objetoJoin, campoObjetoJoin);
                    }
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }

                try {
                    field.set(objetoRetorno, objetoJoin);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        db.cerrar();
        return objetoRetorno;
    }

    public static <T> List<T> findAll(Class<T> clazz) {
        // PROGRAMAR AQUI
        return null;
    }

    public static Query createQuery(String hql) {
        // PROGRAMAR AQUI
        return null;
    }

    private static Object obtenerValor(Field field, ResultSet rs) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Class<?> tipoField = field.getType();
        Object valorColumna;
        if (field.isAnnotationPresent(Column.class)) {
            // caso campo es un Column
            String nombreColumna = field.getAnnotation(Column.class).name();
            String valorColumnaString;

            try {
                if (tipoField.isAssignableFrom(tipoEntero)) {
                    // el campo es tipo int
                    valorColumna = rs.getInt(nombreColumna);
                    return valorColumna;

                } else if (tipoField.isAssignableFrom(String.class)) {
                    // el campo es tipo string
                    valorColumna = rs.getString(nombreColumna);
                    return valorColumna;

                } else if (tipoField.isAssignableFrom(Date.class)) {
                    // el campo es tipo date
                    valorColumnaString = rs.getString(nombreColumna);
                    try {
                        return df.parse(valorColumnaString);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                } else if (tipoField.isAssignableFrom(tipoDouble)){
                    // el campo es tipo double
                    valorColumna = rs.getDouble(nombreColumna);
                    return valorColumna;
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }

        }
        return null;
    }
}
