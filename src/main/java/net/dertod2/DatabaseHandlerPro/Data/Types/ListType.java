package net.dertod2.DatabaseHandlerPro.Data.Types;


import com.google.gson.*;
import net.dertod2.DatabaseHandlerPro.Data.IncludedTypes;
import net.dertod2.DatabaseHandlerPro.Exceptions.UnhandledDataTypeException;

import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ListType extends AbstractType {
    private static final String OLD_REGEX = "¶";
    private static final String OLD_NULL = "NULL";
    private static final String EMPTY_LIST = (new Gson()).toJson(new JsonArray());


    public ListType() {
        super(List.class.getName(), ArrayList.class.getName());
    }

    public String setResult(Object value) {
        if (value == null) return EMPTY_LIST;


        List<Object> list = (List<Object>) value;
        if (list.size() == 0) return EMPTY_LIST;

        String type = (list.get(0) == null) ? null : list.get(0).getClass().getTypeName();
        boolean customType = (IncludedTypes.getByObject(type) == IncludedTypes.Unknown);

        JsonArray jsonArray = new JsonArray();

        if (customType) {
            for (Object object : list) {
                if (object == null) {
                    jsonArray.add(JsonNull.INSTANCE);
                    continue;
                }

                AbstractType abstractType = this.abstractDatabase.getDataType(type);
                if (abstractType != null) {
                    jsonArray.add(abstractType.setResult(object));
                    continue;
                }
                throw new UnhandledDataTypeException(object.getClass());
            }

        } else {

            for (Object object : list) {
                if (object == null) {
                    jsonArray.add(JsonNull.INSTANCE);
                    continue;
                }
                if (object instanceof Boolean) {
                    jsonArray.add((Boolean) object);
                    continue;
                }
                if (object instanceof Number) {
                    jsonArray.add((Number) object);
                    continue;
                }
                if (object instanceof Character) {
                    jsonArray.add((Character) object);
                    continue;
                }
                if (object instanceof String) {
                    jsonArray.add(object.toString());
                    continue;
                }
                if (object instanceof Timestamp) {
                    jsonArray.add(object.toString());
                }
            }
        }

        return (new Gson()).toJson(jsonArray);
    }

    public Object getResult(String value, Type[] genericTypes) {
        IncludedTypes primitiveWrapper = (genericTypes != null && genericTypes[0] != null) ? IncludedTypes.getByObject(genericTypes[0].getTypeName()) : IncludedTypes.String;

        List<Object> list = new ArrayList<>();
        if (value == null || value.length() <= 0 || value.equalsIgnoreCase("NULL")) return list;

        if (!value.contains("[") && !value.contains("]")) {
            AbstractType abstractType;
            String[] splitter = value.split("¶");
            switch (primitiveWrapper) {
                case Boolean -> {
                    for (String split : splitter) {
                        if (split.equals("NULL")) {
                            list.add(null);
                        } else {

                            list.add(Boolean.valueOf(split));
                        }
                    }
                }
                case Byte -> {
                    for (String split : splitter) {
                        if (split.equals("NULL")) {
                            list.add(null);
                        } else {

                            list.add(Byte.valueOf(split));
                        }
                    }
                }
                case Char -> {
                    for (String split : splitter) {
                        if (split.equals("NULL")) {
                            list.add(null);
                        } else {

                            list.add(split.charAt(0));
                        }
                    }
                }
                case Double -> {
                    for (String split : splitter) {
                        if (split.equals("NULL")) {
                            list.add(null);
                        } else {

                            list.add(Double.valueOf(split));
                        }
                    }
                }
                case Float -> {
                    for (String split : splitter) {
                        if (split.equals("NULL")) {
                            list.add(null);
                        } else {

                            list.add(Float.valueOf(split));
                        }
                    }
                }
                case Int -> {
                    for (String split : splitter) {
                        if (split.equals("NULL")) {
                            list.add(null);
                        } else {

                            list.add(Integer.valueOf(split));
                        }
                    }
                }
                case Long -> {
                    for (String split : splitter) {
                        if (split.equals("NULL")) {
                            list.add(null);
                        } else {

                            list.add(Long.valueOf(split));
                        }
                    }
                }
                case Short -> {
                    for (String split : splitter) {
                        if (split.equals("NULL")) {
                            list.add(null);
                        } else {

                            list.add(Short.valueOf(split));
                        }
                    }
                }
                case String -> {
                    for (String split : splitter) {
                        if (split.equals("NULL")) {
                            list.add(null);
                        } else {

                            list.add(split);
                        }
                    }
                }
                case Timestamp -> {
                    for (String split : splitter) {
                        if (split.equals("NULL")) {
                            list.add(null);
                        } else {

                            list.add(Timestamp.valueOf(split));
                        }
                    }
                }
                case Unknown -> {
                    abstractType = this.abstractDatabase.getDataType(genericTypes[0].getTypeName());
                    for (String split : splitter) {
                        if (split.equals("NULL")) {
                            list.add(null);
                        } else {

                            list.add(abstractType.getResult(split, genericTypes));
                        }
                    }
                }
            }
            return list;
        }

        JsonElement jsonMain = JsonParser.parseString(value);

        JsonArray jsonArray = jsonMain.getAsJsonArray();
        Iterator<JsonElement> iterator = jsonArray.iterator();

        if (primitiveWrapper == IncludedTypes.Unknown) {
            AbstractType abstractType = this.abstractDatabase.getDataType(genericTypes[0].getTypeName());
            while (iterator.hasNext()) {
                JsonElement jsonElement = iterator.next();
                list.add(jsonElement.isJsonNull() ? null : abstractType.getResult(jsonElement.getAsString(), (genericTypes.length > 1) ? Arrays.copyOfRange(genericTypes, 1, genericTypes.length) : null));
            }
        } else {
            switch (primitiveWrapper) {
                case Boolean -> {
                    while (iterator.hasNext()) {
                        JsonElement jsonElement = iterator.next();
                        list.add(jsonElement.isJsonNull() ? null : jsonElement.getAsBoolean());
                    }
                }
                case Byte -> {
                    while (iterator.hasNext()) {
                        JsonElement jsonElement = iterator.next();
                        list.add(jsonElement.isJsonNull() ? null : jsonElement.getAsByte());
                    }
                }
                case Char -> {
                    while (iterator.hasNext()) {
                        JsonElement jsonElement = iterator.next();
                        list.add(jsonElement.isJsonNull() ? null : jsonElement.getAsCharacter());
                    }
                }
                case Double -> {
                    while (iterator.hasNext()) {
                        JsonElement jsonElement = iterator.next();
                        list.add(jsonElement.isJsonNull() ? null : jsonElement.getAsDouble());
                    }
                }
                case Float -> {
                    while (iterator.hasNext()) {
                        JsonElement jsonElement = iterator.next();
                        list.add(jsonElement.isJsonNull() ? null : jsonElement.getAsFloat());
                    }
                }
                case Int -> {
                    while (iterator.hasNext()) {
                        JsonElement jsonElement = iterator.next();
                        list.add(jsonElement.isJsonNull() ? null : jsonElement.getAsInt());
                    }
                }
                case Long -> {
                    while (iterator.hasNext()) {
                        JsonElement jsonElement = iterator.next();
                        list.add(jsonElement.isJsonNull() ? null : jsonElement.getAsLong());
                    }
                }
                case Short -> {
                    while (iterator.hasNext()) {
                        JsonElement jsonElement = iterator.next();
                        list.add(jsonElement.isJsonNull() ? null : jsonElement.getAsShort());
                    }
                }
                case String -> {
                    while (iterator.hasNext()) {
                        JsonElement jsonElement = iterator.next();
                        list.add(jsonElement.isJsonNull() ? null : jsonElement.getAsString());
                    }
                }
                case Timestamp -> {
                    while (iterator.hasNext()) {
                        JsonElement jsonElement = iterator.next();
                        list.add(jsonElement.isJsonNull() ? null : Timestamp.valueOf(jsonElement.getAsString()));
                    }
                }
            }


        }
        return list;
    }
}
