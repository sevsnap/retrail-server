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

    UconSession revokeSession(UconSession uconSession);

    Object save(Object o);

    UconSession saveSession(UconSession uconSession, UconRequest uconRequest);

    UconSession startSession(UconSession uconSession, UconRequest uconRequest) throws Exception;

    UconSession endSession(UconSession uconSession);

    UconSession getSession(String uuid, URL uconUrl);

}
