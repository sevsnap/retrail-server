/*
 * CNR - IIT
 * Coded by: 2014-2015 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.impl;

import it.cnr.iit.retrail.server.behaviour.Behaviour;
import it.cnr.iit.retrail.server.UConInterface;
import it.cnr.iit.retrail.server.UConProtocol;
import it.cnr.iit.retrail.commons.DomUtils;
import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.commons.PepRequestInterface;
import it.cnr.iit.retrail.commons.impl.PepResponse;
import it.cnr.iit.retrail.commons.impl.PepSession;
import it.cnr.iit.retrail.commons.Server;
import it.cnr.iit.retrail.commons.StateType;
import it.cnr.iit.retrail.commons.impl.PepRequest;
import it.cnr.iit.retrail.server.behaviour.OngoingAccess;
import it.cnr.iit.retrail.server.behaviour.UConState;
import it.cnr.iit.retrail.server.dal.UconAttribute;
import it.cnr.iit.retrail.server.dal.DAL;
import it.cnr.iit.retrail.server.dal.DALInterface;
import it.cnr.iit.retrail.server.dal.UconRequest;
import it.cnr.iit.retrail.server.dal.UconSession;
import it.cnr.iit.retrail.server.pip.PIPChainInterface;
import it.cnr.iit.retrail.server.pip.SystemEvent;
import it.cnr.iit.retrail.server.pip.impl.PIPChain;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.persistence.NoResultException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class UCon extends Server implements UConInterface, UConProtocol {

    public final static String uri = "http://security.iit.cnr.it/retrail/ucon";
    public final static int version = 2;
    private final AsyncNotifier notifier;
    private int maxMissedHeartbeats = 1;

    private Behaviour automatonFactory;

    protected PIPChainInterface pipChain;
    protected final DAL dal;
    protected boolean inited = false;

    public boolean isInited() {
        return inited;
    }

    protected UCon(URL url) throws Exception {
        super(url, UConProtocolProxy.class);
        dal = DAL.getInstance();
        notifier = new AsyncNotifier(this);
        defaultConfiguration();
    }

    @Override
    public void loadConfiguration(InputStream is) throws Exception {
        Document config = DomUtils.read(is);
        DomUtils.setPropertyOnObjectNS(uri, "Property", config.getDocumentElement(), this);
        Element behaviourConfig = (Element) config.getElementsByTagNameNS(uri, "Behaviour").item(0);
        if (behaviourConfig == null) {
            throw new RuntimeException("missing mandatory ucon:Behaviour element");
        }
        automatonFactory = new Behaviour(this, behaviourConfig);
        Element pipChainConfig = (Element) config.getElementsByTagNameNS(uri, "PIPChain").item(0);
        if (pipChainConfig != null) {
            pipChain = new PIPChain(pipChainConfig);
            if (isInited()) {
                pipChain.init(this);
            }
        }
    }

    @Override
    public final void defaultConfiguration() throws Exception {
        InputStream uconConfigStream = getClass().getClassLoader().getResourceAsStream("ucon.xml");
        loadConfiguration(uconConfigStream);
    }

    @Override
    public Node echo(Node node) throws TransformerConfigurationException, TransformerException {
        return node;
    }

    @Override
    public Node apply(String actionName, String uuid, String customId, Object... args) throws Exception {
        UconSession session = null;
        if (uuid != null || customId != null) {
            session = getSession(uuid, customId);
        }
        UconSession response = null;
        dal.begin();
        try {
            response = automatonFactory.apply(session, actionName, args);
            dal.commit();
        } catch (Exception e) {
            log.error("while applying {} to {}: {}", actionName, session, e);
            dal.rollback();
            throw e;
        }
        return response.toXacml3Element();
    }

    @Override
    public Node tryAccess(Node accessRequest, String pepUrlString) throws Exception {
        return tryAccess(accessRequest, pepUrlString, null);
    }

    @Override
    public Node tryAccess(Node accessRequest, String pepUrlString, String customId) throws Exception {
        log.info("pepUrl={}, customId={}", pepUrlString, customId);
        long start = System.currentTimeMillis();

        // Check the customId is not already used
        try {
            if (customId != null) {
                dal.getSessionByCustomId(customId);
                throw new RuntimeException("session " + customId + " already exists!");
            }
        } catch (NoResultException e) {
            // pass
        }
        // Create new session in the dal.
        // UUID is attributed by the dal, as well as customId if not given.
        UconSession session = new UconSession();
        session.setCustomId(customId);
        session.setPepUrl(pepUrlString);
        session.setUconUrl(myUrl);
        session.setState((UConState) automatonFactory.getBegin());
        UconRequest request = new UconRequest((Document) accessRequest);
        UconSession response = null;
        dal.begin();
        try {
            session = dal.startSession(session, request);
            response = automatonFactory.apply(session, "tryAccess");
            dal.commit();
        } catch (Exception e) {
            log.error("while applying {} to {}: {}", "tryAccess", session, e);
            dal.rollback();
            throw e;
        }
        return response.toXacml3Element();
    }

    @Override
    public Node assignCustomId(String uuid, String oldCustomId, String newCustomId) throws Exception {
        log.info("uuid={}, oldCustomId={}, newCustomId={}", uuid, oldCustomId, newCustomId);
        if (newCustomId == null || newCustomId.length() == 0) {
            throw new RuntimeException("null or empty customId for: " + uuid);
        }
        try {
            dal.getSessionByCustomId(newCustomId);
            throw new RuntimeException("customId " + newCustomId + " already exists");
        } catch (NoResultException e) {
        }
        UconSession uconSession = getSession(uuid, oldCustomId);
        dal.begin();
        try {
            uconSession.setCustomId(newCustomId);
            uconSession = (UconSession) dal.save(uconSession);
            uconSession.setMessage("new customId assigned");
            uconSession.setUconUrl(myUrl);
            dal.commit();
        } catch (Exception e) {
            log.error("while assigning custom id {} to {}: {}", newCustomId, uconSession, e);
            dal.rollback();
            throw e;
        }
        //log.error("XXXX: {}", DomUtils.toString(uconSession.toXacml3Element()));
        return uconSession.toXacml3Element();
    }

    @Override
    public Node endAccess(String uuid) throws Exception {
        return endAccess(uuid, null);
    }

    @Override
    public Node endAccess(String uuid, String customId) throws Exception {
        Node response = apply("endAccess", uuid, customId);
        return response;
    }

    @Override
    public List<Node> endAccess(List<String> uuidList) throws Exception {
        return endAccess(uuidList, null);
    }

    @Override
    public List<Node> endAccess(List<String> uuidList, List<String> customIdList) throws Exception {
        List<Node> responses = new ArrayList<>(uuidList.size());
        for (String uuid : uuidList) {
            String customId = customIdList != null ? customIdList.remove(0) : null;
            Node responseElement = endAccess(uuid, customId);
            responses.add(responseElement);
        }
        return responses;
    }

    @Override
    public Node heartbeat(String pepUrl, List<String> sessionsList) throws Exception {
        log.debug("called, with url: " + pepUrl);
        return heartbeat(new URL(pepUrl), sessionsList);
    }

    protected Document heartbeat(URL pepUrl, List<String> sessionsList) throws Exception {
        Collection<UconSession> sessions = dal.listSessions(pepUrl);
        Document doc = DomUtils.newDocument();
        Element heartbeat = doc.createElement("Heartbeat");
        doc.appendChild(heartbeat);
        Element responses = doc.createElement("Responses");
        heartbeat.appendChild(responses);
        Element config = doc.createElement("Config");
        config.setAttribute("version", Integer.toString(version));
        config.setAttribute("watchdogPeriod", Integer.toString(getWatchdogPeriod()));
        config.setAttribute("maxMissedHeartbeats", Integer.toString(getMaxMissedHeartbeats()));
        heartbeat.appendChild(config);
        Date now = new Date();
        // Permit sessions unknown to the client.
        for (UconSession uconSession : sessions) {

            boolean isNotKnownByClient = true;
            for (Iterator<String> i = sessionsList.iterator(); isNotKnownByClient && i.hasNext();) {
                String uuid = i.next();
                isNotKnownByClient = !uuid.equals(uconSession.getUuid());
                if (!isNotKnownByClient) {
                    i.remove();
                }
            }
            if (isNotKnownByClient) {
                UconRequest uconRequest = dal.rebuildUconRequest(uconSession);
                PepResponse r = new PepSession(PepResponse.DecisionEnum.Permit, "recoverable session");
                log.warn("found session unknown to client: " + uconSession);
                Node n = doc.adoptNode(uconSession.toXacml3Element());
                // include enriched request information
                n.appendChild(doc.adoptNode(uconRequest.toElement()));
                responses.appendChild(n);
            }
            // "Touch" session
            uconSession.setLastSeen(now);
            dal.save(uconSession);
        }
        // Revoke sessions known by the client, but unknown to the server.
        for (String uuid : sessionsList) {
            PepSession pepSession = new PepSession(PepResponse.DecisionEnum.NotApplicable, "Unexistent session");
            pepSession.setUuid(uuid);
            pepSession.setStateType(StateType.UNKNOWN);
            log.error("PEP at {} told it has {}, that is unknown to me: replying with UNKNOWN status", pepUrl, pepSession);
            pepSession.setUconUrl(myUrl);
            Node n = doc.adoptNode(pepSession.toXacml3Element());
            responses.appendChild(n);
        }
        return doc;
    }

    public void reevaluateSessions(Collection<UconSession> involvedSessions) throws Exception {
        for (UconSession involvedSession : involvedSessions) {
            dal.begin();
            try {
                log.debug("involved session: {}", involvedSession);
                // Now make PDP evaluate the request    
                involvedSession = automatonFactory.apply(involvedSession, OngoingAccess.name);
                boolean mustRevoke = involvedSession.getDecision() != PepResponse.DecisionEnum.Permit;
                // Explicitly revoke access if anything went wrong
                if (mustRevoke) {
                    log.info("revoking {}", involvedSession);
                    URL pepUrl = new URL(involvedSession.getPepUrl());
                    notifier.revokeAccess(pepUrl, involvedSession);
                } else if (!involvedSession.getObligations().isEmpty()) {
                    URL pepUrl = new URL(involvedSession.getPepUrl());
                    notifier.runObligations(pepUrl, involvedSession);
                }
                dal.commit();
            } catch (Exception ex) {
                log.error("while reevaluating  " + involvedSession + ": " + ex.getMessage(), ex);
                dal.rollback();
            }
        }
    }

    @Override
    public Node applyChanges(Node xacmlRequest, String uuid) throws Exception {
        long start = System.currentTimeMillis();
        UconSession uconSession = dal.getSession(uuid, myUrl);
        // rebuild pepAccessRequest for PIP's event
        UconRequest uconRequest = dal.rebuildUconRequest(uconSession);
        // We create a fake PepRequest, which works just as a container for the
        // Attribute values and handles the grouping by category.
        PepRequestInterface container = new PepRequest((Document) xacmlRequest);

        dal.begin();
        try {
            // First send event to the PIPs
            pipChain.fireSystemEvent(new SystemEvent(SystemEvent.EventType.beforeApplyChanges, uconRequest, uconSession, container));
            // Ok we have a live container of attribute values grouped by 
            // category and id now. 
            // Replace old values with new ones.
            for (PepAttributeInterface a : container) {
                UconAttribute uconA = dal.newPrivateAttribute(a.getId(), a.getType(), a.getValue(), a.getIssuer(), a.getCategory(), uconSession, null);
                uconRequest.replace(uconA);
            }
            // We need to save them to the database.
            UconSession uconSession2 = dal.saveSession(uconSession, uconRequest);
            pipChain.fireSystemEvent(new SystemEvent(SystemEvent.EventType.afterApplyChanges, uconRequest, uconSession2));
            dal.saveSession(uconSession2, uconRequest);
            dal.commit();
        } catch (Exception e) {
            log.error("while applying changes to {}: {}", uconSession, e);
            dal.rollback();
            throw e;
        }

        // New values are saved. It's time to reevaluate the involved sessions.
        // To do this, we awake the watchdog thread.
        wakeup();
        uconSession.setMs(System.currentTimeMillis() - start);

        return uconSession.toXacml3Element();
    }

    @Override
    public Node applyChanges(Node xacmlRequest, String uuid, String customId) throws Exception {
        return applyChanges(xacmlRequest, getSession(uuid, customId).getUuid());
    }

    @Override
    public void notifyChanges(PepAttributeInterface changedAttribute) throws Exception {
        log.info("changed {}, updating DAL", changedAttribute);
        Collection<UconSession> involvedSessions;
        dal.begin();
        try {
            UconAttribute u = (UconAttribute) dal.save(changedAttribute);
            if (u.isShared()) {
                involvedSessions = dal.listSessions(StateType.ONGOING);
            } else {
                involvedSessions = new ArrayList<>(1);
                if(u.getSession().getStateType() == StateType.ONGOING)
                   involvedSessions.add((UconSession) u.getSession());
            }
            dal.commit();
        } catch (Exception e) {
            log.error("while saving changes: {}", e);
            dal.rollback();
            throw e;
        }
        reevaluateSessions(involvedSessions);
        log.debug("done (total sessions: {})", dal.listSessions().size());
    }

    @Override
    public void notifyChanges(Collection<PepAttributeInterface> changedAttributes) throws Exception {
        // Gather all the sessions that involve the changed attributes
        log.info("{} attributes changed, updating DAL", changedAttributes.size());
        Collection<UconSession> involvedSessions = new HashSet<>();
        dal.begin();
        try {
            for (PepAttributeInterface a : changedAttributes) {
                UconAttribute u = (UconAttribute) dal.save(a);
                if (u.isShared()) {
                    involvedSessions = dal.listSessions(StateType.ONGOING);
                    break;
                } else if(u.getSession().getStateType() == StateType.ONGOING)
                    involvedSessions.add(u.getSession());
            }
            dal.commit();
        } catch (Exception e) {
            log.error("while notifying changes: {}", e);
            dal.rollback();
            throw e;
        }
        reevaluateSessions(involvedSessions);
        log.debug("done (total sessions: {})", dal.listSessions().size());
    }

    private UconSession getSession(String uuid, String customId) {
        if (uuid != null) {
            return dal.getSession(uuid, myUrl);
        }
        return dal.getSessionByCustomId(customId);
    }

    @Override
    protected void watchdog() {
        // List all sessions that were not heartbeaten since at least maxMissedHeartbeats+1 periods
        int period = getWatchdogPeriod();
        if (period > 0) {
            Date now = new Date();
            Date lastSeenBefore = new Date(now.getTime() - 1000 * (getMaxMissedHeartbeats() + 1) * period);
            Collection<UconSession> expiredSessions = dal.listSessions(lastSeenBefore);
            // Remove them
            for (UconSession expiredSession : expiredSessions) {
                try {
                    log.warn("removing stale " + expiredSession);
                    endAccess(expiredSession.getUuid(), null);
                } catch (Exception ex) {
                    log.error("could not properly end stale {}: {}", expiredSession, ex.getMessage());
                }
            }
        }
        // Gather all ongoing sessions
        Collection<UconSession> outdatedSessions = dal.listSessions(StateType.ONGOING);
        try {
            // Re-evaluating possible outdated sessions
            reevaluateSessions(outdatedSessions);
        } catch (Exception ex) {
            log.error("while reevaluating sessions: {}", ex);
        }

        log.debug("OK (#sessions: {})", dal.listSessions().size());
    }

    @Override
    public void init() throws Exception {
        super.init();
        pipChain.init(this);
        notifier.start();
        Collection<UconSession> sessions = dal.listSessions(StateType.ONGOING);
        if (sessions.size() > 0) {
            log.warn("reevaluating {} previously opened sessions", sessions.size());
            reevaluateSessions(sessions);
        }
        inited = true;
    }

    @Override
    public void term() throws InterruptedException {
        pipChain.term();
        notifier.interrupt();
        notifier.join();
        log.info("completing shutdown procedure for the UCon service");
        super.term();
        UConFactory.releaseInstance(this);
        log.warn("UCon shutdown");
    }

    @Override
    public PIPChainInterface getPIPChain() {
        return pipChain;
    }

    @Override
    public DALInterface getDAL() {
        return dal;
    }

    @Override
    public void startRecording(File outputFile) throws Exception {
        notifier.startRecording(outputFile);
    }

    @Override
    public void continueRecording(File outputFile, long millis) throws Exception {
        notifier.continueRecording(outputFile, millis);
    }

    @Override
    public boolean isRecording() {
        return notifier.isRecording();
    }

    @Override
    public void stopRecording() {
        notifier.stopRecording();
    }

    @Override
    public SSLContext trustAllPeers() throws Exception {
        return notifier.trustAllPeers();
    }
    
    public int getMaxMissedHeartbeats() {
        return maxMissedHeartbeats;
    }

    public void setMaxMissedHeartbeats(int maxMissedHeartbeats) {
        log.warn("maxMissedHeartbeats set to {}", maxMissedHeartbeats);
        this.maxMissedHeartbeats = maxMissedHeartbeats;
    }

    @Override
    public Node apply(String actionName, String uuid, Object... args) throws Exception {
        return apply(actionName, uuid, null, args);
    }
}
