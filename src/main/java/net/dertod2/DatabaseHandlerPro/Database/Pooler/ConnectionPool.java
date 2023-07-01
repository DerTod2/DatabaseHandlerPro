package net.dertod2.DatabaseHandlerPro.Database.Pooler;

import com.google.common.collect.ImmutableList;
import net.dertod2.DatabaseHandlerPro.Database.PooledDatabase;
import net.dertod2.DatabaseHandlerPro.Exceptions.NoPooledConnectionAvailableException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConnectionPool
        implements Runnable {
    private final PooledDatabase pooledDatabase;
    private final PoolSettings poolSettings;
    private final PoolStatistics poolStatistics;
    private final List<PooledConnection> availableList = new CopyOnWriteArrayList<>();
    private final List<PooledConnection> loanedList = new CopyOnWriteArrayList<>();
    private final Object informer = new Object();
    private volatile long lastConnectionFetched;

    public ConnectionPool(PooledDatabase pooledDatabase, PoolSettings poolSettings) {
        this.pooledDatabase = pooledDatabase;

        this.poolSettings = poolSettings;
        this.poolStatistics = new PoolStatistics();
    }

    public boolean testCredentials() {
        if (this.poolSettings.minPoolSize > this.poolSettings.maxPoolSize) return false;

        Connection connection = startConnection();
        if (connection != null) {
            try { connection.close(); } catch (SQLException ignored) { }
            return true;
        }

        return false;
    }

    public PoolStatistics getStatistics() {
        return this.poolStatistics;
    }

    public PoolSettings getSettings() {
        return this.poolSettings;
    }


    public void run() {
        this.pooledDatabase.logger.finest("Working on: Thread Start");
        long startNanos = System.currentTimeMillis();

        List<PooledConnection> removableList = new ArrayList<>();


        try {
            this.pooledDatabase.logger.finest("Working on: Loaned Connections");
            for (PooledConnection pooledConnection : this.loanedList) {
                try {
                    if (pooledConnection.returnToPool) {
                        pooledConnection.isInPool = true;

                        pooledConnection.autoClose = true;
                        pooledConnection.currentUser = "None";

                        removableList.add(pooledConnection);
                        this.availableList.add(pooledConnection);

                        this.poolStatistics.returnedToPool++;

                        this.pooledDatabase.logger.finest("Returned Connection to the pool (fetcher executed close method)...");
                        continue;
                    }
                    if (pooledConnection.getLoanedTime() > this.poolSettings.maxLoanedTime && pooledConnection.autoClose) {
                        try {
                            pooledConnection.rawConnection.close();
                        } catch (SQLException sQLException) {
                        }

                        removableList.add(pooledConnection);
                        this.poolStatistics.maxLoanedTimeReached++;

                        this.pooledDatabase.logger.finest("Force closed pooled connection 'cause of maxLoanedTime reached...");
                    }
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }

            this.pooledDatabase.logger.finest("Working on: Removable Connections");
            for (PooledConnection pooledConnection : removableList) {
                this.loanedList.remove(pooledConnection);
            }

            removableList.clear();


            this.pooledDatabase.logger.finest("Working on: Available Connections");
            for (PooledConnection pooledConnection : this.availableList) {
                try {
                    if (pooledConnection.getLifetime() >= this.poolSettings.maxLifeTime) {
                        removableList.add(pooledConnection);
                        this.poolStatistics.maxLifeTimeReached++;

                        this.pooledDatabase.logger.finest("Closed pooled connection 'cause of maxLifetime reached...");
                        continue;
                    }
                    if (pooledConnection.getIdleTime() >= this.poolSettings.maxIdleTime) {
                        removableList.add(pooledConnection);
                        this.poolStatistics.maxIdleTimeReached++;

                        this.pooledDatabase.logger.finest("Closed pooled connection 'cause of maxIdleTime reached...");
                        continue;
                    }
                    if (!pooledConnection.rawConnection.isValid(1)) {
                        removableList.add(pooledConnection);
                        this.poolStatistics.invalidConnection++;

                        this.pooledDatabase.logger.finest("Closed pooled connection 'cause of invalid raw connection...");
                    }
                } catch (Exception ignored) { }
            }


            this.pooledDatabase.logger.finest("Working on: Invalid Connections");
            for (PooledConnection pooledConnection : removableList) {
                try { pooledConnection.rawConnection.close(); } catch (SQLException ignored) { }

                this.availableList.remove(pooledConnection);
            }

            removableList.clear();


            this.pooledDatabase.logger.finest("Working on: Pool Size");
            if (getLastFetchTime() < this.poolSettings.startSleepMode) {
                while (this.availableList.size() < this.poolSettings.minPoolSize) {
                    if (this.availableList.size() + this.loanedList.size() >= this.poolSettings.maxPoolSize) {
                        this.poolStatistics.maxPoolSizeReached++;

                        break;
                    }
                    Connection connection = startConnection();
                    if (connection != null) {
                        this.poolStatistics.openedConnections++;
                        this.availableList.add(new PooledConnection(this, connection));

                        this.pooledDatabase.logger.finest("Opened new pooled connection cause not enough available connections");
                    }
                }
            }


            synchronized (this.informer) {
                this.informer.notifyAll();
            }


            this.pooledDatabase.logger.finest("Working on: Statistics");
            this.poolStatistics.lastWatcherDuration = System.currentTimeMillis() - startNanos;
            this.poolStatistics.watcherRuns++;
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    private Connection startConnection() {
        try {
            return DriverManager.getConnection(this.poolSettings.jdbcUrl, this.poolSettings.username, this.poolSettings.password);
        } catch (SQLException exc) {
            exc.printStackTrace();
            return null;
        }
    }

    public Connection getConnection() {
        this.lastConnectionFetched = System.currentTimeMillis();

        if (this.availableList.isEmpty()) {
            int openedConnections = this.loanedList.size();

            if (openedConnections >= this.poolSettings.maxPoolSize) {
                this.poolStatistics.maxPoolSizeReachedWhileFetching++;
                throw new NoPooledConnectionAvailableException();
            }
            this.poolStatistics.threadLock++;
            synchronized (this.informer) {
                try { this.informer.wait(10000L); } catch (InterruptedException ignored) { }
            }
        }


        PooledConnection pooledConnection = this.availableList.remove(0);
        pooledConnection.loaned = System.currentTimeMillis();
        pooledConnection.returnToPool = false;
        pooledConnection.isInPool = false;

        pooledConnection.currentUser = getFetcher(Thread.currentThread().getStackTrace());

        this.loanedList.add(pooledConnection);

        this.pooledDatabase.logger.finest("Fetched connection out of pool...");

        return pooledConnection;
    }

    private String getFetcher(StackTraceElement[] stackTrace) {
        String fetcher = "Unknown";

        for (StackTraceElement stackTraceElement : stackTrace) {
            String className = stackTraceElement.getClassName();

            if (className.contains("net.dertod2") && !className.contains("DatabaseHandler")) {
                int beginIndex = className.indexOf(".", className.indexOf(".")) + 1;
                int endIndex = className.indexOf(".", beginIndex);

                fetcher = className.substring(beginIndex, endIndex);

                break;
            }
        }

        return fetcher;
    }

    public int getAvailableConnections() {
        return this.availableList.size();
    }

    public int getLoanedConnections() {
        return this.loanedList.size();
    }

    public long getLastFetched() {
        return this.lastConnectionFetched;
    }

    public long getLastFetchTime() {
        return System.currentTimeMillis() - this.lastConnectionFetched;
    }


    public List<PooledConnection> getActiveConnections() {
        return ImmutableList.<PooledConnection>builder().addAll(this.availableList).addAll(this.loanedList).build();
    }

    public PoolStatistics shutdown() {
        for (PooledConnection pooledConnection : this.availableList) {
            try {
                pooledConnection.rawConnection.close();
            } catch (SQLException sQLException) {
            }
        }


        for (PooledConnection pooledConnection : this.loanedList) {
            try { pooledConnection.rawConnection.close(); } catch (SQLException ignored) { }
        }

        this.availableList.clear();
        this.loanedList.clear();

        return this.poolStatistics;
    }

}
