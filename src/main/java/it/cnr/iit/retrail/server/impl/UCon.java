/*
 * CNR - IIT
 * Coded by: 2014-2015 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.impl;

import it.cnr.iit.retrail.server.behaviour.Behaviour;
import it.cnr.iit.retrail.server.UConInterface;
import it.cnr.iit.retrail.server.UConProtocol;
import it.cnr.iit.retrail.commons.impl.Client;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class UCon extends Server implements UConInterface, UConProtocol {

    public final static String uri = "http://security.iit.cnr.it/retrail/ucon";
    private File recorderFile = null;
    private boolean mustAppendToRecorderFile = false;
    private boolean mustRecorderTrustAllPeers = false;
    private long recorderMillis = 0;
    public int maxMissedHeartbeats = 1;
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
        defaultConfiguration();
    }

    @Override
    public void loadConfiguration(InputStream is) throws Exception {
        Document config = DomUtils.read(is);
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
    public Node startAccess(String uuid) throws Exception {
        return apply("startAccess", uuid, null);
    }

    @Override
    public Node startAccess(String uuid, String customId) throws Exception {
        return apply("startAccess", uuid, customId);
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

    private List<Element> revokeAccess(URL pepUrl, Collection<UconSession> sessions) throws Exception {
        List<Element> responses = new ArrayList<>(sessions.size());

        for (UconSession uconSession : sessions) {
            long start = System.currentTimeMillis();
            // XXX Rebuilding the request may be too heavy. 
            // We should provide a way to dynamically get it if the PIP needs it.
            pipChain.fireSystemEvent(new SystemEvent(SystemEvent.EventType.beforeRevokeAccess, null, uconSession));
            uconSession.setMs(System.currentTimeMillis() - start);
            Element sessionElement = uconSession.toXacml3Element();
            responses.add(sessionElement);
        }

        // TODO: check error
        Object ack = rpc(pepUrl, "PEP.revokeAccess", responses);

        for (UconSession uconSession : sessions) {
            pipChain.fireSystemEvent(new SystemEvent(SystemEvent.EventType.afterRevokeAccess, null, uconSession, ack));
            dal.save(uconSession);
        }
        return responses;
    }

    private void runObligations(URL pepUrl, Collection<UconSession> uconSessions) throws Exception {
        List<Element> responses = new ArrayList<>(uconSessions.size());
        Collection<UconSession> uconSessions2 = new ArrayList<>(uconSessions.size());
        for (UconSession uconSession : uconSessions) {
            // revoke session on db
            UconRequest uconRequest = dal.rebuildUconRequest(uconSession);
            pipChain.refresh(uconRequest, uconSession);
            pipChain.fireSystemEvent(new SystemEvent(SystemEvent.EventType.beforeRunObligations, uconRequest, uconSession));
            Element sessionElement = uconSession.toXacml3Element();
            responses.add(sessionElement);
            // save ucon session
            uconSession = (UconSession) dal.save(uconSession);
            uconSessions2.add(uconSession);
        }
        // TODO: check error
        // TODO: returned docs are currently ignored. 
        // should use them for some back ack
        Object ack = rpc(pepUrl, "PEP.runObligations", responses);
        for (UconSession uconSession : uconSessions2) {
            UconRequest uconRequest = dal.rebuildUconRequest(uconSession);
            pipChain.fireSystemEvent(new SystemEvent(SystemEvent.EventType.afterRunObligations, uconRequest, uconSession, ack));
            uconSession = (UconSession) dal.save(uconSession);
        }
    }

    protected Document heartbeat(URL pepUrl, List<String> sessionsList) throws Exception {
        Collection<UconSession> sessions = dal.listSessions(pepUrl);
        Document doc = DomUtils.newDocument();
        Element heartbeat = doc.createElement("Heartbeat");
        doc.appendChild(heartbeat);
        Element responses = doc.createElement("Responses");
        heartbeat.appendChild(responses);
        Element config = doc.createElement("Config");
        config.setAttribute("watchdogPeriod", Integer.toString(getWatchdogPeriod()));
        config.setAttribute("maxMissedHeartbeats", Integer.toString(maxMissedHeartbeats));
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

    private void flushRevocations(Map<URL, Collection<UconSession>> revokedSessionsMap) throws Exception {
        // Send  revocations
        for (URL pepUrl : revokedSessionsMap.keySet()) {
            revokeAccess(pepUrl, revokedSessionsMap.get(pepUrl));
        }
        revokedSessionsMap.clear();
    }

    private void flushObligations(Map<URL, Collection<UconSession>> obligationSessionsMap) throws Exception {
        // Send obligations 
        for (URL pepUrl : obligationSessionsMap.keySet()) {
            runObligations(pepUrl, obligationSessionsMap.get(pepUrl));
        }
        obligationSessionsMap.clear();
    }

    public void reevaluateSessions(Collection<UconSession> involvedSessions) throws Exception {
        dal.begin();
        try {
            Map<URL, Collection<UconSession>> revokedSessionsMap = new HashMap<>(involvedSessions.size());
            Map<URL, Collection<UconSession>> obligationSessionsMap = new HashMap<>(involvedSessions.size());
            int revoked = 0;
            int run = 0;
            for (UconSession involvedSession : involvedSessions) {
                try {
                    log.debug("involved session: {}", involvedSession);
                    // Now make PDP evaluate the request    
                    involvedSession = automatonFactory.apply(involvedSession, OngoingAccess.name);
                    boolean mustRevoke = involvedSession.getDecision() != PepResponse.DecisionEnum.Permit;
                    // Explicitly revoke access if anything went wrong
                    if (mustRevoke) {
                        log.info("revoking {}", involvedSession);
                        URL pepUrl = new URL(involvedSession.getPepUrl());
                        Collection<UconSession> revokedSessions = revokedSessionsMap.get(pepUrl);
                        if (revokedSessions == null) {
                            revokedSessions = new ArrayList<>();
                            revokedSessionsMap.put(pepUrl, revokedSessions);
                        }
                        revokedSessions.add(involvedSession);
                        revoked++;
                    } else if (!involvedSession.getObligations().isEmpty()) {
                        URL pepUrl = new URL(involvedSession.getPepUrl());
                        Collection<UconSession> obligationSessions = revokedSessionsMap.get(pepUrl);
                        if (obligationSessions == null) {
                            obligationSessions = new ArrayList<>();
                            obligationSessionsMap.put(pepUrl, obligationSessions);
                        }
                        obligationSessions.add(involvedSession);
                        run++;
                    //} else {
                        //log.info("updating {}", involvedSession);
                        //dal.saveSession(involvedSession, uconRequest);
                    }

                } catch (Exception ex) {
                    log.error("while reevaluating  " + involvedSession + ": " + ex.getMessage(), ex);
                }
                // send partial data
                if (revoked > 9) {
                    flushRevocations(revokedSessionsMap);
                    revoked = 0;
                }
                if (run > 9) {
                    flushObligations(obligationSessionsMap);
                    run = 0;
                }
            }
            flushRevocations(revokedSessionsMap);
            flushObligations(obligationSessionsMap);
            dal.commit();
        } catch (Exception e) {
            dal.rollback();
            log.error("while reevaluating sessions: {}", e);
            throw e;
        }
    }

    @Override
    public void notifyChanges(Collection<PepAttributeInterface> changedAttributes) throws Exception {
        // Gather all the sessions that involve the changed attributes
        log.info("{} attributes changed, updating DAL", changedAttributes.size());
        Collection<UconSession> involvedSessions = new HashSet<>();
        dal.begin();
        boolean mustReevaluateAll = false;
        try {
            for (PepAttributeInterface a : changedAttributes) {
                UconAttribute u = (UconAttribute) dal.save(a);
                if (u.isShared()) {
                    involvedSessions = dal.listSessions(StateType.ONGOING);
                    break;
                }
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
            Date lastSeenBefore = new Date(now.getTime() - 1000 * (maxMissedHeartbeats + 1) * period);
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
        // Gather all the sessions that involve expired attributes
        Collection<UconSession> outdatedSessions = period > 0 ? dal.listOutdatedSessions() : dal.listSessions(StateType.ONGOING);
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
        log.warn("completing shutdown procedure for the UCon service");
        super.term();
        UConFactory.releaseInstance(this);
        log.warn("UCon shutdown");
    }

    private Object[] rpc(URL pepUrl, String api, List<Element> responses) throws Exception {
        // create client
        log.warn("invoking {} at {}", api, pepUrl);
        Client client = new Client(pepUrl);
        if (mustRecorderTrustAllPeers) {
            client.trustAllPeers();
        }
        if (recorderFile != null) {
            if (mustAppendToRecorderFile) {
                client.continueRecording(recorderFile, recorderMillis);
            } else {
                client.startRecording(recorderFile);
                mustAppendToRecorderFile = true;
            }
        }
        // remote call. TODO: should consider error handling
        Object[] params = new Object[]{responses};
        Object[] rv = (Object[]) client.execute(api, params);
        if (recorderFile != null) {
            recorderMillis = client.getMillis();
            client.stopRecording();
        }
        return rv;
    }

    @Override
    public void startRecording(File outputFile) throws Exception {
        recorderFile = outputFile;
        mustAppendToRecorderFile = false;
        recorderMillis = 0;
    }

    @Override
    public void continueRecording(File outputFile, long millis) throws Exception {
        recorderFile = outputFile;
        mustAppendToRecorderFile = true;
        recorderMillis = millis;
    }

    @Override
    public boolean isRecording() {
        return recorderFile != null;
    }

    @Override
    public void stopRecording() {
        recorderFile = null;
        mustAppendToRecorderFile = false;
    }

    @Override
    public SSLContext trustAllPeers() throws Exception {
        mustRecorderTrustAllPeers = true;
        // XXX TODO just emulated by this call. So no SSL context.
        // Should use the right ssl context.
        return null;
    }

    @Override
    public PIPChainInterface getPIPChain() {
        return pipChain;
    }

    @Override
    public DALInterface getDAL() {
        return dal;
    }
}
