package net.dertod2.DatabaseHandlerPro.Database.Pooler;

public class PoolStatistics {
    protected long lastWatcherDuration;
    protected int watcherRuns = 0;
    protected int openedConnections = 0;

    protected int threadLock = 0;
    protected int maxPoolSizeReachedWhileFetching = 0;
    protected int maxPoolSizeReached = 0;
    protected int invalidConnection = 0;
    protected int maxLifeTimeReached = 0;
    protected int maxIdleTimeReached = 0;
    protected int returnedToPool = 0;
    protected int maxLoanedTimeReached = 0;

    public synchronized long getLastWatcherDuration() {
        return this.lastWatcherDuration;
    }

    public synchronized int getWatcherRuns() {
        return this.watcherRuns;
    }

    public synchronized int getOpenedConnections() {
        return this.openedConnections;
    }

    public synchronized int getThreadLock() {
        return this.threadLock;
    }

    public synchronized int getMaxPoolSizeReachedWhileFetching() {
        return this.maxPoolSizeReachedWhileFetching;
    }

    public synchronized int getMaxPoolSizeReached() {
        return this.maxPoolSizeReached;
    }

    public synchronized int getInvalidConnection() {
        return this.invalidConnection;
    }

    public synchronized int getMaxLifeTimeReached() {
        return this.maxLifeTimeReached;
    }

    public synchronized int getMaxIdleTimeReached() {
        return this.maxIdleTimeReached;
    }

    public synchronized int getReturnedToPool() {
        return this.returnedToPool;
    }

    public synchronized int getMaxLoanedTimeReached() {
        return this.maxLoanedTimeReached;
    }

}
