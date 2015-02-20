/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.impl;

import it.cnr.iit.retrail.commons.Status;
import it.cnr.iit.retrail.commons.automata.Action;
import it.cnr.iit.retrail.commons.automata.StateInterface;
import it.cnr.iit.retrail.commons.impl.PepResponse;
import it.cnr.iit.retrail.server.dal.UconRequest;
import it.cnr.iit.retrail.server.dal.UconSession;
import it.cnr.iit.retrail.server.pip.Event;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 *
 * @author oneadmin
 */

public class UConAction extends Action {
    protected final UCon ucon;
    protected PDPPool pdpPool;
    private final String name;
    
    public UConAction(StateInterface targetState, String name) {
        super(targetState);
        ucon = (UCon) targetState.getAutomaton();
        this.name = name;
    }
    
    protected String getPolicyName() {
        return getTargetState().getName().toLowerCase()+getName().toLowerCase();
    }

    public final void setPolicy(URL url) throws Exception {
        if (url == null) {
            log.warn("creating pool with default {} policy", getPolicyName());
            String path = "/META-INF/default-policies/"+getPolicyName()+".xml";
            InputStream stream = UCon.class.getResourceAsStream(path);
            pdpPool = new PDPPool(stream);
        } else {
            log.warn("creating pool for policy {} at URL {}", getPolicyName(), url);
            pdpPool = new PDPPool(url);
        }
        if (getTargetState().getName().equals("ONGOING") && ucon.inited) {
            Collection<UconSession> sessions = ucon.dal.listSessions(Status.ONGOING);
            if (sessions.size() > 0) {
                log.warn("UCon already running, reevaluating {} currently opened sessions", sessions.size());
                ucon.reevaluateSessions(sessions);
            }
        }
    }

    public final void setPolicy(InputStream stream) throws Exception {
        if (stream == null) {
            log.warn("creating pool with default {} policy", getPolicyName());
            String path = "/META-INF/default-policies/"+getPolicyName()+".xml";
            stream = UCon.class.getResourceAsStream(path);
        } else {
            log.warn("creating pool for policy {} with resource stream {}", getName(), stream);
        }
        pdpPool = new PDPPool(stream);
        if (getTargetState().getName().equals("ONGOING") && ucon.inited) {
            Collection<UconSession> sessions = ucon.dal.listSessions(Status.ONGOING);
            if (sessions.size() > 0) {
                log.warn("UCon already running, reevaluating {} currently opened sessions", sessions.size());
                ucon.reevaluateSessions(sessions);
            }
        }
    }

    public Document execute(UconRequest uconRequest, Object[] args) {
        return pdpPool.access(uconRequest);
    }
    
    private Node startAccess(String uuid, String customId) throws Exception {
        log.info("uuid={}, customId={}", uuid, customId);
        long start = System.currentTimeMillis();
        uuid = ucon.getUuid(uuid, customId);
        UconSession session = ucon.dal.getSession(uuid, ucon.myUrl);
        if (session == null) {
            throw new RuntimeException("no session with uuid: " + uuid);
        }
        assert (session.getUuid() != null && session.getUuid().length() > 0);
        session.setUconUrl(ucon.myUrl);
        assert (session.getUconUrl() != null);
        assert (session.getCustomId() != null && session.getCustomId().length() > 0);

        // rebuild pepAccessRequest for PIP's onBeforeStartAccess argument
        UconRequest uconRequest = ucon.rebuildUconRequest(session);
        ucon.pipChain.refresh(uconRequest, session);
        ucon.pipChain.fireEvent(new Event(Event.EventType.beforeStartAccess, uconRequest, session));
        Document responseDocument = pdpPool.access(uconRequest);
        session.setResponse(responseDocument);
        session.setStatus(session.getDecision() == PepResponse.DecisionEnum.Permit
                ? Status.ONGOING : Status.TRY);
        ucon.dal.saveSession(session, uconRequest);
        ucon.pipChain.fireEvent(new Event(Event.EventType.afterStartAccess, uconRequest, session));
        ucon.dal.saveSession(session, uconRequest);
        assert (session.getUuid() != null);
        assert (session.getUconUrl() != null);
        session.setMs(System.currentTimeMillis() - start);
        return session.toXacml3Element();
    }
}
