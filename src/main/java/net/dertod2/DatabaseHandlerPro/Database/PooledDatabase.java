package net.dertod2.DatabaseHandlerPro.Database;

import net.dertod2.DatabaseHandlerPro.Database.Pooler.ConnectionPool;
import net.dertod2.DatabaseHandlerPro.Database.Pooler.PoolSettings;
import net.dertod2.DatabaseHandlerPro.Database.Pooler.PoolStatistics;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public abstract class PooledDatabase
        extends DriverDatabase {
    protected ConnectionPool connectionPool;
    private ScheduledExecutorService executorService;

    public PooledDatabase(String host, Integer port, String database, String username, String password, boolean debugMode) throws SQLException {
        super(host, port, database, username, password);
        if (debugMode) {
            this.logger.setLevel(Level.FINEST);
        } else {
            this.logger.setLevel(Level.INFO);
        }


        PoolSettings poolSettings = new PoolSettings();
        poolSettings.setUrl(getConnectionString());
        poolSettings.setUsername(this.username);
        poolSettings.setPassword(this.password);

        this.connectionPool = new ConnectionPool(this, poolSettings);
        this.executorService = Executors.newScheduledThreadPool(2);
        this.executorService.scheduleAtFixedRate((Runnable) this.connectionPool, 0L, 1L, TimeUnit.SECONDS);

        Connection connection = getConnection();
        if (connection != null && connection.isValid(1))
            this.logger.fine("Test connection successfully fetched out of connection pool.");
    }

    public PooledDatabase(String host, Integer port, String database, String username, String password) throws SQLException {
        this(host, port, database, username, password, false);
    }

    public ConnectionPool getPool() {
        return this.connectionPool;
    }

    public Connection getConnection() {
        if (this.connectionPool == null) return null;
        return this.connectionPool.getConnection();
    }

    public PoolStatistics shutdown() {
        if (this.connectionPool != null) {
            this.executorService.shutdown();

            PoolStatistics poolStatistics = this.connectionPool.shutdown();
            this.connectionPool = null;

            return poolStatistics;
        }

        return null;
    }

    public PoolStatistics restart() {
        PoolStatistics poolStatistics = shutdown();

        PoolSettings poolSettings = new PoolSettings();
        poolSettings.setUrl(getConnectionString());
        poolSettings.setUsername(this.username);
        poolSettings.setPassword(this.password);

        this.connectionPool = new ConnectionPool(this, poolSettings);
        this.executorService = Executors.newSingleThreadScheduledExecutor();
        this.executorService.scheduleAtFixedRate((Runnable) this.connectionPool, 0L, 1L, TimeUnit.SECONDS);

        return poolStatistics;
    }

}
