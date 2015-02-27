/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.dal;

import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.commons.Status;
import java.net.URL;
import java.util.Collection;
import java.util.Date;

/**
 *
 * @author oneadmin
 */
public interface DALInterface {
    
    UconAttribute newPrivateAttribute(String id, String type, String value, String issuer, UconAttribute parent, String uuid);
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
     * @param uuid the factory uuid.
     * @return the new PEP request attribute.
     */
    UconAttribute newSharedAttribute(String id, String type, String value, String issuer, String category, String uuid);

    UconAttribute getSharedAttribute(String category, String id);

    Collection<UconAttribute> listAttributes(URL pepUrl);

    Collection<UconAttribute> listAttributes();

    Collection<UconAttribute> listAttributes(String category, String id);

    Collection<PepAttributeInterface> listManagedAttributes(String factory);

    Collection<PepAttributeInterface> listUnmanagedAttributes(String factory);

    void removeAttributesByFactory(String factory);

    @Deprecated
    UconSession getSessionByCustomId(String customId);

    Collection<UconSession> listOutdatedSessions();

    Collection<UconSession> listSessions();

    Collection<UconSession> listSessions(Status status);

    Collection<UconSession> listSessions(Date lastSeenBefore);

    Collection<UconSession> listSessions(URL pepUrl);

    Object save(Object o);
    
    Collection<?> saveCollection(Collection<?> o);

    UconSession saveSession(UconSession uconSession, UconRequest uconRequest);
        
    UconSession startSession(UconSession uconSession, UconRequest uconRequest) throws Exception;

    UconSession endSession(UconSession uconSession);

    UconSession getSession(String uuid, URL uconUrl);
    
    UconRequest rebuildUconRequest(UconSession uconSession);

}
