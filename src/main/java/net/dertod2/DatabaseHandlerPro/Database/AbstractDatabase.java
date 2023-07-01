package net.dertod2.DatabaseHandlerPro.Database;

import net.dertod2.DatabaseHandlerPro.Data.Handler;
import net.dertod2.DatabaseHandlerPro.Data.MySQLHandler;
import net.dertod2.DatabaseHandlerPro.Data.PostGREHandler;
import net.dertod2.DatabaseHandlerPro.Data.SQLiteHandler;
import net.dertod2.DatabaseHandlerPro.Data.Types.AbstractType;
import net.dertod2.DatabaseHandlerPro.Data.Types.ListType;
import net.dertod2.DatabaseHandlerPro.Data.Types.MapType;
import net.dertod2.DatabaseHandlerPro.Data.Types.UniqueIdType;

import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public abstract class AbstractDatabase {
    public final Logger logger;
    protected final String host;
    protected final Integer port;
    protected final String database;
    protected final String username;
    protected final String password;
    private final Map<String, AbstractType> types;
    protected Handler handler;

    public AbstractDatabase(String host, Integer port, String database, String username, String password) {
        this.logger = Logger.getLogger("DatabaseHandler");

        this.host = host;
        this.port = port;

        this.database = database;
        this.username = username;
        this.password = password;

        this.types = new HashMap<>();

        addDataType(new UniqueIdType(), true);
        addDataType(new ListType(), true);
        addDataType(new MapType(), true);
    }

    public abstract DatabaseType getType();

    public String getDatabaseName() {
        return this.database;
    }

    public AbstractType getDataType(String classPath) {
        if (classPath.contains("<")) return this.types.get(classPath.substring(0, classPath.indexOf("<")));
        return this.types.get(classPath);
    }

    private void addDataType(AbstractType abstractType, boolean silent) {
        abstractType.database(this);

        for (String classPath : abstractType.getClassPath()) {
            this.types.put(classPath, abstractType);
        }

        if (!silent)
            this.logger.info(String.format("Injected custom type '%1$s' into '%2$s' database handler.", Arrays.toString((Object[]) abstractType.getClassPath()), getType().name()));
    }

    public void addDataType(AbstractType abstractType) {
        addDataType(abstractType, false);
    }


    public Connection getConnection() {
        return null;
    }


    public Handler getHandler() {
        if (this.handler == null) {
            switch (getType()) {
                case MySQL -> this.handler = (Handler) new MySQLHandler((MySQLDatabase) this);
                case PostGRE -> this.handler = (Handler) new PostGREHandler((PostGREDatabase) this);
                case SQLite -> this.handler = (Handler) new SQLiteHandler((SQLiteDatabase) this);
            }
        }

        return this.handler;
    }

    protected abstract String getConnectionString();

    public abstract boolean tableExist(String paramString);

    public abstract List<String> getAllTables();

}
