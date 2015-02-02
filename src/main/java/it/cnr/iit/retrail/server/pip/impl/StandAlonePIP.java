/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */

package it.cnr.iit.retrail.server.pip.impl;

import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.server.impl.UCon;
import java.util.Collection;

/**
 *
 * @author oneadmin
 */
public abstract class StandAlonePIP extends PIP implements Runnable {
    private Thread thread;
    
    @Override
    public void init() {
        super.init();
        thread = new Thread(this);
        thread.setName(getUUID());
        log.info("starting standalone thread "+thread);
        thread.start();
    }

    protected void notifyChanges(Collection<PepAttributeInterface> changedAttributes) throws Exception {
        UCon.getInstance().notifyChanges(changedAttributes);
    }
    
    protected void notifyChanges(PepAttributeInterface changedAttribute) throws Exception {
        UCon.getInstance().notifyChanges(changedAttribute);
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
