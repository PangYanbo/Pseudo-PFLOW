/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package jp.ac.ut.csis.pflow.thread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import jp.ac.ut.csis.pflow.thread.ARunningThread;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AMainThread {
    private static final Logger LOGGER = LogManager.getLogger(AMainThread.class);
    private int _threadNum;
    private ExecutorService _exService;
    private List<ARunningThread> _threadList;

    protected AMainThread(int threadNum) {
        this._threadNum = threadNum;
        this._exService = null;
        this._threadList = null;
    }

    protected abstract void doMain();

    public int getThreadNum() {
        return this._threadNum;
    }

    public void init() {
        this._exService = Executors.newFixedThreadPool(this._threadNum);
        this._threadList = new ArrayList<ARunningThread>();
    }

    public void invoke() {
        LOGGER.debug("[INVOKE]");
        try {
            this.init();
            this.doMain();
            this.terminate();
        }
        finally {
            this.close();
        }
    }

    protected synchronized void release(ARunningThread thread) {
        LOGGER.debug("[RELEASE]");
        this._threadList.remove(thread);
        this.notifyAll();
    }

    protected synchronized void terminate() {
        while (!this._threadList.isEmpty()) {
            LOGGER.debug("[TERMINATING]" + this._threadList.size());
            try {
                this.wait();
            }
            catch (InterruptedException exp) {
                LOGGER.error("fail to wait", (Throwable)exp);
            }
        }
    }

    protected void close() {
        LOGGER.debug("[CLOSE]");
        this._exService.shutdown();
    }

    protected synchronized void append(ARunningThread thread) {
        LOGGER.debug("[APPENDING]" + this._threadList.size());
        this._threadList.add(thread);
        while (this._threadList.size() > this._threadNum) {
            try {
                this.wait();
            }
            catch (InterruptedException exp) {
                LOGGER.error("fail to wait", (Throwable)exp);
            }
        }
        this._exService.execute(thread);
    }
}

