/*
 * CNR - IIT
 * Coded by: 2014-2015 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.pip.impl;

import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.commons.PepRequestInterface;
import it.cnr.iit.retrail.commons.PepSessionInterface;
import it.cnr.iit.retrail.commons.StateType;
import it.cnr.iit.retrail.server.UConInterface;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kicco
 */

public class PIPSessions extends PIP {

    protected int sessions = 0;

    final public String id = "openSessions";
    final public String category = "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject";

    public PIPSessions() {
        super();
        this.log = LoggerFactory.getLogger(PIPSessions.class);
    }

    @Override
    public void init(UConInterface ucon) {
        super.init(ucon);
        sessions = 
                dal.listSessions(StateType.ONGOING).size()+
                dal.listSessions(StateType.REVOKED).size();
    }

    public int getSessions() {
        return sessions;
    }

    @Override
    public void onBeforeTryAccess(PepRequestInterface request) {
        log.info("Number of open sessions: " + sessions);
        PepAttributeInterface test = newSharedAttribute(id, "http://www.w3.org/2001/XMLSchema#integer", Integer.toString(sessions), "http://localhost:8080/federation-id-prov/saml", category);
        request.replace(test);
    }

    @Override
    public void onBeforeStartAccess(PepRequestInterface request, PepSessionInterface session) {
        sessions++;
        log.info("Number of open sessions incremented to: " + sessions);
        PepAttributeInterface test = newSharedAttribute(id, "http://www.w3.org/2001/XMLSchema#integer", Integer.toString(sessions), "http://localhost:8080/federation-id-prov/saml", category);
        request.replace(test);
    }

    @Override
    public void onAfterStartAccess(PepRequestInterface request, PepSessionInterface session) {
        if (session.getStateType() != StateType.ONGOING) {
            sessions--;
            assert (sessions >= 0);
            log.warn("Number of open sessions decremented to {} because session status = {}", sessions, session.getStateType());
            PepAttributeInterface test = newSharedAttribute(id, "http://www.w3.org/2001/XMLSchema#integer", Integer.toString(sessions), "http://localhost:8080/federation-id-prov/saml", category);
            request.replace(test);
        }
        log.info("Number of open sessions: {}, status = {}", sessions, session.getStateType());
    }

    @Override
    public void onBeforeEndAccess(PepRequestInterface request, PepSessionInterface session) {
        if (session.getStateType() != StateType.PASSIVE) { // FIXME was TRY
            sessions--;
            log.info("Number of open sessions decremented to {} because status = {}", sessions, session.getStateType());
            assert (sessions >= 0);
            PepAttributeInterface test = newSharedAttribute(id, "http://www.w3.org/2001/XMLSchema#integer", Integer.toString(sessions), "http://localhost:8080/federation-id-prov/saml", category);
            request.replace(test);
        }
        log.info("Number of open sessions: {}, status = {}", sessions, session.getStateType());
    }

}
