/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package it.cnr.iit.retrail.server;

import it.cnr.iit.retrail.commons.PepAccessRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kicco
 */
public class PIP {
    protected static final Logger log = LoggerFactory.getLogger(PIP.class);

    public void enrich(PepAccessRequest request) {
        log.info("dummy PIP processor called, ignoring");
    }
    
    public String getUUID() {
        return getClass().getCanonicalName();
    }
}
