/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */

package it.cnr.iit.retrail.server.pip.impl;

import it.cnr.iit.retrail.commons.PepRequestAttribute;
import it.cnr.iit.retrail.server.UConInterface;
import it.cnr.iit.retrail.server.impl.UCon;
import java.util.Collection;

/**
 *
 * @author oneadmin
 */
public abstract class StandAlonePIP extends PIP implements Runnable {
    private Thread thread;
    final private UConInterface ucon = UCon.getInstance();
    
    @Override
    public void init() {
        super.init();
        thread = new Thread(this);
        thread.setName(getUUID());
        log.info("starting standalone thread "+thread);
        thread.start();
    }

    protected void notifyChanges(Collection<PepRequestAttribute> changedAttributes) throws Exception {
        ucon.notifyChanges(changedAttributes);
    }
    
    protected void notifyChanges(PepRequestAttribute changedAttribute) throws Exception {
        ucon.notifyChanges(changedAttribute);
    }
    
    @Override
    public void term() {
        try {
            thread.interrupt();
            thread.join();
        } catch (InterruptedException ex) {
            log.error(ex.toString());
        }
        super.term();
    }
}
