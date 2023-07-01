package net.dertod2.DatabaseHandlerPro.Database.Pooler;

import java.util.concurrent.TimeUnit;

public class PoolSettings {
    protected String jdbcUrl;
    protected String username;
    protected String password;
    protected int minAvailable = 2;

    protected int minPoolSize = 2;
    protected int maxPoolSize = 50;

    protected long startSleepMode = TimeUnit.MINUTES.toMillis(20L);

    protected long maxLifeTime = TimeUnit.MINUTES.toMillis(60L);
    protected long maxIdleTime = TimeUnit.MINUTES.toMillis(10L);
    protected long maxLoanedTime = TimeUnit.MINUTES.toMillis(5L);

    protected long watcherTimer = TimeUnit.MILLISECONDS.toMillis(250L);

    public void setUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getMinimumAvailable() {
        return this.minAvailable;
    }


    public void setMinimumAvailable(int minAvailable) {
        this.minAvailable = minAvailable;
    }

    public int getMinimumPoolSize() {
        return this.minPoolSize;
    }

    public void setMinimumPoolSize(int minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    public int getMaximumPoolSize() {
        return this.maxPoolSize;
    }

    public void setMaximumPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public long getStartSleepMode() {
        return this.startSleepMode;
    }


    public void setStartSleepMode(long timeToStartInMS) {
        this.startSleepMode = timeToStartInMS;
    }

    public long getLifetime() {
        return this.maxLifeTime;
    }

    public void setLifetime(long maxLifeTimeMS) {
        this.maxLifeTime = maxLifeTimeMS;
    }

    public long getIdleTime() {
        return this.maxIdleTime;
    }

    public void setIdleTime(long maxIdleTimeMS) {
        this.maxIdleTime = maxIdleTimeMS;
    }

    public long getLoanedTime() {
        return this.maxLoanedTime;
    }

    public void setLoanedTime(long maxLoanedTimeMS) {
        this.maxLoanedTime = maxLoanedTimeMS;
    }

    public long getWatcherTime() {
        return this.watcherTimer;
    }

    public void setWatcherTime(long watcherTimeMS) {
        this.watcherTimer = watcherTimeMS;
    }

}
