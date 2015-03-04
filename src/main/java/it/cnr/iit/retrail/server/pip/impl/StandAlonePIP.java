/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.pip.impl;

import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.server.UConInterface;
import java.util.Collection;

/**
 *
 * @author oneadmin
 */
public abstract class StandAlonePIP extends PIP implements Runnable {

    private Thread thread;

    @Override
    public void init(UConInterface ucon) {
        super.init(ucon);
        thread = new Thread(this);
        thread.setName(getUuid());
        log.info("starting standalone thread " + thread);
        thread.start();
    }

    protected void notifyChanges(Collection<PepAttributeInterface> changedAttributes) throws Exception {
        ucon.notifyChanges(changedAttributes);
    }

    protected void notifyChanges(PepAttributeInterface changedAttribute) throws Exception {
        ucon.notifyChanges(changedAttribute);
    }

    @Override
    public void term() {
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException ex) {
            log.error(ex.toString());
        }
        super.term();
    }
}
