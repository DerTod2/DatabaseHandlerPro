package net.dertod2.DatabaseHandlerPro.Data;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains all included types in this database handler plugin
 */
public enum IncludedTypes {
    Byte("byte", Byte.class.getName()),
    Short("short", Short.class.getName()),
    Int("int", Integer.class.getName()),
    Long("long", Long.class.getName()),
    Float("float", Float.class.getName()),
    Double("double", Double.class.getName()),
    Char("char", Character.class.getName()),
    String(null, String.class.getName()),
    Boolean("boolean", Boolean.class.getName()),
    Timestamp(null, Timestamp.class.getName()),
    Unknown(null, null);

    private static final IncludedTypes[] values;

    static {
        List<IncludedTypes> list = new ArrayList<>();

        for (IncludedTypes primitiveWrapper : values()) {
            if (primitiveWrapper != Unknown) {
                list.add(primitiveWrapper);
            }
        }

        values = list.toArray(new IncludedTypes[0]);
    }

    private final String primitiveIdentifier;
    private final String objectIdentifier;

    IncludedTypes(String primitiveIdentifier, String objectIdentifier) {
        this.primitiveIdentifier = primitiveIdentifier;
        this.objectIdentifier = objectIdentifier;
    }

    public static IncludedTypes getByPrimitive(String primitive) {
        for (IncludedTypes primitiveWrapper : values) {
            if (primitiveWrapper.primitiveIdentifier.equals(primitive)) {
                return primitiveWrapper;
            }
        }

        return Unknown;
    }

    public static IncludedTypes getByObject(String object) {
        if (object == null) return Unknown;

        if (!object.contains(".")) object = RowHelper.getPrimitiveObject(object);
        for (IncludedTypes primitiveWrapper : values) {
            if (primitiveWrapper.objectIdentifier.equals(object)) {
                return primitiveWrapper;
            }
        }

        return Unknown;
    }

    public String getPrimitive() {
        return this.primitiveIdentifier;
    }

    public String getObject() {
        return this.objectIdentifier;
    }

    public String getName() {
        return name();
    }

}
