/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.pip;

import it.cnr.iit.retrail.commons.PepRequestInterface;
import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.commons.PepSessionInterface;
import it.cnr.iit.retrail.server.UConInterface;
import java.util.Collection;

/**
 *
 * @author oneadmin
 */
public interface PIPInterface {

    /**
     * getUUID()
     *
     * returns the universal unique identifier for this PIP instance. The
     * default implementation generates the identifier randomly. This behavior
     * may be overridden.
     *
     * @return
     */
    String getUUID();

    /**
     * init()
     *
     * is called by the UCon to allow the PIP for its own initialization. This
     * is called each time the PIP is inserted in a chain, so it is very
     * important that an init()-term() call leaves the PIP in a consistent and
     * recyclable environment. The default implementation does nothing.
     * @param uconInterface the ucon this pip belongs to.
     */
    void init(UConInterface uconInterface);

    boolean isInited();
    
    @Deprecated
    void fireEvent(Event e);
    
    void fireAfterActionEvent(Event e);
    void fireBeforeActionEvent(Event e);
    
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
    PepAttributeInterface newSharedAttribute(String id, String type, String value, String issuer, String category);

    /**
     * newPrivateAttribute() creates a new private PEP attribute managed by this
     * PIP. The attribute is private, meaning that its value changes will
     * eventually affect only the session that is using it. If a private
     * attribute for a session with the same id and category already exists, it
     * will be overwritten by the new values. The PIP itself will be able to
     * recover the created attribute later by using the listManagedAttributes()
     * API. An attribute may also be managed (handled by this PIP
     * asynchronously), or unmanaged (handled automatically by the UCon itself,
     * on a refresh technique basis). By default, all attributes are managed. To
     * make them unmanaged, simply set the expires field to a valid timestamp.
     *
     *
     * @param id the id of the subject.
     * @param type the type description of data this attribute is holding.
     * @param value the value held by this attribute.
     * @param issuer the issuer of the attribute.
     * @param parent the parent attribute. The category in inherited from this
     * attribute instance.
     * @return the new PEP request attribute.
     */
    PepAttributeInterface newPrivateAttribute(String id, String type, String value, String issuer, PepAttributeInterface parent);

    /**
     * listManagedAttributes()
     *
     * gets back a collection of PEP request attributes managed by this PIP,
     * that is the ones created via the newSharedAttribute() or
     * newPrivateAttribute() methods.
     *
     * @return the collection of attributes created by this PIP via
     * newSharedAttribute() or newPrivateAttribute().
     */
    Collection<PepAttributeInterface> listManagedAttributes();

    /**
     * listUnmanagedAttributes()
     *
     * gets back a collection of PEP request attributes managed by the UCon
     * itself. These attributes are handled by refreshing them when needed.
     *
     * @return the collection of attributes created by this PIP via
     * newSharedAttribute() or newPrivateAttribute() that are managed by the
     * UCon and not the PIP.
     */
    Collection<PepAttributeInterface> listUnmanagedAttributes();

    /**
     * refresh()
     *
     * is called by the UCon each time a request is going to be re-evaluated
     * through some policy. This allows the PIP to implement a simpler
     * synchronous technique in place of an asynchronous one (i.e. without using
     * notifyChanges()) for attributes update. Please note this means that the
     * refreshed/added attributes MUST NOT be notified, since this semantics is
     * implicit. The default implementation does nothing.
     *
     * @param accessRequest the request whose attributes are to be refreshed.
     * @param session the session related to the request.
     * @throws java.lang.Exception
     */
    void refresh(PepRequestInterface accessRequest, PepSessionInterface session) throws Exception;

    /**
     * term()
     *
     * is invoked by the UCon to allow PIP for custom termination. It's called
     * anytime it is removed from the UCon chain, or when the UCon itself is
     * terminated. The implementation must clean up the object and remove any
     * state resiliency from the component. It is really important that this is
     * done to allow an init()-term() cycle without any restriction. The default
     * implementation removes all shared attributes created by this module and
     * must be invoked by overloading implementation.
     */
    void term();

}
