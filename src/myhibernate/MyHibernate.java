package myhibernate;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.List;
import java.lang.System;

import myhibernate.demo.DatabaseManager;
import myhibernate.ann.*;
import myhibernate.demo.QueryBuilder;
import myhibernate.demo.QueryResult;

public class MyHibernate {
    public static final DatabaseManager db = new DatabaseManager(System.getenv("DB_URL"), "sa", "");
    private static final Class<?> tipoEntero = Integer.TYPE;
    private static final Class<?> tipoDouble = Double.TYPE;

    public static <T> T find(Class<T> clazz, int id) {
        QueryBuilder qb = new QueryBuilder(clazz, db);
        T objetoRetorno;

        QueryResult qr = qb.getQueryFind(id);
        avanzarResultado(qr);
        objetoRetorno = construirObjeto(clazz, qr);
        return objetoRetorno;
    }

    public static <T> List<T> findAll(Class<T> clazz) {
        QueryBuilder qb = new QueryBuilder(clazz, db);
        List<T> listaObjetosRetorno = new ArrayList<>();
        T objetoRetorno;
        QueryResult qr = qb.getQueryFind(-1);
        do{
            avanzarResultado(qr);
            objetoRetorno = construirObjeto(clazz, qr);
            if(objetoRetorno != null)
                listaObjetosRetorno.add(objetoRetorno);
        } while (objetoRetorno != null);
        return listaObjetosRetorno;
    }

    public static Query createQuery(String hql) {
        // PROGRAMAR AQUI
        return null;
    }

    private static <T> T instanciarObjetoGenerico(Class <T> clazz){
        T objetoRetorno = null;
        try {
            objetoRetorno = clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return objetoRetorno;
    }

    private static void setearCampo(Field field, Object objeto, Object valor){
        try {
            field.set(objeto, valor);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            e.printStackTrace();
        }

    }

    private static <T> T construirObjeto(Class<T> clazz, QueryResult qr){
        T objetoRetorno = instanciarObjetoGenerico(clazz);
        Field[] fields = clazz.getFields();
        String nombreColumnaID = "id_" + clazz.getAnnotation(Table.class).name();

        int id;
        try {
            id = qr.rs.getInt(nombreColumnaID);
        } catch (SQLException throwables) {
            // throwables.printStackTrace();
            return null;
        }

        for (Field field : fields) {
            // recorro todos los campos
            if (field.isAnnotationPresent(Column.class)) {
                // caso campo es una columna comun
                Object campoObjetoRetorno;
                if(field.isAnnotationPresent(Id.class))
                    campoObjetoRetorno = id;
                else campoObjetoRetorno = obtenerValor(field, qr, clazz);
                setearCampo(field, objetoRetorno, campoObjetoRetorno);
            }
            else if (field.isAnnotationPresent(JoinColumn.class)) {
                // caso campo es una FK
                Object objetoJoin;
                int idObjetoJoin = id;
                Class<?> tipoCampo = field.getType();
                String nombreColumnaJoin = field.getAnnotation(JoinColumn.class).name();

                if(qr.referencianMismaTabla.contains(field)) {
                    try {
                        idObjetoJoin = qr.rs.getInt(nombreColumnaJoin);
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                        return null;
                    }
                }

                if(idObjetoJoin > 0) {
                    if(qr.referencianMismaTabla.contains(field))
                        // este es un caso especial en donde la data que quiero esta en otra tabla que todavia no traje
                        // y ademas esa tabla tiene un columna con FK que referencia a esa misma tabla
                        // entonces voy trayendo uno por uno a medida que los voy encontrando
                        // una alternativa para optimizar esto y tambien evitar recursion infinita seria hacerlo lazy
                        // y que solo haga el query cuando se accede al campo
                        // asi como esta si por ejemplo dos empleados se tuvieran como jefes el uno al otro, entra en un
                        // bucle infinito (pero esa situacion no tiene sentido)
                        objetoJoin = find(tipoCampo, idObjetoJoin);
                    else objetoJoin = construirObjeto(tipoCampo, qr);
                } else objetoJoin = null;
                setearCampo(field, objetoRetorno, objetoJoin);
            }
        }
        return objetoRetorno;
    }

    public static void avanzarResultado(QueryResult qr){
        try {
            qr.rs.next();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private static Object obtenerValor(Field field, QueryResult qr, Class<?> claseMadre) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        ResultSet rs = qr.rs;
        Class<?> tipoField = field.getType();
        String tablaClase = claseMadre.getAnnotation(Table.class).name();
        Object valorColumna;
        if (field.isAnnotationPresent(Column.class)) {
            // caso campo es un Column
            String nombreColumna = String.format("%s.%s", tablaClase, field.getAnnotation(Column.class).name());
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
                } else if (tipoField.isAssignableFrom(tipoDouble)) {
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
