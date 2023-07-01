package net.dertod2.DatabaseHandlerPro.Data.Types;

import com.google.gson.*;
import net.dertod2.DatabaseHandlerPro.Data.IncludedTypes;
import net.dertod2.DatabaseHandlerPro.Exceptions.UnhandledDataTypeException;

import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MapType extends AbstractType {
    private static final String EMPTY_MAP;

    static {
        JsonArray mainArray = new JsonArray();

        JsonArray jsonArray = new JsonArray();
        jsonArray.add(JsonNull.INSTANCE);
        jsonArray.add(JsonNull.INSTANCE);

        mainArray.add(jsonArray);

        EMPTY_MAP = (new Gson()).toJson(mainArray);
    }

    public MapType() {
        super(Map.class.getName(), HashMap.class.getName());
    }

    public String setResult(Object value) {
        if (value == null) return EMPTY_MAP;


        Map<Object, Object> map = (Map<Object, Object>) value;
        if (map.size() <= 0) return EMPTY_MAP;

        String keyType = null;
        String valueType = null;

        boolean customKey = false;
        boolean customValue = false;

        JsonArray mainArray = new JsonArray();
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            JsonArray jsonArray = new JsonArray();
            Object key = entry.getKey();
            Object val = entry.getValue();

            if (keyType == null) {
                keyType = (key == null) ? null : key.getClass().getTypeName();
                customKey = (IncludedTypes.getByObject(keyType) == IncludedTypes.Unknown);
            }

            if (valueType == null) {
                valueType = (val == null) ? null : val.getClass().getTypeName();
                customValue = (IncludedTypes.getByObject(valueType) == IncludedTypes.Unknown);
            }


            if (customKey) {
                if (key == null) {
                    jsonArray.add(JsonNull.INSTANCE);
                } else {
                    AbstractType abstractType = this.abstractDatabase.getDataType(keyType);
                    if (abstractType != null) {
                        jsonArray.add(abstractType.setResult(key));
                    } else {
                        throw new UnhandledDataTypeException(key.getClass());
                    }
                }

            } else if (key == null) {
                jsonArray.add(JsonNull.INSTANCE);
            } else if (key instanceof Boolean) {
                jsonArray.add((Boolean) key);
            } else if (key instanceof Number) {
                jsonArray.add((Number) key);
            } else if (key instanceof Character) {
                jsonArray.add((Character) key);
            } else if (key instanceof String) {
                jsonArray.add(key.toString());
            } else if (key instanceof Timestamp) {
                jsonArray.add(key.toString());
            }


            if (customValue) {
                if (val == null) {
                    jsonArray.add(JsonNull.INSTANCE);
                } else {
                    AbstractType abstractType = this.abstractDatabase.getDataType(valueType);
                    if (abstractType != null) {
                        jsonArray.add(abstractType.setResult(val));
                    } else {
                        throw new UnhandledDataTypeException(val.getClass());
                    }
                }

            } else if (val == null) {
                jsonArray.add(JsonNull.INSTANCE);
            } else if (val instanceof Boolean) {
                jsonArray.add((Boolean) val);
            } else if (val instanceof Number) {
                jsonArray.add((Number) val);
            } else if (val instanceof Character) {
                jsonArray.add((Character) val);
            } else if (val instanceof String) {
                jsonArray.add((String) val);
            } else if (val instanceof Timestamp) {
                jsonArray.add(val.toString());
            }


            mainArray.add(jsonArray);
        }

        return (new Gson()).toJson(mainArray);
    }

    public Object getResult(String value, Type[] genericTypes) {
        Map<Object, Object> map = new HashMap<>();
        if (value == null || value.length() == 0) return map;

        JsonArray mainArray = JsonParser.parseString(value).getAsJsonArray();
        Iterator<JsonElement> iterator = mainArray.iterator();

        String keyType = genericTypes[0].getTypeName();
        String valueType = genericTypes[1].getTypeName();

        IncludedTypes customKey = IncludedTypes.getByObject(keyType);
        IncludedTypes customValue = IncludedTypes.getByObject(valueType);

        while (iterator.hasNext()) {
            JsonArray jsonArray = iterator.next().getAsJsonArray();

            JsonElement jsonKey = jsonArray.get(0);
            JsonElement jsonValue = jsonArray.get(1);

            Object key = null;
            Object val = null;

            if (customKey == IncludedTypes.Unknown) {
                AbstractType abstractKey = this.abstractDatabase.getDataType(keyType);
                key = jsonKey.isJsonNull() ? null : abstractKey.getResult(jsonKey.getAsString(), (genericTypes.length > 1) ? Arrays.copyOfRange(genericTypes, 1, genericTypes.length) : null);
            } else {
                key = switch (customKey) {
                    case Boolean -> jsonKey.isJsonNull() ? null : jsonKey.getAsBoolean();
                    case Byte -> jsonKey.isJsonNull() ? null : jsonKey.getAsByte();
                    case Char -> jsonKey.isJsonNull() ? null : jsonKey.getAsCharacter();
                    case Double -> jsonKey.isJsonNull() ? null : jsonKey.getAsDouble();
                    case Float -> jsonKey.isJsonNull() ? null : jsonKey.getAsFloat();
                    case Int -> jsonKey.isJsonNull() ? null : jsonKey.getAsInt();
                    case Long -> jsonKey.isJsonNull() ? null : jsonKey.getAsLong();
                    case Short -> jsonKey.isJsonNull() ? null : jsonKey.getAsShort();
                    case String -> jsonKey.isJsonNull() ? null : jsonKey.getAsString();
                    case Timestamp -> jsonKey.isJsonNull() ? null : Timestamp.valueOf(jsonKey.getAsString());
                    default -> key;
                };


            }
            if (customValue == IncludedTypes.Unknown) {
                AbstractType abstractKey = this.abstractDatabase.getDataType(valueType);
                val = jsonValue.isJsonNull() ? null : abstractKey.getResult(jsonValue.getAsString(), (genericTypes.length > 1) ? Arrays.copyOfRange(genericTypes, 1, genericTypes.length) : null);
            } else {
                val = switch (customKey) {
                    case Boolean -> jsonValue.isJsonNull() ? null : jsonValue.getAsBoolean();
                    case Byte -> jsonValue.isJsonNull() ? null : jsonValue.getAsByte();
                    case Char -> jsonValue.isJsonNull() ? null : jsonValue.getAsCharacter();
                    case Double -> jsonValue.isJsonNull() ? null : jsonValue.getAsDouble();
                    case Float -> jsonValue.isJsonNull() ? null : jsonValue.getAsFloat();
                    case Int -> jsonValue.isJsonNull() ? null : jsonValue.getAsInt();
                    case Long -> jsonValue.isJsonNull() ? null : jsonValue.getAsLong();
                    case Short -> jsonValue.isJsonNull() ? null : jsonValue.getAsShort();
                    case String -> jsonValue.isJsonNull() ? null : jsonValue.getAsString();
                    case Timestamp -> jsonValue.isJsonNull() ? null : Timestamp.valueOf(jsonValue.getAsString());
                    default -> val;
                };


            }
            map.put(key, val);
        }

        return map;
    }

}
