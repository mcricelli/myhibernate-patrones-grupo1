package myhibernate;

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

public class MyHibernate
{
   private static final Class<?> tipoEntero = Integer.TYPE;

   public static <T> T find(Class<T> clazz, int id)
   {
      String nombreColumnaID = "";
      SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
      T objetoRetorno = null;
      DatabaseManager db = new DatabaseManager(System.getenv("DB_URL"),"sa", "");
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

      for(Field field : fields){
         // recorro los annotations hasta que encuentro el Id
         if(field.isAnnotationPresent(Id.class))
            nombreColumnaID = field.getAnnotation(Column.class).name();
      }

      // voy a buscar a la db
      String q = String.format("select * from %s where %s=%s", table, nombreColumnaID, id);
      ResultSet rs = db.query(q);
      try {
         rs.next();
      } catch (SQLException throwables) {
         // si el resultset esta vacio, deberia fallar
         throwables.printStackTrace();
         db.cerrar();
         return null;
      }

      for(Field field : fields){
         // TODO ver ManyToOne
         // recorro todos los campos
         Class<?> tipoField = field.getType();

         if(field.isAnnotationPresent(Column.class)) {
            // caso campo es un Column
            String nombreColumna = field.getAnnotation(Column.class).name();
            int valorColumnaInt;
            String valorColumnaString;

            try {
               if (tipoField.isAssignableFrom(tipoEntero)) {
                  // el field es tipo int
                  valorColumnaInt = rs.getInt(nombreColumna);
                  try {
                     field.set(objetoRetorno, valorColumnaInt);
                  } catch (IllegalAccessException e) {
                     e.printStackTrace();
                  }

               } else if (tipoField.isAssignableFrom(String.class)) {
                  // el campo es tipo string
                  valorColumnaString = rs.getString(nombreColumna);
                  try {
                     field.set(objetoRetorno, valorColumnaString);
                  } catch (IllegalAccessException e) {
                     e.printStackTrace();
                  }

               } else if (tipoField.isAssignableFrom(Date.class)) {
                  // el campo es tipo date
                  valorColumnaString = rs.getString(nombreColumna);
                  try {
                     Date d = df.parse(valorColumnaString);
                     field.set(objetoRetorno, d);
                  } catch (ParseException | IllegalAccessException e) {
                     e.printStackTrace();
                  }
               }
            } catch (SQLException throwables) {
               throwables.printStackTrace();
            }

         } else if(field.isAnnotationPresent(JoinColumn.class)){
            // caso campo es un JoinColumn (hay que encontrar la FK y llamar a find recursivamente)
            String nombreColumna = field.getAnnotation(JoinColumn.class).name();
            int foreignKey = -1;
            try {
               foreignKey = rs.getInt(nombreColumna);
            } catch (SQLException throwables) {
               throwables.printStackTrace();
            }

            try {
               field.set(objetoRetorno, find(tipoField, foreignKey));
            } catch (IllegalAccessException e) {
               e.printStackTrace();
            }
         }
      }
      db.cerrar();
      return objetoRetorno;
   }

   public static <T> List<T> findAll(Class<T> clazz)
   {
      // PROGRAMAR AQUI
      return null;
   }

   public static Query createQuery(String hql)
   {
      // PROGRAMAR AQUI
      return null;
   }

}
