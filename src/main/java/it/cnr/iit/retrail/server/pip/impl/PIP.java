/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.pip.impl;

import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.commons.PepRequestInterface;
import it.cnr.iit.retrail.commons.PepSessionInterface;
import it.cnr.iit.retrail.commons.impl.PepRequest;
import it.cnr.iit.retrail.commons.impl.PepAttribute;
import it.cnr.iit.retrail.commons.impl.PepSession;
import it.cnr.iit.retrail.server.dal.UconAttribute;
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
    public PepAttributeInterface newSharedAttribute(String id, String type, String value, String issuer, String category) {
        UconAttribute u = dal.getSharedAttribute(category, id);
        //log.error("SEARCH CAT {} ID {} I GOT ATTRIBUTE: {}", category, id, u);
        if(u == null) {
            PepAttribute a = new PepAttribute(id, type, value, issuer, category, uuid);
            u = UconAttribute.newInstance(a);
            // save and get the id for this object, so it can be shared.
            //u = (UconAttribute) dal.save(u);
        //log.error("ITS NEW: {}", u);
        } else {
            u.setType(type);
            u.setValue(value);
            u.setIssuer(issuer);
            //log.error("ITS BEEN FOUND: {}", u);
        }
        assert(u.getFactory() != null);
        //assert(u.getRowId() != null);
        return u;
    }

    @Override
    public PepAttributeInterface newPrivateAttribute(String id, String type, String value, String issuer, PepAttributeInterface parent) {
        UconAttribute p = (UconAttribute) parent;
        for(UconAttribute child: p.getChildren())
            if(child.getId().equals(id)) {
                child.setType(type);
                child.setValue(value);
                child.setIssuer(issuer);
                 //log.error("XXXQQQ child FOUND: {}", child);
                return child;
        }
        PepAttribute a = new PepAttribute(id, type, value, issuer, parent.getCategory(), uuid);
        UconAttribute u = UconAttribute.newInstance(a);
        u = (UconAttribute) dal.save(u);
        // DON'T SAVE ME!
        u.setParent(p);
        //log.error("XXXQQQ child not found, creating new: {}", u);
        return u;
    }
    
    @Override
    public Collection<PepAttributeInterface> listUnmanagedAttributes() {
        return dal.listUnmanagedAttributes(uuid);
    }

    @Override
    public Collection<PepAttributeInterface> listManagedAttributes() {
        return dal.listManagedAttributes(uuid);
    }

    @Override
    public void onBeforeTryAccess(PepRequestInterface request) {
        log.debug("dummy PIP processor called, ignoring");
    }

    @Override
    public void onAfterTryAccess(PepRequestInterface request, PepSessionInterface session) {
        log.debug("dummy PIP processor called, ignoring");
    }

    @Override
    public void onBeforeStartAccess(PepRequestInterface request, PepSessionInterface session) {
        log.debug("dummy PIP processor called, ignoring");
    }

    @Override
    public void onAfterStartAccess(PepRequestInterface request, PepSessionInterface session) {
        log.debug("dummy PIP processor called, ignoring");
    }

    protected void refresh(PepAttributeInterface pepAttribute, PepSessionInterface session) {
        log.debug("dummy PIP processor called, ignoring");
    }

    @Override
    public void onBeforeRevokeAccess(PepRequestInterface request, PepSessionInterface session) {
        log.debug("dummy PIP processor called, ignoring");
    }

    @Override
    public void onAfterRevokeAccess(PepRequestInterface request, PepSessionInterface session) {
        log.debug("dummy PIP processor called, ignoring");
    }

    @Override
    public void onBeforeEndAccess(PepRequestInterface request, PepSessionInterface session) {
        log.debug("dummy PIP processor called, ignoring");
    }

    @Override
    public void onAfterEndAccess(PepRequestInterface request, PepSessionInterface session) {
        log.debug("dummy PIP processor called, ignoring");
    }

    @Override
    public void refresh(PepRequestInterface accessRequest, PepSessionInterface session) {
        log.debug("{} refreshing {}", uuid, accessRequest);
        Date now = new Date();
        for (PepAttributeInterface a : accessRequest) {
            if (uuid.equals(a.getFactory()) && a.getExpires() != null && a.getExpires().before(now)) {
                refresh(a, session);
            }
        }
    }

    @Override
    public String getUUID() {
        return getClass().getCanonicalName();
    }

    @Override
    public void term() {
        dal.removeAttributesByFactory(getUUID());
        log.info("{} terminated", this);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [UUID=" + uuid + "]";
    }
}
