package net.dertod2.DatabaseHandlerPro.Data;

import net.dertod2.DatabaseHandlerPro.Exceptions.UnsupportedMapDepthException;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;


public class RowHelper {
    protected static String[] extractFieldTypes(Field field) {
        String name = field.getType().getName();
        Type genericType = field.getGenericType();

        if (genericType instanceof ParameterizedType) {
            Type[] arguments = ((ParameterizedType) genericType).getActualTypeArguments();

            if (arguments.length == 1)
                return new String[]{name, arguments[0].getTypeName()};
            if (arguments.length == 2) {
                if (arguments[1] instanceof ParameterizedType) {
                    throw new UnsupportedMapDepthException();
                }
                return new String[]{name, arguments[0].getTypeName(), arguments[1].getTypeName()};
            }

            throw new UnsupportedMapDepthException();
        }

        return new String[]{getPrimitiveObject(name)};
    }


    public static String getPrimitiveObject(String name) {
        switch (name) {
            case "boolean":
            case "byte":
            case "char":
            case "double":
            case "float":
            case "int":
            case "long":
            case "short":
        }

        return name;
    }

}
