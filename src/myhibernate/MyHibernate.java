package myhibernate;

import java.util.List;
import java.lang.System;

public abstract class MyHibernate {
    public static final DatabaseManager db = new DatabaseManager(System.getenv("DB_URL"), "sa", "");

    public static <T> T find(Class<T> clazz, int id) {
        return Query.fromClass(clazz, id).getSingleResult(db);
    }

    public static <T> List<T> findAll(Class<T> clazz) {
        return Query.fromClass(clazz, -1).getResultList(db);
    }

    public static Query createQuery(String hql) {
        return new Query(hql);
    }
}
