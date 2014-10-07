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
    public PepAttribute newSharedAttribute(String id, String type, String value, String issuer, String category) {
        return new PepAttribute(id, type, value, issuer, category, uuid);
    }

    @Override
    public PepAttributeInterface newPrivateAttribute(String id, String type, String value, String issuer, PepAttributeInterface parent) {
        PepAttribute a = new PepAttribute(id, type, value, issuer, parent.getCategory(), uuid);
        a.setParent(parent);
        return a;
    }

    private PepAttribute pepFromDalAttribute(UconAttribute a) {
        PepAttribute pepA = new PepAttribute(a.getId(), a.getType(), a.getValue(), a.getIssuer(), a.getCategory(), a.getFactory());
        UconAttribute p = (UconAttribute) a.getParent();
        if (p != null) {
            PepAttribute parentA = new PepAttribute(p.getId(), p.getType(), p.getValue(), p.getIssuer(), p.getCategory(), p.getFactory());
            pepA.setParent(parentA);
        }
        log.info("*** PARENT = {} {}", p, pepA.getParent());
        return pepA;
    }
    /*
    protected Collection<PepRequestAttribute> listSharedAttributes() {
        Collection<PepRequestAttribute> pepAttributes = new ArrayList<>();
        for (UconAttribute a : dal.listSharedAttributes(uuid)) {
            PepAttribute pepA = pepFromDalAttribute(a);
            pepAttributes.add(pepA);
        }
        return pepAttributes;
    }
    
    protected Collection<PepRequestAttribute> listPrivateAttributes(PepAttribute parent) {
        Collection<PepRequestAttribute> pepAttributes = new ArrayList<>();
        for (UconAttribute a : dal.listPrivateAttributes(uuid, parent.id, parent.category)) {
            PepAttribute pepA = pepFromDalAttribute(a);
            pepAttributes.add(pepA);
        }
        return pepAttributes;
    }
*/
    @Override
    public Collection<PepAttributeInterface> listUnmanagedAttributes() {
        Collection<PepAttributeInterface> pepAttributes = new ArrayList<>();
        for (UconAttribute a : dal.listUnmanagedAttributes(uuid)) {
            PepAttributeInterface pepA = pepFromDalAttribute(a);
            pepAttributes.add(pepA);
        }
        return pepAttributes;
    }

    @Override
    public Collection<PepAttributeInterface> listManagedAttributes() {
        Collection<PepAttributeInterface> pepAttributes = new ArrayList<>();
        for (UconAttribute a : dal.listManagedAttributes(uuid)) {
            PepAttribute pepA = pepFromDalAttribute(a);
            pepAttributes.add(pepA);
        }
        return pepAttributes;
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
        log.info("{} terminated", this);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [UUID=" + uuid + "]";
    }
}
