package myhibernate;

import myhibernate.ann.*;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Query
{
   private final Map<String, String> map = new HashMap<>();
   private static final Pattern parameterPattern = Pattern.compile(":[a-z][a-z0-9]+", Pattern.CASE_INSENSITIVE);
   private static final Pattern hqlPattern = Pattern.compile("from ([a-z]+) ([a-z]+)((?: join [.a-z]+ (as [a-z]+)?)*) ?(where ([a-z._]+)=([a-z0-9.:]+))?", Pattern.CASE_INSENSITIVE);
   private static final Pattern joinPattern = Pattern.compile("(?:join ([a-z]+).([a-z]+) as ([a-z]+))+", Pattern.CASE_INSENSITIVE);
   private static final Pattern wherePattern = Pattern.compile("where (?:([a-z_]+)\\.?)+=(:?[a-z0-9.]+)", Pattern.CASE_INSENSITIVE);
   private Class<?> claseObjetivo;
   private String tabla = "", nombreAlias = "", clauseJoins = "", clauseWhere = "";
   private final Map<String, Class<?>> symbols = new HashMap<>();
   private List<Field> autoreferencias;

   public Query(String q){
      Matcher m = parameterPattern.matcher(q);
      while (m.find())
         map.put(m.group(0), "");

      Matcher hql = hqlPattern.matcher(q);
      if(hql.find()) {
         try {
            claseObjetivo = Class.forName("myhibernate.demo." + hql.group(1));
            if(!claseObjetivo.isAnnotationPresent(Entity.class)){
               claseObjetivo = null;
               return;
            }

            tabla = claseObjetivo.getAnnotation(Table.class).name();
            nombreAlias = hql.group(2);
            clauseJoins = hql.group(3);
            clauseWhere = hql.group(5);
            if(clauseWhere == null)
               clauseWhere = "";

            symbols.put(nombreAlias, claseObjetivo);
            autoreferencias = buscarAutoreferencias(claseObjetivo);
         } catch (ClassNotFoundException | NullPointerException e) {
            claseObjetivo = null;
            e.printStackTrace();
         }
      }
   }

   public boolean esValido(){
      return claseObjetivo != null && !tabla.isEmpty() && !nombreAlias.isEmpty() && !existenParametrosIndefinidos();
   }

   public static Query fromClass(Class<?> clazz, int id){
      String nombreColumnaID;
      StringBuilder q = new StringBuilder();
      String nombreClase = clazz.getSimpleName();
      q.append(String.format("FROM %s x ", nombreClase));

      if(id >= 0){
         nombreColumnaID = obtenerNombreCampoID(clazz);
         q.append(String.format("WHERE x.%s=%d", nombreColumnaID, id));
      }
      return new Query(q.toString());
   }


   private String generarJoin(Field field, String aliasClase, String aliasJoin){
      StringBuilder j = new StringBuilder();
      Class<?> clazz = field.getType();
      String tabla = clazz.getDeclaredAnnotation(Table.class).name(), nombreCampo = field.getName();
      String nombreColumnaID = obtenerNombreColumnaID(clazz);

      String clauseOn = String.format("%s.id_%s=%s.%s", aliasClase, nombreCampo, aliasJoin, nombreColumnaID);
      j.append(String.format("LEFT JOIN %s AS %s ON %s ", tabla, aliasJoin, clauseOn));
      return j.toString();
   }

   private String parsearJoins(){
      StringBuilder outputJoins = new StringBuilder();
      Class<?> claseJoin;

      Matcher joins = joinPattern.matcher(clauseJoins);
      while(joins.find()) {
         String aliasClase = joins.group(1);
         String nombreField = joins.group(2);
         String aliasJoin = joins.group(3);
         Field field;

         claseJoin = symbols.get(aliasClase);

         try {
            field = claseJoin.getDeclaredField(nombreField);
            symbols.put(aliasJoin, field.getType());
            outputJoins.append(generarJoin(field, aliasClase, aliasJoin));
         } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return "";
         }
      }

      return outputJoins.toString();
   }

   private String parsearWhere(){
      StringBuilder outputWhere = new StringBuilder();
      Class<?> clazz, clazz2;
      String valorBuscado;
      String[] campos;
      Field field;
      Number numeroValorBuscado;
      Column ann;

      try {
         campos = clauseWhere.substring(6, clauseWhere.indexOf("=")).split("\\.");
      } catch (StringIndexOutOfBoundsException e) {
         return "";
      }

      for (String nombreCampo : campos) {
         for (Map.Entry<String, Class<?>> clase : symbols.entrySet()) {
            clazz = clase.getValue();
            try {
               field = clazz.getDeclaredField(nombreCampo);
               clazz2 = field.getType();
               if(field.isAnnotationPresent(JoinColumn.class)) {
                  outputWhere.append(generarJoin(field, clase.getKey(), nombreCampo));
                  symbols.put(clazz2.getSimpleName().toLowerCase(), clazz2);
               }
            } catch (NoSuchFieldException e) {
               // e.printStackTrace();
            }
         }
      }

      Matcher where = wherePattern.matcher(clauseWhere);
      if(where.find()) {
         String nombreCampo = where.group(1);
         valorBuscado = where.group(2);

         try {
            numeroValorBuscado = NumberFormat.getInstance().parse(valorBuscado);
         } catch (ParseException e){
            numeroValorBuscado = 0;
         }

         for (Map.Entry<String, Class<?>> clase : symbols.entrySet()) {
            clazz = clase.getValue();
            try {
               field = clazz.getDeclaredField(nombreCampo);
               ann = field.getAnnotation(Column.class);
               if(ann != null)
                  nombreCampo = ann.name();
               else
                  nombreCampo = field.getAnnotation(JoinColumn.class).name();

               if(numeroValorBuscado.intValue() == 0)
                  outputWhere.append(String.format("WHERE %s.%s='%s'", clase.getKey(), nombreCampo, valorBuscado));
               else
                  outputWhere.append(String.format("WHERE %s.%s=%s", clase.getKey(), nombreCampo, numeroValorBuscado.toString()));
               break;
            } catch (NoSuchFieldException e) {
               // e.printStackTrace();
            }
         }
      }

      return outputWhere.toString();
   }

   private static String obtenerNombreColumnaID(Class<?> clazz) {
      Field[] fields = clazz.getDeclaredFields();
      for (Field field : fields) {
         if (field.isAnnotationPresent(Id.class))
            return field.getAnnotation(Column.class).name();
      }
      return "";
   }

   private static String obtenerNombreCampoID(Class<?> clazz) {
      Field[] fields = clazz.getDeclaredFields();
      for (Field field : fields) {
         if (field.isAnnotationPresent(Id.class))
            return field.getName();
      }
      return "";
   }


   private static List<Field> buscarAutoreferencias(Class<?> clazz){
      Field[] fields = clazz.getDeclaredFields();
      List<Field> autoReferencias = new ArrayList<>(), autoReferenciasAux;
      String table = clazz.getAnnotation(Table.class).name();

      for (Field field : fields) {
         if (field.isAnnotationPresent(JoinColumn.class)) {
            Class<?> tipoField = field.getType();
            String tablaTipoField = tipoField.getAnnotation(Table.class).name();

            if (!table.equals(tablaTipoField)){
               autoReferenciasAux = buscarAutoreferencias(tipoField);

               autoReferencias.addAll(autoReferenciasAux);
            }
            else autoReferencias.add(field);
         }
      }
      return autoReferencias;
   }

   private String getFinalQuery(){
      String finalQuery = String.format("SELECT * FROM %s AS %s ", tabla, nombreAlias) + parsearJoins() +
              parsearWhere();
      for (Map.Entry<String, String> parameter : map.entrySet())
         finalQuery = finalQuery.replace(parameter.getKey(), parameter.getValue());
      return finalQuery;
   }

   private boolean existenParametrosIndefinidos(){
      return map.containsValue("");
   }

   private static void avanzarResultado(QueryResult qr){
      try {
         qr.rs.next();
      } catch (SQLException throwables) {
         throwables.printStackTrace();
      }
   }

   private QueryResult getQueryResult(DatabaseManager db) {
      String q = getFinalQuery();
      if(!esValido()) {
         System.err.println("Query no valido: " + q);
         q = "";
      }
      return new QueryResult(db.query(q), autoreferencias);
   }

   public void setParameter(String pName,Object pValue)
   {
      assert pValue != null;
      Class<?> clazz = pValue.getClass();
      String valorReemplazo = "";
      pName = ":" + pName;

      if(pValue instanceof String)
         valorReemplazo = (String)pValue;
      else {
         Field[] fields = clazz.getFields();
         for (Field field : fields) {
            if (field.isAnnotationPresent(Id.class)) {
               try {
                  valorReemplazo = field.get(pValue).toString();
                  break;
               } catch (IllegalAccessException e) {
                  e.printStackTrace();
               }
            }
         }
      }

      if(map.containsKey(pName))
         map.put(pName, valorReemplazo);
   }

   public <T> List<T> getResultList(DatabaseManager db) {
      QueryResult qr = getQueryResult(db);

      List<T> listaObjetosRetorno = new ArrayList<>();
      T objetoRetorno;
      do{
         avanzarResultado(qr);
         objetoRetorno = ConstructorObjetos.construir(claseObjetivo, qr);
         if(objetoRetorno != null)
            listaObjetosRetorno.add(objetoRetorno);
      } while (objetoRetorno != null);
      return listaObjetosRetorno;
   }

   public <T> T getSingleResult(DatabaseManager db) {
      QueryResult qr = getQueryResult(db);
      avanzarResultado(qr);
      return ConstructorObjetos.construir(claseObjetivo, qr);
   }
}
