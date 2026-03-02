/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package jp.ac.ut.csis.pflow.thread;

import jp.ac.ut.csis.pflow.thread.AMainThread;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class ARunningThread
implements Runnable {
    private static final Logger LOGGER = LogManager.getLogger(ARunningThread.class);
    private AMainThread _mainThread;

    public ARunningThread(AMainThread mainThread) {
        this._mainThread = mainThread;
    }

    protected abstract void doMain();

    protected AMainThread getMainThread() {
        return this._mainThread;
    }

    @Override
    public void run() {
        LOGGER.debug("[THREAD]start");
        try {
            this.doMain();
        }
        finally {
            this._mainThread.release(this);
        }
    }
}

