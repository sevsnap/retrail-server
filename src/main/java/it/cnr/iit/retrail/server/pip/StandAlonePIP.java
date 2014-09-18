/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and onTryAccess the template in the editor.
 */

package it.cnr.iit.retrail.server.pip;

import it.cnr.iit.retrail.commons.PepRequestAttribute;
import it.cnr.iit.retrail.server.UCon;
import java.util.Collection;

/**
 *
 * @author oneadmin
 */
public abstract class StandAlonePIP extends PIP implements Runnable {
    private Thread thread;
    final private UCon ucon = UCon.getInstance();
    
    @Override
    public void init() {
        super.init();
        thread = new Thread(this);
        thread.setName(getUUID());
        log.info("starting standalone thread "+thread);
        thread.start();
    }

    protected void notifyChanges(Collection<PepRequestAttribute> changedAttributes) {
        ucon.notifyChanges(changedAttributes);
    }
    
    protected void notifyChanges(PepRequestAttribute changedAttribute) {
        ucon.notifyChanges(changedAttribute);
    }
    
    @Override
    public void term() {
        try {
            thread.join();
        } catch (InterruptedException ex) {
            log.error(ex.toString());
        }
        super.term();
    }
}
