package myhibernate;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;
import java.text.SimpleDateFormat;
import java.lang.System;

import myhibernate.demo.*;
import myhibernate.ann.*;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.SuperMethodCall;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class MyHibernate {
    public static final DatabaseManager db = new DatabaseManager(System.getenv("DB_URL"), "sa", "");
    private static final Class<?> tipoEntero = Integer.TYPE;
    private static final Class<?> tipoDouble = Double.TYPE;
    private static final Map<Class<?>, Class<?>> clasesMejoradas = new HashMap<>();

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

    private static <T> T generarProxy(Class<T> claseBase, int id){
        DynamicType.Builder<?> builder = new ByteBuddy().subclass(claseBase);

        try {
            Class<?> claseMejorada = clasesMejoradas.get(claseBase);

            if (claseMejorada == null) {
                for (Field field : claseBase.getDeclaredFields()) {
                    if (field.isAnnotationPresent(JoinColumn.class)) {
                        String name = field.getName();
                        String camelCaseName = name.substring(0, 1).toUpperCase() + name.substring(1);

                        String getterName = "get" + camelCaseName;
                        builder = builder.method(named(getterName))
                                .intercept(
                                        MethodCall.invoke(Interceptor.class.getMethod("intercept", Field.class, Object.class))
                                                .with(field).withThis()
                                                .andThen(SuperMethodCall.INSTANCE)
                                );
                    }
                }

                claseMejorada = builder.make().load(Demo.class.getClassLoader()).getLoaded();

                clasesMejoradas.put(claseBase, claseMejorada);
            }
            return (T)claseMejorada.getConstructors()[0].newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void setearCampo(Field field, Object objeto, Object valor){
        try {
            field.set(objeto, valor);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            e.printStackTrace();
        }

    }

    private static <T> T construirObjeto(Class<T> clazz, QueryResult qr){
        Field[] fields = clazz.getFields();
        String nombreColumnaID = "id_" + clazz.getAnnotation(Table.class).name();

        int id;
        try {
            id = qr.rs.getInt(nombreColumnaID);
        } catch (SQLException throwables) {
            // throwables.printStackTrace();
            return null;
        }
        T objetoRetorno = generarProxy(clazz, id);

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
                int idObjetoJoin = id;
                Class<?> tipoCampo = field.getType();
                Object objetoJoin = generarProxy(tipoCampo, idObjetoJoin);
                String nombreColumnaJoin = field.getAnnotation(JoinColumn.class).name();

                try {
                    idObjetoJoin = qr.rs.getInt(nombreColumnaJoin);
                } catch (SQLException throwables) {
                    System.out.println("Falla al encontrar ID de objeto join en columna: " + nombreColumnaJoin);
                    throwables.printStackTrace();
                    idObjetoJoin = -1;
                }

                if (objetoJoin != null && idObjetoJoin > 0)
                    // se usa el ID del objeto negativo para que el proxy detecte que el objeto todavia no se cargo
                    // desde la base de datos. Una vez lo carga, lo reemplaza con el ID positivo para indicar que ya
                    // esta cargado y no volver a cargarlo cada vez que intercepta el metodo
                    // queda como limitacion que no tolera IDs negativos en la base de datos
                    encontrarYsetearId(objetoJoin, -idObjetoJoin);
                else objetoJoin = null;
                setearCampo(field, objetoRetorno, objetoJoin);
            }
        }
        return objetoRetorno;
    }

    private static void encontrarYsetearId(Object o, int id) {
        Field[] fields = o.getClass().getFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                if (field.isAnnotationPresent(Id.class))
                    setearCampo(field, o, id);
            }
        }
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
