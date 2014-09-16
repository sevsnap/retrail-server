/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package it.cnr.iit.retrail.server;

import it.cnr.iit.retrail.commons.PepAccessRequest;
import it.cnr.iit.retrail.commons.PepRequestAttribute;
import it.cnr.iit.retrail.server.db.Attribute;
import it.cnr.iit.retrail.server.db.DAL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kicco
 */
public abstract class PIP {
    protected static final Logger log = LoggerFactory.getLogger(PIP.class);
    protected static final DAL dal = DAL.getInstance();
    private final String uuid = getUUID();

    public void init() {
        log.info("initializing {}", this);
    }
    
    protected PepRequestAttribute newAttribute(String id, String type, String value, String issuer, String category) {
        return new PepRequestAttribute(id, type, value, issuer, category, uuid);
    }
    
    protected PepRequestAttribute getAttribute(String id, String category) {
        Attribute a = dal.getAttribute(id, category, uuid);
        return new PepRequestAttribute(a.getId(), a.getType(), a.getValue(), a.getIssuer(), a.getCategory(), a.getFactory());
    }
    
    protected Collection<PepRequestAttribute> listAttributes() {
        Collection<PepRequestAttribute> pepAttributes = new ArrayList<>();
        for(Attribute a: dal.listAttributes(uuid))
            pepAttributes.add(new PepRequestAttribute(a.getId(), a.getType(), a.getValue(), a.getIssuer(), a.getCategory(), a.getFactory()));
        return pepAttributes;
    }
    
    public void open(PepAccessRequest request) {
        log.debug("dummy PIP processor called, ignoring");
    }
    
    public void close(PepAccessRequest request) {
        log.debug("dummy PIP processor called, ignoring");
    }
    
    protected void refresh(PepRequestAttribute pepAttribute) {
        log.debug("dummy PIP processor called, ignoring");
    }
    
    public void refresh(PepAccessRequest accessRequest) {
        log.debug("{} refreshing {}", uuid, accessRequest);
        Date now = new Date();
        for(PepRequestAttribute a: accessRequest)
            if(uuid.equals(a.factory) && a.expires != null && a.expires.before(now))
                refresh(a);
    }
        
    public String getUUID() {
        return getClass().getCanonicalName();
    }
    
    public void term() {
        log.info("{} terminated", this);
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName()+" [UUID="+uuid+"]";
    }
}
