/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package it.cnr.iit.retrail.server;

import it.cnr.iit.retrail.commons.PepRequestAttribute;
import it.cnr.iit.retrail.server.db.Attribute;
import it.cnr.iit.retrail.server.db.DAL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author oneadmin
 */
public class PIPThread extends PIP implements Runnable {
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
    
    @Override
    public void run() {
        throw new UnsupportedOperationException("Not supported yet.");
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
