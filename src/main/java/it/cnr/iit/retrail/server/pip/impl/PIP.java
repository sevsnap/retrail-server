/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.pip.impl;

import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.commons.PepRequestInterface;
import it.cnr.iit.retrail.commons.PepSessionInterface;
import it.cnr.iit.retrail.commons.impl.PepAttribute;
import it.cnr.iit.retrail.server.UConInterface;
import it.cnr.iit.retrail.server.dal.UconAttribute;
import it.cnr.iit.retrail.server.dal.DAL;
import it.cnr.iit.retrail.server.pip.Event;
import it.cnr.iit.retrail.server.pip.PIPInterface;
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
    protected UConInterface ucon;
    
    private final String uuid = getUUID();

    @Override
    public void init(UConInterface ucon) {
        if(ucon == null)
            log.warn("initializing {} with null UConInterface!", this);
        else
            log.info("initializing {} with {}", this, ucon);
        this.ucon = ucon;
    }
    
    @Override
    public boolean isInited() {
        return ucon != null;
    }

    @Override
    public void fireEvent(Event e) {
        switch(e.type) {
            case beforeTryAccess:
                onBeforeTryAccess(e.request);
                break;
            case afterTryAccess:
                onAfterTryAccess(e.request, e.session);
                break;
            case beforeStartAccess:
                onBeforeStartAccess(e.request, e.session);
                break;
            case afterStartAccess:
                onAfterStartAccess(e.request, e.session);
                break;
            case beforeRunObligations:
                onBeforeRunObligations(e.request, e.session);
                break;
            case afterRunObligations:
                onAfterRunObligations(e.request, e.session, e.ack);
                break;
            case beforeRevokeAccess:
                onBeforeRevokeAccess(e.request, e.session);
                break;
            case afterRevokeAccess:
                onAfterRevokeAccess(e.request, e.session, e.ack);
                break;
            case beforeEndAccess:
                onBeforeEndAccess(e.request, e.session);
                break;
            case afterEndAccess:
                onAfterEndAccess(e.request, e.session);
                break;
            default:
                throw new RuntimeException("while handling event "+e+": unknown type " + e.type);
        }
    }
    
    @Override
    public PepAttributeInterface newPrivateAttribute(String id, String type, String value, String issuer, PepAttributeInterface parent) {
        UconAttribute p = (UconAttribute) parent;
        return dal.newPrivateAttribute(id, type, value, issuer, p, uuid);
    }

    @Override
    public Collection<PepAttributeInterface> listUnmanagedAttributes() {
        return dal.listUnmanagedAttributes(uuid);
    }

    @Override
    public Collection<PepAttributeInterface> listManagedAttributes() {
        return dal.listManagedAttributes(uuid);
    }

    /**
     * onBeforeTryAccess()
     *
     * is the event handler called by the UCon just before executing a tryAccess
     * for the given request. This chance may be used to add or alter some
     * attributes of the request. The default implementation does nothing. New
     * attributes are automatically registered as owned by this PIP and may be
     * retrieved back by the listManagedAttributes() method.
     *
     * @param request the request to be processed by the UCon.
     */
    public void onBeforeTryAccess(PepRequestInterface request) 
    {
        log.debug("dummy PIP processor called, ignoring");
    }

    /**
     * onAfterTryAccess()
     *
     * is the event handler called by the UCon just after executing a tryAccess
     * for the given request. The default implementation does nothing. New
     * attributes must be added by invoking the newAttributes() method in order
     * to register the new attribute as owned by this PIP and retrieve it back
     * by the listManagedAttributes() method.
     *
     * @param request the request just processed.
     * @param session the answer from UCon. In particular, it holds the decision
     * made by the PDP and the consequent state of the session (i.e., ONGOING).
     */
    public void onAfterTryAccess(PepRequestInterface request, PepSessionInterface session)
    {
        log.debug("dummy PIP processor called, ignoring");
    }

    /**
     * onBeforeStartAccess()
     *
     * is the event handler called by the UCon just before executing a
     * startAccess for the given request and session. The default implementation
     * does nothing.
     *
     * @param request the request that is going to be processed.
     * @param session the current session.
     */
    
    public void onBeforeStartAccess(PepRequestInterface request, PepSessionInterface session)
    {
        log.debug("dummy PIP processor called, ignoring");
    }

    /**
     * onAfterStartAccess()
     *
     * is the event handler called by the UCon just after having executed a
     * startAccess for the given request and session. The default implementation
     * does nothing.
     *
     * @param request the request just processed.
     * @param session the current session holding the decision made by the PDP
     * and the consequent status.
     */
    
    public void onAfterStartAccess(PepRequestInterface request, PepSessionInterface session)
                {
        log.debug("dummy PIP processor called, ignoring");
    }


    /**
     * onBeforeRevokeAccess()
     *
     * is the event handler called by the UCon just before revoking the access
     * rights to the corresponding PEP. The default implementation does nothing.
     *
     * @param request the request that is going to be revoked.
     * @param session the current session.
     */
    public void onBeforeRevokeAccess(PepRequestInterface request, PepSessionInterface session)
    {
        log.debug("dummy PIP processor called, ignoring");
    }

    /**
     * onAfterRevokeAccess()
     *
     * is the event handler called by the UCon just after having revoked the
     * access rights to the corresponding PEP. The default implementation does
     * nothing.
     *
     * @param request the request whose rights have just been revoked.
     * @param session the current (revoked) session.
     * @param ack
     */
    public void onAfterRevokeAccess(PepRequestInterface request, PepSessionInterface session, Object ack)
    {
        log.debug("dummy PIP processor called, ignoring");
    }

    /**
     * onBeforeEndAccess()
     *
     * is the event handler called by the UCon just before ending a session. The
     * default implementation does nothing.
     *
     * @param request the request to be ended.
     * @param session the current session.
     */
    public void onBeforeEndAccess(PepRequestInterface request, PepSessionInterface session)
    {
        log.debug("dummy PIP processor called, ignoring");
    }

    /**
     * onAfterEndAccess()
     *
     * is the event handler invoked by the UCon just after having ended a
     * session. The default implementation does nothing.
     *
     * @param request the request just ended.
     * @param session the current (deleted) session.
     */
    public void onAfterEndAccess(PepRequestInterface request, PepSessionInterface session)
    {
        log.debug("dummy PIP processor called, ignoring");
    }

    /**
     * onBeforeRunObligations()
     *
     * is the event handler called by the UCon just before sending obligations
     * during the evaluation runloop. The default implementation does nothing.
     *
     * @param request the request.
     * @param session the current session.
     */
    public void onBeforeRunObligations(PepRequestInterface request, PepSessionInterface session)
    {
        log.debug("dummy PIP processor called, ignoring");
    }

    /**
     * onAfterRunObligations()
     *
     * is the event handler invoked by the UCon just after having sent
     * obligations to the PEP. The default implementation does nothing.
     *
     * @param request the request.
     * @param session the current session.
     * @param ack the answer back from the pep. It's format is undefined.
     */
    public void onAfterRunObligations(PepRequestInterface request, PepSessionInterface session, Object ack)
                {
        log.debug("dummy PIP processor called, ignoring");
    }

    /**
     * newSharedAttribute() creates a new shared PEP attribute. The new
     * attribute is shared, meaning that its value changes may affect all
     * sessions that will be using it. If a shared attribute with the same id
     * and category already exists, it will be overwritten by the new values.
     * The PIP itself will be able to recover the created attribute later. An
     * attribute may also be managed (handled by this PIP asynchronously), or
     * unmanaged (handled automatically by the UCon itself, on a refresh
     * technique basis). By default, all attributes are managed. To make them
     * unmanaged, simply set the expires field to a valid timestamp.
     *
     * @param id the id of the subject.
     * @param type the type description of data this attribute is holding.
     * @param value the value held by this attribute.
     * @param issuer the issuer of the attribute.
     * @param category the category (subject, resource, or action).
     * @return the new PEP request attribute.
     */

    @Override
    public PepAttributeInterface newSharedAttribute(String id, String type, String value, String issuer, String category) {
        UconAttribute u = dal.getSharedAttribute(category, id);
        if (u == null) {
            PepAttribute a = new PepAttribute(id, type, value, issuer, category, uuid);
            u = UconAttribute.newInstance(a);
        } else {
            u.setType(type);
            u.setValue(value);
            u.setIssuer(issuer);
        }
        assert (u.getFactory() != null);
        //assert(u.getRowId() != null);
        return u;
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
    public String getUUID() {
        return getClass().getCanonicalName();
    }
    
    public UConInterface getUCon() {
        return ucon;
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
