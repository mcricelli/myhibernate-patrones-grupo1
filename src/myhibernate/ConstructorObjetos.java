package myhibernate;

import myhibernate.ann.Column;
import myhibernate.ann.Id;
import myhibernate.ann.JoinColumn;
import myhibernate.ann.Table;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class ConstructorObjetos {
    private static final Class<?> tipoEntero = Integer.TYPE;
    private static final Class<?> tipoDouble = Double.TYPE;

    public static <T> T construir(Class<?> clazz, QueryResult qr){
        Field[] fields = clazz.getFields();
        String nombreColumnaID = "id_" + clazz.getAnnotation(Table.class).name();

        int rowNumber;
        int id;
        try {
            id = qr.rs.getInt(nombreColumnaID);
        } catch (SQLException throwables) {
            // throwables.printStackTrace();
            return null;
        }
        T objetoRetorno = Proxy.generar(clazz);

        for (Field field : fields) {
            // recorro todos los campos
            if (field.isAnnotationPresent(Column.class)) {
                // caso campo es una columna comun
                Object campoObjetoRetorno;
                if(field.isAnnotationPresent(Id.class))
                    campoObjetoRetorno = id;
                else {
                    try {
                        campoObjetoRetorno = obtenerValor(field, qr, clazz);
                    } catch (SQLException throwables) {
                        return null;
                    }
                }
                setearCampo(field, objetoRetorno, campoObjetoRetorno);
            }
            else if (field.isAnnotationPresent(JoinColumn.class)) {
                // caso campo es una FK
                int idObjetoJoin;
                Class<?> tipoCampo = field.getType();
                Object objetoJoin;
                String nombreColumnaJoin = field.getAnnotation(JoinColumn.class).name();

                try {
                    idObjetoJoin = qr.rs.getInt(nombreColumnaJoin);
                } catch (SQLException throwables) {
                    System.out.println("Falla al encontrar ID de objeto join en columna: " + nombreColumnaJoin);
                    throwables.printStackTrace();
                    idObjetoJoin = -1;
                }

                if(qr.autoReferencias.contains(field)) {
                    // si hay una autoreferencia hay que mover el cursor a la fila que se busca y luego retornar el
                    // cursor a donde estaba antes
                    rowNumber = avanzarResultadoHasta(qr, idObjetoJoin, nombreColumnaID);
                    objetoJoin = construir(tipoCampo, qr);
                    try {
                        qr.rs.absolute(rowNumber);
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                }else objetoJoin = construir(tipoCampo, qr);

                if(objetoJoin == null) {
                    objetoJoin = Proxy.generar(tipoCampo);
                    if (objetoJoin != null && idObjetoJoin > 0)
                        // se usa el ID del objeto negativo para que el proxy detecte que el objeto todavia no se cargo
                        // desde la base de datos. Una vez lo carga, lo reemplaza con el ID positivo para indicar que ya
                        // esta cargado y no volver a cargarlo cada vez que intercepta el metodo
                        // queda como limitacion que no tolera IDs negativos en la base de datos
                        encontrarYsetearId(objetoJoin, -idObjetoJoin);
                    else objetoJoin = null;
                }
                setearCampo(field, objetoRetorno, objetoJoin);
            }
        }
        return objetoRetorno;
    }

    public static int avanzarResultadoHasta(QueryResult qr, int id, String nombreColumnaID){
        int idAux, rowNumber = 1;
        try {
            rowNumber = qr.rs.getRow();
            qr.rs.absolute(1);
            do{
                idAux = qr.rs.getInt(nombreColumnaID);
            } while(idAux != id && qr.rs.next());
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return rowNumber;
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

    private static Object obtenerValor(Field field, QueryResult qr, Class<?> claseMadre) throws SQLException {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        ResultSet rs = qr.rs;
        Class<?> tipoField = field.getType();
        String tablaClase = claseMadre.getAnnotation(Table.class).name();
        Object valorColumna;
        if (field.isAnnotationPresent(Column.class)) {
            // caso campo es un Column
            String nombreColumna = String.format("%s.%s", tablaClase, field.getAnnotation(Column.class).name());
            String valorColumnaString;

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
}
