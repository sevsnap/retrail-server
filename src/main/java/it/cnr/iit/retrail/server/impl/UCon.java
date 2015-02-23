/*
 * CNR - IIT
 * Coded by: 2014-2015 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.impl;

import it.cnr.iit.retrail.server.automaton.PDPPool;
import it.cnr.iit.retrail.server.automaton.AutomatonFactory;
import it.cnr.iit.retrail.server.UConInterface;
import it.cnr.iit.retrail.server.UConProtocol;
import it.cnr.iit.retrail.commons.impl.Client;
import it.cnr.iit.retrail.commons.DomUtils;
import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.commons.PepRequestInterface;
import it.cnr.iit.retrail.commons.impl.PepResponse;
import it.cnr.iit.retrail.commons.impl.PepSession;
import it.cnr.iit.retrail.commons.Server;
import it.cnr.iit.retrail.commons.Status;
import it.cnr.iit.retrail.commons.automata.ActionInterface;
import it.cnr.iit.retrail.commons.automata.StateInterface;
import it.cnr.iit.retrail.commons.impl.PepRequest;
import it.cnr.iit.retrail.server.automaton.UConAutomaton;
import it.cnr.iit.retrail.server.automaton.UConState;
import it.cnr.iit.retrail.server.dal.UconAttribute;
import it.cnr.iit.retrail.server.dal.DAL;
import it.cnr.iit.retrail.server.dal.DALInterface;
import it.cnr.iit.retrail.server.dal.UconRequest;
import it.cnr.iit.retrail.server.dal.UconSession;
import it.cnr.iit.retrail.server.pip.Event;
import it.cnr.iit.retrail.server.pip.Event.EventType;
import it.cnr.iit.retrail.server.pip.PIPChainInterface;
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
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class UCon extends Server implements UConInterface, UConProtocol {

    private File recorderFile = null;
    private boolean mustAppendToRecorderFile = false;
    private boolean mustRecorderTrustAllPeers = false;
    private long recorderMillis = 0;
    public int maxMissedHeartbeats = 1;
    private final AutomatonFactory automatonFactory = new AutomatonFactory(this);

    protected final PIPChainInterface pipChain = new PIPChain();
    protected final DAL dal;
    protected boolean inited = false;

    public boolean isInited() {
        return inited;
    }

    protected UCon(URL url) throws Exception {
        super(url, UConProtocolProxy.class);
        dal = DAL.getInstance();
    }

    @Override
    public Node echo(Node node) throws TransformerConfigurationException, TransformerException {
        return node;
    }

    @Override
    public Node apply(String actionName, String uuid, String customId, Object... args) throws Exception {
        UconSession session = null;
        if(uuid != null || customId != null)
            session = dal.getSession(getUuid(uuid, customId), myUrl);
        Element response = automatonFactory.apply(session, actionName, args);
        return response;
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
        session.setStatus(Status.INIT);
        session = dal.startSession(session);

        // Obtain and use automaton instance.
        Element response = automatonFactory.apply(session, "tryAccess");
        return response;
    }

    @Override
    public Node assignCustomId(String uuid, String oldCustomId, String newCustomId) throws Exception {
        log.info("uuid={}, oldCustomId={}, newCustomId={}", uuid, oldCustomId, newCustomId);
        uuid = getUuid(uuid, oldCustomId);
        if (newCustomId == null || newCustomId.length() == 0) {
            throw new RuntimeException("invalid customId: " + uuid);
        }
        UconSession uconSession = dal.getSession(uuid, myUrl);
        uconSession.setCustomId(newCustomId);
        // save, but don't recover new saved object (transients are reset)
        dal.save(uconSession);
        uconSession.setMessage("new customId assigned");
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
        Collection<UconSession> uconSessions2 = new ArrayList<>(sessions.size());
        // revoke sessions on db
        for (UconSession uconSession : sessions) {
            long start = System.currentTimeMillis();
            UconRequest uconRequest = dal.rebuildUconRequest(uconSession);
            pipChain.refresh(uconRequest, uconSession);
            pipChain.fireEvent(new Event(EventType.beforeRevokeAccess, uconRequest, uconSession));
            UconSession uconSession2 = dal.revokeSession(uconSession);
            uconSession.setStatus(Status.REVOKED);
            uconSession.setMs(System.currentTimeMillis() - start);
            Element sessionElement = uconSession.toXacml3Element();
            responses.add(sessionElement);
            uconSessions2.add(uconSession2);
        }
        // TODO: check error
        // TODO: returned docs are currently ignored. 
        // should use them for some back ack

        Object ack = rpc(pepUrl, "PEP.revokeAccess", responses);
        for (UconSession uconSession : uconSessions2) {
            UconRequest uconRequest = dal.rebuildUconRequest(uconSession);
            pipChain.fireEvent(new Event(EventType.afterRevokeAccess, uconRequest, uconSession, ack));
            uconSession = (UconSession) dal.save(uconSession);
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
            pipChain.fireEvent(new Event(EventType.beforeRunObligations, uconRequest, uconSession));
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
            pipChain.fireEvent(new Event(EventType.afterRunObligations, uconRequest, uconSession, ack));
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
            pepSession.setStatus(Status.UNKNOWN);
            log.error("PEP at {} told it has {}, that is unknown to me: replying with UNKNOWN status", pepUrl, pepSession);
            pepSession.setUconUrl(myUrl);
            Node n = doc.adoptNode(pepSession.toXacml3Element());
            responses.appendChild(n);
        }
        return doc;
    }

    public void reevaluateSessions(Collection<UconSession> involvedSessions) throws Exception {
        Map<URL, Collection<UconSession>> revokedSessionsMap = new HashMap<>(involvedSessions.size());
        Map<URL, Collection<UconSession>> obligationSessionsMap = new HashMap<>(involvedSessions.size());
        for (UconSession involvedSession : involvedSessions) {
            try {
                log.debug("involved session: {}", involvedSession);
                // Rebuild PEP request without expired attributes 
                UconRequest uconRequest = dal.rebuildUconRequest(involvedSession);
                // refresh the request then re-evaluate.
                pipChain.refresh(uconRequest, involvedSession);
                // Now make PDP evaluate the request
                log.debug("evaluating request");                
                Document responseDocument = automatonFactory.getOngoingAccessPDPPool().access(uconRequest);
                involvedSession.setResponse(responseDocument);
                boolean mustRevoke = involvedSession.getDecision() != PepResponse.DecisionEnum.Permit;
                // Explicitly revoke access if anything went wrong
                if (mustRevoke) {
                    log.warn("revoking {}", involvedSession);
                    URL pepUrl = new URL(involvedSession.getPepUrl());
                    Collection<UconSession> revokedSessions = revokedSessionsMap.get(pepUrl);
                    if (revokedSessions == null) {
                        revokedSessions = new ArrayList<>();
                        revokedSessionsMap.put(pepUrl, revokedSessions);
                    }
                    revokedSessions.add(involvedSession);
                } else if (involvedSession.getObligations().size() > 0) {
                    URL pepUrl = new URL(involvedSession.getPepUrl());
                    Collection<UconSession> obligationSessions = revokedSessionsMap.get(pepUrl);
                    if (obligationSessions == null) {
                        obligationSessions = new ArrayList<>();
                        obligationSessionsMap.put(pepUrl, obligationSessions);
                    }
                    obligationSessions.add(involvedSession);
                } else {
                    log.info("updating {}", involvedSession);
                    dal.saveSession(involvedSession, uconRequest);
                }
            } catch (Exception ex) {
                log.error(ex.getMessage());
            }
        }
        for (URL pepUrl : revokedSessionsMap.keySet()) {
            revokeAccess(pepUrl, revokedSessionsMap.get(pepUrl));
        }
        for (URL pepUrl : obligationSessionsMap.keySet()) {
            runObligations(pepUrl, obligationSessionsMap.get(pepUrl));
        }
    }

    @Override
    public void notifyChanges(Collection<PepAttributeInterface> changedAttributes) throws Exception {
        // Gather all the sessions that involve the changed attributes
        log.info("{} attributes changed, updating DAL", changedAttributes.size());
        Collection<UconSession> involvedSessions = new HashSet<>();
        for (PepAttributeInterface a : changedAttributes) {
            UconAttribute u = (UconAttribute) dal.save(a);
            involvedSessions.addAll(u.getSessions());
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
        // First send event to the PIPs
        pipChain.fireEvent(new Event(EventType.beforeApplyChanges, uconRequest, uconSession, container));

        // Ok we have a live container of attribute values grouped by 
        // category and id now. 
        // Replace old values with new ones.
        for (PepAttributeInterface a : container) {
            UconAttribute uconA = dal.newSharedAttribute(a.getId(), a.getType(), a.getValue(), a.getIssuer(), a.getCategory(), "FAKENESS");
            uconRequest.replace(uconA);
        }

        // We need to save them to the database.
        UconSession uconSession2 = dal.saveSession(uconSession, uconRequest);
        pipChain.fireEvent(new Event(EventType.afterApplyChanges, uconRequest, uconSession2));
        dal.saveSession(uconSession2, uconRequest);
        // New values are saved. It's time to reevaluate the involved sessions.
        wakeup();
        uconSession.setMs(System.currentTimeMillis() - start);
        return uconSession.toXacml3Element();
    }

    @Override
    public Node applyChanges(Node xacmlRequest, String uuid, String customId) throws Exception {
        return applyChanges(xacmlRequest, getUuid(uuid, customId));
    }

    @Override
    public void notifyChanges(PepAttributeInterface changedAttribute) throws Exception {
        log.info("changed {}, updating DAL", changedAttribute);
        UconAttribute u = (UconAttribute) dal.save(changedAttribute);
        Collection<UconSession> involvedSessions = u.getSessions();
        reevaluateSessions(involvedSessions);
        log.debug("done (total sessions: {})", dal.listSessions().size());
    }

    public String getUuid(String uuid, String customId) {
        if (uuid != null) {
            return uuid;
        }
        return dal.getSessionByCustomId(customId).getUuid();
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
        Collection<UconSession> outdatedSessions = period > 0 ? dal.listOutdatedSessions() : dal.listSessions(Status.ONGOING);
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
        Collection<UconSession> sessions = dal.listSessions(Status.ONGOING);
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
