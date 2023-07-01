package net.dertod2.DatabaseHandlerPro.Data;

import com.google.common.collect.ImmutableList;
import net.dertod2.DatabaseHandlerPro.Database.AbstractDatabase;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public abstract class Handler {
    protected final AbstractDatabase abstractDatabase;
    protected final UUID handlerUniqueId;

    public Handler(AbstractDatabase abstractDatabase) {
        this.abstractDatabase = abstractDatabase;
        this.handlerUniqueId = UUID.randomUUID();
    }

    /**
     * Inserts a single row into the database
     * @param tableRow The element to insert
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws SQLException
     * @throws IOException
     */
    public abstract void insert(TableRow tableRow) throws IllegalArgumentException, IllegalAccessException, SQLException, IOException;

    /**
     * Inserts many rows at the same time into the database. Some databases supports faster adding of multiple rows at once
     * @param entries The list of elements
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws SQLException
     * @throws IOException
     */
    public abstract <T extends TableRow> void insert(List<T> entries) throws IllegalArgumentException, IllegalAccessException, SQLException, IOException;

    /**
     * Removes the given row out of the database. Works only for tables with primary keys
     * @param tableRow The element to remove out of the database
     * @return Wherever the element was removed or not
     * @throws SQLException
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public boolean remove(TableRow tableRow) throws SQLException, IOException, IllegalArgumentException, IllegalAccessException {
        TableCache tableCache = TableCache.getCache(tableRow.getClass(), this);

        if (tableCache.hasPrimaryKey()) {
            return remove(tableRow.getClass(), (new LoadHelper()).filter(tableCache.getName(tableCache.getPrimaryKey()), tableRow.getColumn(tableCache.getPrimaryKey())));
        }

        return false;
    }

    /**
     * Removes all matching rows (by the filter) out of the database table given with the tableRow
     * @param tableRow The Table Type
     * @param loadHelper The Filter to delete only specific elements
     * @return Wherever the operation was successful or not
     * @throws SQLException
     * @throws IOException
     */
    public abstract <T extends TableRow> boolean remove(Class<T> tableRow, LoadHelper loadHelper) throws SQLException, IOException;

    /**
     * Updates the row in the database to match the new data out of the tableRow argument. Works only for tables with primary keys.
     * @param tableRow The already loaded element with new data
     * @return Wherever the operation was successful or not
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws SQLException
     * @throws IOException
     */
    public boolean update(TableRow tableRow) throws IllegalArgumentException, IllegalAccessException, SQLException, IOException {
        if (!tableRow.isLoaded) return false;

        TableCache tableCache = TableCache.getCache(tableRow.getClass(), this);

        LoadHelper loadHelper = new LoadHelper();
        if (tableCache.hasPrimaryKey()) {
            loadHelper.filter(tableCache.getName(tableCache.getPrimaryKey()), tableRow.getColumn(tableCache.getPrimaryKey()));
        } else if (tableCache.hasUniqueKeys()) {
            for (Column column : tableCache.getUniqueKeys()) {
                loadHelper.filter(tableCache.getName(column), tableRow.getColumn(column));
            }
        }

        return update(tableRow, loadHelper);
    }

    /**
     * Updates all matching rows inside a database table given by tableRow and the filter
     * @param tableRow The element with the changed data
     * @param loadHelper The Filter to update only specific elements
     * @return Wherever the operation was successful or not
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws SQLException
     * @throws IOException
     */
    public boolean update(TableRow tableRow, LoadHelper loadHelper) throws IllegalArgumentException, IllegalAccessException, SQLException, IOException {
        return update(tableRow, loadHelper, ImmutableList.of());
    }

    /**
     * Updates only the given rows inside an database table matching all rows by the filter
     * @param tableRow The Table Type
     * @param loadHelper The Filter to update only specific elements
     * @param rows A list with all rows to update. Ignores all other rows inside the table
     * @return Wherever the operation was successful or not
     * @throws SQLException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws IOException
     */
    public abstract boolean update(TableRow tableRow, LoadHelper loadHelper, List<String> rows) throws SQLException, IllegalArgumentException, IllegalAccessException, IOException;

    /**
     * Updates only the given rows to the given data in content that's matching the filter
     * @param tableRow The Table Type
     * @param loadHelper The Filter to update only specific elements
     * @param content A Map with all row names and the corresponding data
     * @return Wherever the operation was successful or not
     * @throws Exception
     */
    public abstract boolean update(TableRow tableRow, LoadHelper loadHelper, Map<String, Object> content) throws Exception;

    /**
     * Loads the last added element out of the given table
     * @param tableRow The Table Type
     * @return The loaded element or null
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws SecurityException
     * @throws SQLException
     * @throws IOException
     */
    public <T extends TableRow> T loadLast(Class<T> tableRow) throws IllegalArgumentException, IllegalAccessException, InstantiationException, SecurityException, SQLException, IOException {
        TableCache tableCache = TableCache.getCache(tableRow, this);
        if (tableCache.hasPrimaryKey()) {
            List<T> list = load(tableRow, (new LoadHelper()).limit(1).sort(tableCache.getName(tableCache.getPrimaryKey()), LoadHelper.Sort.DESC));

            return (list.size() > 0) ? list.get(0) : null;
        }

        return null;
    }

    /**
     * Loads the first ever added still available element out of the given table
     * @param tableRow The Table Type
     * @return The loaded element or null
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws SecurityException
     * @throws SQLException
     * @throws IOException
     */
    public <T extends TableRow> T loadFirst(Class<T> tableRow) throws IllegalArgumentException, IllegalAccessException, InstantiationException, SecurityException, SQLException, IOException {
        TableCache tableCache = TableCache.getCache(tableRow, this);
        if (tableCache.hasPrimaryKey()) {
            List<T> list = load(tableRow, (new LoadHelper()).limit(1).sort(tableCache.getName(tableCache.getPrimaryKey()), LoadHelper.Sort.ASC));

            return (list.size() > 0) ? list.get(0) : null;
        }

        return null;
    }

    /**
     * Loads one element out of the table found with the filter
     * @param tableRow The Table Type
     * @param loadHelper The Filter to search specific elements
     * @return The loaded element or null
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws SecurityException
     * @throws SQLException
     * @throws IOException
     */
    public <T extends TableRow> T loadOne(Class<T> tableRow, LoadHelper loadHelper) throws IllegalArgumentException, IllegalAccessException, InstantiationException, SecurityException, SQLException, IOException {
        loadHelper.limit = 1;

        List<T> list = load(tableRow, loadHelper);
        return (list.size() > 0) ? list.get(0) : null;
    }

    /**
     * Loads all elements out of a table
     * @param tableRow The Table Type
     * @return A List with all loaded elements
     * @throws SQLException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws SecurityException
     * @throws IOException
     */
    public <T extends TableRow> List<T> load(Class<T> tableRow) throws SQLException, IllegalArgumentException, IllegalAccessException, InstantiationException, SecurityException, IOException {
        return load(tableRow, new LoadHelper());
    }

    /**
     * Loads all elements out of the table matching the loadHelper
     * @param tableRow The Table Type
     * @param loadHelper The Filter to search specific elements
     * @return A List with all matching elements
     * @throws SQLException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws SecurityException
     * @throws IOException
     */
    public abstract <T extends TableRow> List<T> load(Class<T> tableRow, LoadHelper loadHelper) throws SQLException, IllegalArgumentException, IllegalAccessException, InstantiationException, SecurityException, IOException;

    /**
     * Checks wherever a table exists inside the database
     * @param tableRow The Table Type
     * @return The result of the search
     * @throws SQLException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws IOException
     */
    public abstract boolean exist(TableRow tableRow) throws SQLException, IllegalArgumentException, IllegalAccessException, IOException;

    /**
     * Returns the amount of elements inside an table that are matching the given loadHelper
     * @param tableRow The Table Type
     * @param loadHelper The Filter to search specific elements
     * @return The number of found matching elements
     * @throws SQLException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws IOException
     */
    public abstract <T extends TableRow> long count(Class<T> tableRow, LoadHelper loadHelper) throws SQLException, IllegalArgumentException, IllegalAccessException, IOException;

    /**
     * Updates the table inside the database to match the layout
     * @param tableCache The cache file of the table
     * @throws SQLException
     */
    protected abstract void updateTable(TableCache tableCache) throws SQLException;

}
