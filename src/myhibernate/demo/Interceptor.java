package myhibernate.demo;

import myhibernate.MyHibernate;
import myhibernate.ann.Column;
import myhibernate.ann.Id;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Field;

import static java.lang.Math.abs;

public class Interceptor {
    public static void intercept(Field field, @This Object self) {
        field.setAccessible(true);

        try {
            Object campo = field.get(self);
            Class<?> type = field.getType();
            int id;
            if(campo != null)
                id = encontrarId(campo);
            else return;

            if(id < 0) {
                Object o = MyHibernate.find(type, abs(id));
                field.set(self, o);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int encontrarId(Object o) {
        Field[] fields = o.getClass().getFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                if (field.isAnnotationPresent(Id.class)) {
                    try {
                        return (int) field.get(o);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return -1;
    }
}
