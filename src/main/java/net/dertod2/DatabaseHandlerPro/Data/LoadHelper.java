package net.dertod2.DatabaseHandlerPro.Data;

import com.google.common.collect.ImmutableList;
import net.dertod2.DatabaseHandlerPro.Database.DatabaseType;
import net.dertod2.DatabaseHandlerPro.Exceptions.FilterNotAllowedException;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoadHelper {
    protected final Map<String, List<Object>> between = new HashMap<>();
    protected final Map<String, Sort> columnSorter = new HashMap<>();
    protected int limit = 0;
    protected int offset = 0;
    protected Map<String, Integer> length = new HashMap<>();
    protected Map<String, Filter> lengthType = new HashMap<>();
    protected Map<String, Object> filter = new HashMap<>();
    protected Map<String, Filter> filterType = new HashMap<>();
    protected List<String> groupBy = new ArrayList<>();

    public LoadHelper limit(int limit) {
        this.limit = limit;
        return this;
    }

    public LoadHelper limit(int limit, int offset) {
        this.limit = limit;
        this.offset = offset;
        return this;
    }

    public LoadHelper offset(int offset) {
        this.offset = offset;
        return this;
    }

    public LoadHelper sort(String field) {
        this.columnSorter.put(field, Sort.ASC);
        return this;
    }

    public LoadHelper sort(String field, Sort sortOrder) {
        this.columnSorter.put(field, sortOrder);
        return this;
    }

    public LoadHelper length(String field, int length) {
        return length(field, length, Filter.Equals);
    }

    public LoadHelper length(String field, int length, Filter filter) {
        if (!filter.allowedLength) {
            throw new FilterNotAllowedException("length", filter);
        }
        this.length.put(field, Integer.valueOf(length));
        this.lengthType.put(field, filter);


        return this;
    }

    public LoadHelper filter(String field, Object value) {
        return filter(field, value, Filter.Equals);
    }

    public LoadHelper filter(String field, Object value, Filter filter) {
        this.filterType.put(field, filter);
        this.filter.put(field, value);

        return this;
    }

    public LoadHelper group(String field) {
        this.groupBy.add(field);
        return this;
    }

    public LoadHelper between(String field, Date start, Date end) {
        return between(field, new Timestamp(start.getTime()), new Timestamp(end.getTime()));
    }

    public LoadHelper between(String field, Timestamp start, Timestamp end) {
        this.between.put(field, ImmutableList.builder().add(start).add(end).build());
        return this;
    }

    public LoadHelper between(String field, Number start, Number end) {
        this.between.put(field, ImmutableList.builder().add(start).add(end).build());
        return this;
    }

    public String buildWhereQueue(DatabaseType databaseType) {
        if (this.filter.isEmpty() && this.between.isEmpty()) return "";
        StringBuilder stringBuilder = new StringBuilder();


        boolean isPostgres = (databaseType == DatabaseType.PostGRE);

        for (String columnName : this.filter.keySet()) {
            if (stringBuilder.length() > 0) stringBuilder.append(" AND ");

            stringBuilder.append("`").append(columnName).append("`");
            stringBuilder.append(" ").append(this.filterType.get(columnName).getFilter()).append(" ?");
        }

        for (String columnName : this.length.keySet()) {
            if (stringBuilder.length() > 0) stringBuilder.append(" AND ");
            stringBuilder.append("length(").append(columnName).append(")");

            stringBuilder.append(" ").append(this.lengthType.get(columnName).getFilter()).append(" ?");
        }

        for (String columnName : this.between.keySet()) {
            if (stringBuilder.length() > 0) stringBuilder.append(" AND ");
            stringBuilder.append("`").append(columnName).append("`");

            stringBuilder.append(" BETWEEN ? AND ?");
        }

        stringBuilder.insert(0, " WHERE ");
        if (isPostgres) {
            return stringBuilder.toString().replace("`", "");
        }
        return stringBuilder.toString();
    }

    public enum Sort {
        DESC("DESC"),
        ASC("ASC");

        private final String sort;

        Sort(String sort) {
            this.sort = sort;
        }

        public String getSort() {
            return this.sort;
        }
    }

    public enum Filter {
        Equals("=", true),
        GreaterThan(">", true),
        LessThan("<", true),
        GreaterEquals(">=", true),
        LessEquals("<=", true),
        NotEquals("!=", true),
        Like("LIKE", false),
        ILike("ILIKE", false);

        private final String filter;

        private final boolean allowedLength;

        Filter(String filter, boolean allowedLength) {
            this.filter = filter;
            this.allowedLength = allowedLength;
        }

        public String getFilter() {
            return this.filter;
        }

        public boolean isAllowedLength() {
            return this.allowedLength;
        }
    }

}
