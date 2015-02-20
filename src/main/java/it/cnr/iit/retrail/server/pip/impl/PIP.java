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
    public void fireBeforeActionEvent(Event e) {
        switch(e.action.getName()) {
            case "tryAccess":
                onBeforeTryAccess(e.request);
                break;
            case "startAccess":
                onBeforeStartAccess(e.request, e.session);
                break;
            case "runObligations":
                onBeforeRunObligations(e.request, e.session);
                break;
            case "applyChanges":
                onBeforeApplyChanges(e.request, e.session);
                break;
            case "revokeAccess":
                onBeforeRevokeAccess(e.request, e.session);
                break;
            case "endAccess":
                onBeforeEndAccess(e.request, e.session);
                break;
            default:
                throw new RuntimeException("while handling event "+e+": unknown action " + e.action);
        }
    }

    @Override
    public void fireAfterActionEvent(Event e) {
        switch(e.action.getName()) {
            case "tryAccess":
                onAfterTryAccess(e.request, e.session);
                break;
            case "startAccess":
                onAfterStartAccess(e.request, e.session);
                break;
            case "runObligations":
                onAfterRunObligations(e.request, e.session, e.ack);
                break;
            case "applyChanges":
                onAfterApplyChanges(e.request, e.session);
                break;
            case "revokeAccess":
                onAfterRevokeAccess(e.request, e.session, e.ack);
                break;
            case "endAccess":
                onAfterEndAccess(e.request, e.session);
                break;
            default:
                throw new RuntimeException("while handling event "+e+": unknown action " + e.action);
        } 
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
            case beforeApplyChanges:
                onBeforeApplyChanges(e.request, e.session);
                break;
            case afterApplyChanges:
                onAfterApplyChanges(e.request, e.session);
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
    public PepAttributeInterface newSharedAttribute(String id, String type, String value, String issuer, String category) {
        return dal.newSharedAttribute(id, type, value, issuer, category, uuid);
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
     * onBeforeApplyChanges()
     *
     * is the event handler called by the UCon just before updating some
     * attributes by the UCon side itself.
     * The default implementation does nothing.
     *
     * @param request the request.
     * @param session the current session.
     */
    public void onBeforeApplyChanges(PepRequestInterface request, PepSessionInterface session)
    {
        log.debug("dummy PIP processor called, ignoring");
    }

    /**
     * onAfterApplyChanges()
     *
     * is the event handler called by the UCon just after updating some
     * attributes by the UCon side itself.
     * The default implementation does nothing.
     *
     * @param request the request.
     * @param session the current session.
     */
    public void onAfterApplyChanges(PepRequestInterface request, PepSessionInterface session) {
        log.debug("dummy PIP processor called, ignoring");
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
