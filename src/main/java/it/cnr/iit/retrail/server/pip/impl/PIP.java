/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.pip.impl;

import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.commons.PepRequestInterface;
import it.cnr.iit.retrail.commons.PepSessionInterface;
import it.cnr.iit.retrail.server.UConInterface;
import it.cnr.iit.retrail.server.behaviour.OngoingAccess;
import it.cnr.iit.retrail.server.dal.UconAttribute;
import it.cnr.iit.retrail.server.dal.DAL;
import it.cnr.iit.retrail.server.pip.ActionEvent;
import it.cnr.iit.retrail.server.pip.PIPInterface;
import it.cnr.iit.retrail.server.pip.SystemEvent;
import java.util.Collection;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

/**
 *
 * @author kicco
 */
public abstract class PIP implements PIPInterface {

    protected Logger log = LoggerFactory.getLogger(PIP.class);
    protected static final DAL dal = DAL.getInstance();
    protected UConInterface ucon;

    private String uuid = getClass().getCanonicalName();

    public PIP()  { 
    }
    
    public PIP(Element configElement) throws Exception {
        throw new UnsupportedOperationException("not supported by this PIP");
    }
    
    @Override
    public void init(UConInterface ucon) {
        if (ucon == null) {
            log.warn("initializing {} with null UConInterface!", this);
        } else {
            log.info("initializing {}", this);
        }
        this.ucon = ucon;
    }

    @Override
    public boolean isInited() {
        return ucon != null;
    }

    @Override
    public void fireBeforeActionEvent(ActionEvent e) {
        log.debug("ignoring {}", e);
    }

    @Override
    public void fireAfterActionEvent(ActionEvent e) {
        log.debug("ignoring {}", e);
    }

    @Override
    public void fireSystemEvent(SystemEvent e) {
        log.debug("ignoring {}", e);
    }

    @Override
    public PepAttributeInterface newPrivateAttribute(String id, String type, String value, String issuer, PepAttributeInterface parent
    ) {
        UconAttribute p = (UconAttribute) parent;
        return dal.newPrivateAttribute(id, type, value, issuer, p, uuid);
    }

    @Override
    public PepAttributeInterface newSharedAttribute(String id, String type, String value, String issuer, String category
    ) {
        return dal.newSharedAttribute(id, type, value, issuer, category, uuid);
    }
    
    @Override
    public PepAttributeInterface getSharedAttribute(String category, String id) {
        return dal.getSharedAttribute(category, id);
    }

    @Override
    public Collection<PepAttributeInterface> listUnmanagedAttributes() {
        return dal.listUnmanagedAttributes(uuid);
    }

    @Override
    public Collection<PepAttributeInterface> listManagedAttributes() {
        return dal.listManagedAttributes(uuid);
    }

    protected void refresh(PepAttributeInterface pepAttribute, PepSessionInterface session) {
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
    public String getUuid() {
        return uuid;
    }

    @Override
    public void setUuid(String uuid) {
        if(isInited())
            throw new RuntimeException(this+" already inited and cannot change its uuid to "+uuid);
        log.info("setting uuid={} for {}", uuid, this);
        this.uuid = uuid;
    }

    public UConInterface getUCon() {
        return ucon;
    }

    @Override
    public void term() {
        dal.removeAttributesByFactory(getUuid());
        log.info("{} terminated", this);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [uuid=" + uuid + "]";
    }
}
