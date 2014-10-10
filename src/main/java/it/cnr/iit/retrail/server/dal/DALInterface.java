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

    UconSession endSession(UconSession uconSession);

    UconAttribute getSharedAttribute(String category, String id);

    UconSession getSession(String uuid, URL uconUrl);

    @Deprecated
    UconSession getSessionByCustomId(String customId);

    Collection<UconAttribute> listAttributes(URL pepUrl);

    Collection<UconAttribute> listAttributes();

    Collection<PepAttributeInterface> listManagedAttributes(String factory);

    Collection<UconSession> listOutdatedSessions();

    Collection<UconSession> listSessions();

    Collection<UconSession> listSessions(Status status);

    Collection<UconSession> listSessions(Date lastSeenBefore);

    Collection<UconSession> listSessions(URL pepUrl);

    Collection<PepAttributeInterface> listUnmanagedAttributes(String factory);

    void removeAttributesByFactory(String factory);

    UconSession revokeSession(UconSession uconSession);

    Object save(Object o);

    UconSession saveSession(UconSession uconSession, UconRequest uconRequest);

    UconSession startSession(UconSession uconSession, UconRequest uconRequest) throws Exception;

}
