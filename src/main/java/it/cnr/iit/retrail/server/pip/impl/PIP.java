/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */

package it.cnr.iit.retrail.server.pip.impl;

import it.cnr.iit.retrail.commons.PepAccessRequest;
import it.cnr.iit.retrail.commons.PepRequestAttribute;
import it.cnr.iit.retrail.commons.PepSession;
import it.cnr.iit.retrail.server.dal.Attribute;
import it.cnr.iit.retrail.server.dal.DAL;
import it.cnr.iit.retrail.server.pip.PIPInterface;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kicco
 */
public abstract class PIP implements PIPInterface {
    protected Logger log = LoggerFactory.getLogger(PIP.class);
    protected static final DAL dal = DAL.getInstance();
    private final String uuid = getUUID();

    @Override
    public void init() {
        log.info("initializing {}", this);
    }
    
    @Override
    public PepRequestAttribute newSharedAttribute(String id, String type, String value, String issuer, String category) {
        return new PepRequestAttribute(id, type, value, issuer, category, uuid);
    }
    
    @Override
    public PepRequestAttribute newPrivateAttribute(String id, String type, String value, String issuer, PepRequestAttribute parent) {
        PepRequestAttribute a = new PepRequestAttribute(id, type, value, issuer, parent.category, uuid);
        a.parent = parent;
        return a;
    }
    
    protected PepRequestAttribute getAttribute(String id, String category) {
        Attribute a = dal.getAttribute(id, category, uuid);
        return new PepRequestAttribute(a.getId(), a.getType(), a.getValue(), a.getIssuer(), a.getCategory(), a.getFactory());
    }
    
    @Override
    public Collection<PepRequestAttribute> listAttributes() {
        Collection<PepRequestAttribute> pepAttributes = new ArrayList<>();
        for(Attribute a: dal.listAttributesByFactory(uuid))
            pepAttributes.add(new PepRequestAttribute(a.getId(), a.getType(), a.getValue(), a.getIssuer(), a.getCategory(), a.getFactory()));
        return pepAttributes;
    }
    
    @Override
    public void onBeforeTryAccess(PepAccessRequest request) {
        log.debug("dummy PIP processor called, ignoring");
    }
        
    @Override
    public void onAfterTryAccess(PepAccessRequest request, PepSession session) {
        log.debug("dummy PIP processor called, ignoring");
    }
    
    @Override
    public void onBeforeStartAccess(PepAccessRequest request, PepSession session) {
        log.debug("dummy PIP processor called, ignoring");
    }
    
    @Override
    public void onAfterStartAccess(PepAccessRequest request, PepSession session) {
        log.debug("dummy PIP processor called, ignoring");
    }
    
    protected void refresh(PepRequestAttribute pepAttribute, PepSession session) {
        log.debug("dummy PIP processor called, ignoring");
    }
      
    @Override
    public void onBeforeRevokeAccess(PepAccessRequest request, PepSession session) {
        log.debug("dummy PIP processor called, ignoring");
    }
    
    @Override
    public void onAfterRevokeAccess(PepAccessRequest request, PepSession session) {
        log.debug("dummy PIP processor called, ignoring");
    }
    
    @Override
    public void onBeforeEndAccess(PepAccessRequest request, PepSession session) {
        log.debug("dummy PIP processor called, ignoring");
    }
    
    @Override
    public void onAfterEndAccess(PepAccessRequest request, PepSession session) {
        log.debug("dummy PIP processor called, ignoring");
    }
   
    @Override
    public void refresh(PepAccessRequest accessRequest, PepSession session) {
        log.debug("{} refreshing {}", uuid, accessRequest);
        Date now = new Date();
        for(PepRequestAttribute a: accessRequest)
            if(uuid.equals(a.factory) && a.expires != null && a.expires.before(now))
                refresh(a, session);
    }
        
    @Override
    public String getUUID() {
        return getClass().getCanonicalName();
    }
    
    @Override
    public void term() {
        log.info("{} terminated", this);
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName()+" [UUID="+uuid+"]";
    }
}
