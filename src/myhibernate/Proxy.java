package myhibernate;

import myhibernate.ann.JoinColumn;
import myhibernate.demo.Demo;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.SuperMethodCall;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class Proxy {
    private static final Map<Class<?>, Class<?>> clasesMejoradas = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> T generar(Class<?> claseBase){
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
}
