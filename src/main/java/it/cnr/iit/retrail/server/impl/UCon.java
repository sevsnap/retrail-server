/*
 * CNR - IIT
 * Coded by: 2014-2015 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.impl;

import it.cnr.iit.retrail.server.UConInterface;
import it.cnr.iit.retrail.server.UConProtocol;
import it.cnr.iit.retrail.commons.impl.Client;
import it.cnr.iit.retrail.commons.DomUtils;
import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.commons.impl.PepResponse;
import it.cnr.iit.retrail.commons.impl.PepSession;
import it.cnr.iit.retrail.commons.Server;
import it.cnr.iit.retrail.commons.Status;
import it.cnr.iit.retrail.server.dal.UconAttribute;
import it.cnr.iit.retrail.server.dal.DAL;
import it.cnr.iit.retrail.server.dal.UconRequest;
import it.cnr.iit.retrail.server.dal.UconSession;
import it.cnr.iit.retrail.server.pip.PIPInterface;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
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
import org.apache.xmlrpc.XmlRpcException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class UCon extends Server implements UConInterface, UConProtocol {
    private File recorderFile = null;
    private boolean mustAppendToRecorderFile = false;
    private boolean mustRecorderTrustAllPeers = false;
    private long recorderMillis = 0;
    public int maxMissedHeartbeats = 1;

    private static final String defaultUrlString = "http://localhost:8080";
    private static final String defaultPolicyNames[] = {
        "/META-INF/default-policies/pre.xml",
        "/META-INF/default-policies/trystart.xml",
        "/META-INF/default-policies/tryend.xml",
        "/META-INF/default-policies/on.xml",
        "/META-INF/default-policies/post.xml"
    };

    private static UCon singleton;

    private final PDPPool pdpPool[] = new PDPPool[PolicyEnum.values().length];
    public List<PIPInterface> pip = new ArrayList<>();
    public Map<String, PIPInterface> pipNameToInstanceMap = new HashMap<>();
    private final DAL dal;
    private boolean inited = false;

    /**
     *
     * @return
     */
    public static UConInterface getInstance() {
        if (singleton == null) {
            try {
                singleton = new UCon();
            } catch (XmlRpcException | IOException | URISyntaxException e) {
                log.error(e.getMessage());
            } catch (Exception e) {
                log.error("while creating UCon: {}", e.getMessage());
            }
        }
        return singleton;
    }

    public static UConInterface getInstance(URL url) {
        if (singleton == null) {
            try {
                singleton = new UCon(url);
            } catch (XmlRpcException | IOException | URISyntaxException e) {
                log.error(e.getMessage());
            } catch (Exception e) {
                log.error("while creating UCon with URL {}: {}", url, e.getMessage());
            }
        }
        return singleton;
    }

    public static UConProtocol getProtocolInstance() {
        getInstance();
        return singleton;
    }

    private UCon(URL url) throws Exception {
        super(url, UConProtocolProxy.class);
        log.warn("loading builtin policies (permit anything)");
        for (PolicyEnum p : PolicyEnum.values()) {
            setPolicy(p, (URL) null);
        }
        dal = DAL.getInstance();
    }

    private UCon() throws Exception {
        super(new URL(defaultUrlString), UConProtocolProxy.class);
        log.warn("loading builtin policies (permit anything)");
        for (PolicyEnum p : PolicyEnum.values()) {
            setPolicy(p, (URL) null);
        }
        dal = DAL.getInstance();
    }

    @Override
    public final void setPolicy(PolicyEnum p, URL url) throws Exception {
        if (url == null) {
            log.warn("creating pool with default {} policy", p);
            InputStream stream = UCon.class.getResourceAsStream(defaultPolicyNames[p.ordinal()]);
            pdpPool[p.ordinal()] = new PDPPool(stream);
        } else {
            log.warn("creating pool for policy {} at URL {}", p, url);
            pdpPool[p.ordinal()] = new PDPPool(url);
        }
        if (p == PolicyEnum.ON && inited) {
            Collection<UconSession> sessions = dal.listSessions(Status.ONGOING);
            if (sessions.size() > 0) {
                log.warn("UCon already running, reevaluating {} currently opened sessions", sessions.size());
                reevaluateSessions(sessions);
            }
        }
    }

    @Override
    public final void setPolicy(PolicyEnum p, InputStream stream) throws Exception {
        if (stream == null) {
            log.warn("creating pool with default {} policy", p);
            stream = UCon.class.getResourceAsStream(defaultPolicyNames[p.ordinal()]);
        } else {
            log.warn("creating pool for policy {} with resource stream {}", p, stream);
        }
        pdpPool[p.ordinal()] = new PDPPool(stream);
        if (p == PolicyEnum.ON && inited) {
            Collection<UconSession> sessions = dal.listSessions(Status.ONGOING);
            if (sessions.size() > 0) {
                log.warn("UCon already running, reevaluating {} currently opened sessions", sessions.size());
                reevaluateSessions(sessions);
            }
        }
    }

    @Override
    public Node echo(Node node) throws TransformerConfigurationException, TransformerException {
        return node;
    }

    @Override
    public Node tryAccess(Node accessRequest, String pepUrlString, String customId) throws Exception {
        log.info("pepUrl={}, customId={}", pepUrlString, customId);
        long start = System.currentTimeMillis();
        try {
            if (customId != null) {
                dal.getSessionByCustomId(customId);
                throw new RuntimeException("session " + customId + " already exists!");
            }
        } catch (NoResultException e) {
            // pass
        }
        URL pepUrl = new URL(pepUrlString);
        UconRequest uconRequest = new UconRequest((Document) accessRequest);
        log.debug("xacml request BEFORE enrichment: {}", DomUtils.toString(uconRequest.toElement()));

        // First enrich the request by calling the PIPs
        for (PIPInterface p : pip) {
            p.onBeforeTryAccess(uconRequest);
        }
        // Now send the enriched request to the PDP
        // UUID is attributed by the dal, as well as customId if not given.

        Document responseDocument = pdpPool[PolicyEnum.PRE.ordinal()].access(uconRequest);
        UconSession session = new UconSession(responseDocument);
        session.setCustomId(customId);
        session.setPepUrl(pepUrlString);
        session.setUconUrl(myUrl);
        session.setStatus(session.getDecision() == PepResponse.DecisionEnum.Permit
                ? Status.TRY : Status.REJECTED);
        for (PIPInterface p : pip) {
            p.onAfterTryAccess(uconRequest, session);
        }
        if (session.getDecision() == PepResponse.DecisionEnum.Permit) {
            UconSession uconSession = dal.startSession(session, uconRequest);
            session.setUuid(uconSession.getUuid());
            session.setCustomId(uconSession.getCustomId());
            assert (session.getUuid() != null && session.getUuid().length() > 0);
            assert (session.getCustomId() != null && session.getCustomId().length() > 0);
            assert (session.getStatus() == Status.TRY);
        }
        session.setMs(System.currentTimeMillis() - start);
        return session.toXacml3Element();
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
    public Node startAccess(String uuid, String customId) throws Exception {
        log.info("uuid={}, customId={}", uuid, customId);
        long start = System.currentTimeMillis();
        uuid = getUuid(uuid, customId);
        UconSession session = dal.getSession(uuid, myUrl);
        if (session == null) {
            throw new RuntimeException("no session with uuid: " + uuid);
        }
        if (session.getStatus() != Status.TRY) {
            throw new RuntimeException(session + " must be in TRY state to perform this operation");
        }
        assert (session.getUuid() != null && session.getUuid().length() > 0);
        session.setUconUrl(myUrl);
        assert (session.getUconUrl() != null);
        assert (session.getCustomId() != null && session.getCustomId().length() > 0);

        // rebuild pepAccessRequest for PIP's onBeforeStartAccess argument
        UconRequest uconRequest = rebuildUconRequest(session);
        refreshUconRequest(uconRequest, session);
        for (PIPInterface p : pip) {
            p.onBeforeStartAccess(uconRequest, session);
        }
        Document responseDocument = pdpPool[PolicyEnum.TRYSTART.ordinal()].access(uconRequest);
        session.setResponse(responseDocument);
        session.setStatus(session.getDecision() == PepResponse.DecisionEnum.Permit
                ? Status.ONGOING : Status.TRY);
        dal.saveSession(session, uconRequest);
        for (PIPInterface p : pip) {
            p.onAfterStartAccess(uconRequest, session);
        }
        dal.saveSession(session, uconRequest);
        assert (session.getUuid() != null);
        assert (session.getUconUrl() != null);
        session.setMs(System.currentTimeMillis() - start);
        return session.toXacml3Element();
    }

    @Override
    public Node endAccess(String uuid, String customId) throws Exception {
        log.info("uuid={}, customId={}", uuid, customId);
        long start = System.currentTimeMillis();
        uuid = getUuid(uuid, customId);
        UconSession session = dal.getSession(uuid, myUrl);
        if (session == null) {
            throw new RuntimeException("no session with uuid: " + uuid);
        }
        PDPPool pdpPoolInUse = pdpPool[PolicyEnum.POST.ordinal()];
        switch (session.getStatus()) {
            case TRY:
                pdpPoolInUse = pdpPool[PolicyEnum.TRYEND.ordinal()];
                break;
            case ONGOING:
                pdpPoolInUse = pdpPool[PolicyEnum.POST.ordinal()];
                break;
            case REVOKED:
                break;
            default:
                throw new RuntimeException(session + " must be in TRY, ONGOING, or REVOKED state to perform this operation");
        }

        UconRequest uconRequest = rebuildUconRequest(session);
        refreshUconRequest(uconRequest, session);
        for (PIPInterface p : pip) {
            p.onBeforeEndAccess(uconRequest, session);
        }
        if (session.getStatus() == Status.REVOKED) {
            session.setDecision(PepResponse.DecisionEnum.Permit);
        } else {
            Document responseDocument = pdpPoolInUse.access(uconRequest);
            session.setResponse(responseDocument);
        }
        if (session.getDecision() == PepResponse.DecisionEnum.Permit) {
            session.setStatus(Status.DELETED);
            dal.endSession(session);
        } else {
            dal.saveSession(session, uconRequest);
        }
        for (PIPInterface p : pip) {
            p.onAfterEndAccess(uconRequest, session);
        }
        session.setMs(System.currentTimeMillis() - start);
        return session.toXacml3Element();
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
            UconRequest uconRequest = rebuildUconRequest(uconSession);
            refreshUconRequest(uconRequest, uconSession);
            for (PIPInterface p : pip) {
                p.onBeforeRevokeAccess(uconRequest, uconSession);
            }
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

        rpc(pepUrl, "PEP.revokeAccess", responses);
        for (UconSession uconSession : uconSessions2) {
            UconRequest uconRequest = rebuildUconRequest(uconSession);
            for (PIPInterface p : pip) {
                p.onAfterRevokeAccess(uconRequest, uconSession);
            }
            uconSession = (UconSession) dal.save(uconSession);
        }   
        return responses;
    }

    private void runObligations(URL pepUrl, Collection<UconSession> uconSessions) throws Exception {
        List<Element> responses = new ArrayList<>(uconSessions.size());
        Collection<UconSession> uconSessions2 = new ArrayList<>(uconSessions.size());
        for (UconSession uconSession : uconSessions) {
            // revoke session on db
            UconRequest uconRequest = rebuildUconRequest(uconSession);
            refreshUconRequest(uconRequest, uconSession);
            for (PIPInterface p : pip) {
                p.onBeforeRunObligations(uconRequest, uconSession);
            }
            Element sessionElement = uconSession.toXacml3Element();
            responses.add(sessionElement);
            // save ucon session
            uconSession = (UconSession) dal.save(uconSession);
            uconSessions2.add(uconSession);
        }
        // TODO: check error
        // TODO: returned docs are currently ignored. 
        // should use them for some back ack
        rpc(pepUrl, "PEP.runObligations", responses);
        for (UconSession uconSession : uconSessions2) {
            UconRequest uconRequest = rebuildUconRequest(uconSession);
            for (PIPInterface p : pip) {
                p.onAfterRunObligations(uconRequest, uconSession);
            }
            uconSession = (UconSession) dal.save(uconSession);
        }        
    }

    private UconRequest rebuildUconRequest(UconSession uconSession) {
        log.debug("" + uconSession);
        UconRequest accessRequest = new UconRequest();
        for (UconAttribute a : uconSession.getAttributes()) {
            accessRequest.add(a);
        }
        return accessRequest;
    }

    private void refreshUconRequest(UconRequest accessRequest, UconSession session) {
        // TODO: should call only pips for changed attributes
        log.debug("refreshing request attributes");

        for (PIPInterface p : pip) {
            p.refresh(accessRequest, session);
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
        config.setAttribute("watchdogPeriod", Integer.toString(watchdogPeriod));
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
                UconRequest uconRequest = rebuildUconRequest(uconSession);
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

    private void reevaluateSessions(Collection<UconSession> involvedSessions) throws Exception {
        Map<URL, Collection<UconSession>> revokedSessionsMap = new HashMap<>(involvedSessions.size());
        Map<URL, Collection<UconSession>> obligationSessionsMap = new HashMap<>(involvedSessions.size());
        for (UconSession involvedSession : involvedSessions) {
            try {
                log.debug("involved session: {}", involvedSession);
                // Rebuild PEP request without expired attributes 
                UconRequest uconRequest = rebuildUconRequest(involvedSession);
                // refresh the request the ren-evaluate.
                refreshUconRequest(uconRequest, involvedSession);
                // Now make PDP evaluate the request
                log.debug("evaluating request");
                Document responseDocument = pdpPool[PolicyEnum.ON.ordinal()].access(uconRequest);
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
        Date now = new Date();
        Date lastSeenBefore = new Date(now.getTime() - 1000 * (maxMissedHeartbeats + 1) * watchdogPeriod);
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
        // Gather all the sessions that involve expired attributes
        Collection<UconSession> outdatedSessions = dal.listOutdatedSessions();
        try {
            // Re-evaluating possible outdated sessions
            reevaluateSessions(outdatedSessions);
        } catch (Exception ex) {
            log.error("while reevaluating sessions: {}", ex);
        }

        log.debug("OK (#sessions: {})", dal.listSessions().size());
    }

    @Override
    public synchronized void addPIP(PIPInterface p) {
        String uuid = p.getUUID();
        if (!pipNameToInstanceMap.containsKey(uuid)) {
            p.init();
            pipNameToInstanceMap.put(uuid, p);
            pip.add(p);
        } else {
            log.warn("{} already in filter chain -- ignoring", p);
        }
    }

    @Override
    public synchronized PIPInterface removePIP(PIPInterface p) {
        String uuid = p.getUUID();
        if (pipNameToInstanceMap.containsKey(uuid)) {
            p.term();
            pipNameToInstanceMap.remove(uuid);
            pip.remove(p);
        } else {
            log.warn("{} not in filter chain -- ignoring", p);
        }
        return p;
    }

    @Override
    public void init() throws Exception {
        super.init();
        Collection<UconSession> sessions = dal.listSessions(Status.ONGOING);
        if (sessions.size() > 0) {
            log.warn("reevaluating {} previously opened sessions", sessions.size());
            reevaluateSessions(sessions);
        }
        inited = true;
    }

    @Override
    public void term() throws InterruptedException {
        while (pip.size() > 0) {
            removePIP(pip.get(0));
        }
        if (singleton == this) {
            singleton = null;
        }
        log.warn("completing shutdown procedure for the UCon service");
        super.term();
        log.warn("UCon shutdown");
    }

    private Object[] rpc(URL pepUrl, String api, List<Element> responses) throws Exception {
        // create client
        log.warn("invoking {} at {}", api, pepUrl);
        Client client = new Client(pepUrl);
        if(mustRecorderTrustAllPeers)
            client.trustAllPeers();
        if(recorderFile != null) {
            if(mustAppendToRecorderFile)
                client.continueRecording(recorderFile, recorderMillis);
            else  {
                client.startRecording(recorderFile);
                mustAppendToRecorderFile = true;
            }
        }
        // remote call. TODO: should consider error handling
        Object[] params = new Object[]{responses};
        Object[] rv = (Object[]) client.execute(api, params);
        if(recorderFile != null) {
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

}
