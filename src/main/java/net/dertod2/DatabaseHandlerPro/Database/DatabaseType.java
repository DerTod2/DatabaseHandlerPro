package net.dertod2.DatabaseHandlerPro.Database;

public enum DatabaseType {
    MySQL("com.mysql.jdbc.Driver", true, true, MySQLDatabase.class, 3306),
    PostGRE("org.postgresql.Driver", true, true, PostGREDatabase.class, 5432),
    SQLite("org.sqlite.JDBC", true, false, SQLiteDatabase.class, null),
    Disabled(null, false, false, null, -1);

    private final String driverPackage;

    private final Class<? extends AbstractDatabase> driverClass;
    private final Integer defaultPort;
    private final boolean usesDatabaseDriver;
    private final boolean supportConnectionPool;

    DatabaseType(String driverPackage, boolean usesDatabaseDriver, boolean supportConnectionPool, Class<? extends AbstractDatabase> driverClass, Integer defaultPort) {
        this.driverPackage = driverPackage;
        this.driverClass = driverClass;
        this.defaultPort = defaultPort;

        this.usesDatabaseDriver = usesDatabaseDriver;
        this.supportConnectionPool = supportConnectionPool;
    }

    public static DatabaseType byName(String driverName) {
        for (DatabaseType databaseType : values()) {
            if (databaseType.name().equalsIgnoreCase(driverName)) return databaseType;

        }
        return null;
    }

    public static DatabaseType byPackage(String packageName) {
        for (DatabaseType databaseType : values()) {
            if (databaseType.driverPackage.equals(packageName)) return databaseType;

        }
        return null;
    }

    public static DatabaseType byClass(Class<? extends AbstractDatabase> driverClass) {
        for (DatabaseType databaseType : values()) {
            if (databaseType.driverClass.equals(driverClass)) return databaseType;

        }
        return null;
    }

    public boolean isUsingDatabaseDriver() {
        return this.usesDatabaseDriver;
    }

    public boolean isUsingConnectionPool() {
        return this.supportConnectionPool;
    }

    public String getDriverPackage() {
        return this.driverPackage;
    }

    public Class<? extends AbstractDatabase> getDriverClass() {
        return this.driverClass;
    }

    public Integer getDriverPort() {
        return this.defaultPort;
    }

}
