package net.dertod2.DatabaseHandlerPro.Data;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public abstract class TableRow {
    boolean isLoaded = false;

    public boolean isLoaded() {
        return this.isLoaded;
    }


    public void setLoaded() {
        this.isLoaded = true;
    }


    Object getColumn(Column annotation) throws IllegalArgumentException, IllegalAccessException {
        Map<Column, Field> fields = TableCache.getCache(getClass()).getColumnFields();

        for (Map.Entry<Column, Field> entry : fields.entrySet()) {
            if (entry.getKey().equals(annotation)) return entry.getValue().get(this);

        }
        return null;
    }

    Map<Column, Object> getColumns() throws IllegalArgumentException, IllegalAccessException {
        Map<Column, Field> fields = TableCache.getCache(getClass()).getColumnFields();
        Map<Column, Object> data = new HashMap<>();

        for (Map.Entry<Column, Field> entry : fields.entrySet()) {
            data.put(entry.getKey(), entry.getValue().get(this));
        }

        return data;
    }

    void setColumn(Column annotation, Object value) throws IllegalArgumentException, IllegalAccessException {
        Map<Column, Field> fields = TableCache.getCache(getClass()).getColumnFields();

        for (Map.Entry<Column, Field> entry : fields.entrySet()) {
            if (entry.getKey().equals(annotation)) {
                if (entry.getValue().isEnumConstant()) {
                    entry.getValue().set(this, Enum.valueOf(entry.getValue().getType().asSubclass(Enum.class), (String) value));
                } else {
                    entry.getValue().set(this, value);
                }
                entry.getValue().set(this, value);
                return;
            }
        }
    }

}
