/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.impl;

import it.cnr.iit.retrail.server.UConInterface;
import it.cnr.iit.retrail.server.UConProtocol;
import it.cnr.iit.retrail.commons.Client;
import it.cnr.iit.retrail.commons.DomUtils;
import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.commons.impl.PepRequest;
import it.cnr.iit.retrail.commons.impl.PepResponse;
import it.cnr.iit.retrail.commons.impl.PepSession;
import it.cnr.iit.retrail.commons.Server;
import it.cnr.iit.retrail.commons.Status;
import it.cnr.iit.retrail.server.dal.UconAttribute;
import it.cnr.iit.retrail.server.dal.DAL;
import it.cnr.iit.retrail.server.dal.UconRequest;
import it.cnr.iit.retrail.server.dal.UconSession;
import it.cnr.iit.retrail.server.pip.PIPInterface;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.NoResultException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.xmlrpc.XmlRpcException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.wso2.balana.PDP;
import org.wso2.balana.PDPConfig;
import org.wso2.balana.ctx.AbstractRequestCtx;
import org.wso2.balana.ctx.RequestCtxFactory;
import org.wso2.balana.ctx.ResponseCtx;
import org.wso2.balana.finder.AttributeFinder;
import org.wso2.balana.finder.AttributeFinderModule;
import org.wso2.balana.finder.PolicyFinder;
import org.wso2.balana.finder.PolicyFinderModule;
import org.wso2.balana.finder.impl.CurrentEnvModule;
import org.wso2.balana.finder.impl.SelectorModule;
import org.wso2.balana.finder.impl.StreamBasedPolicyFinderModule;
import org.wso2.balana.finder.impl.URLBasedPolicyFinderModule;

public class UCon extends Server implements UConInterface, UConProtocol {

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
    
    private final PDP pdp[] = new PDP[PolicyEnum.values().length];
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
            }
        }
        return singleton;
    }

    public static UConProtocol getProtocolInstance() {
        getInstance();
        return singleton;
    }

    private UCon() throws UnknownHostException, XmlRpcException, IOException, URISyntaxException {
        super(new URL(defaultUrlString), UConProtocolProxy.class);
        log.warn("loading builtin policies (permit anything)");
        for(PolicyEnum p: PolicyEnum.values())
            setPolicy(p, (URL)null);
        dal = DAL.getInstance();
    }

    private PDP newPDP(PolicyFinderModule module) {
        PolicyFinder policyFinder = new PolicyFinder();
        Set<PolicyFinderModule> policyFinderModules = new HashSet<>();
        policyFinderModules.add(module);
        policyFinder.setModules(policyFinderModules);
        AttributeFinder attributeFinder = new AttributeFinder();
        List<AttributeFinderModule> attributeFinderModules = new ArrayList<>();
        SelectorModule selectorModule = new SelectorModule();
        CurrentEnvModule currentEnvModule = new CurrentEnvModule();
        attributeFinderModules.add(selectorModule);
        attributeFinderModules.add(currentEnvModule);
        attributeFinder.setModules(attributeFinderModules);
        PDPConfig pdpConfig = new PDPConfig(attributeFinder, policyFinder, null, false);
        return new PDP(pdpConfig);
    }

    private PDP newPDP(URL location) {
        Set<URL> locationSet = new HashSet<>();
        locationSet.add(location); //set the correct policy location
        URLBasedPolicyFinderModule URLBasedPolicyFinderModule = new URLBasedPolicyFinderModule(locationSet);
        return newPDP(URLBasedPolicyFinderModule);
    }

    private PDP newPDP(InputStream stream) {
        StreamBasedPolicyFinderModule streamBasedPolicyFinderModule = new StreamBasedPolicyFinderModule(stream);
        return newPDP(streamBasedPolicyFinderModule);
    }
    
    private PDP newPDP(String resourceName) {
        InputStream stream = UCon.class.getResourceAsStream(resourceName);
        return newPDP(stream);
    }
    
    @Override
    public void setPolicy(PolicyEnum p, InputStream is) {
        log.warn("changing policy");
        pdp[p.ordinal()] = is == null? 
                newPDP(defaultPolicyNames[p.ordinal()])
                : newPDP(is);
        if(p == PolicyEnum.ON && inited) {
            Collection<UconSession> sessions = dal.listSessions(Status.ONGOING);
            if(sessions.size() > 0) {
               log.warn("UCon already running, reevaluating {} currently opened sessions", sessions.size());
                reevaluateSessions(sessions);
            }
        }

    }
        
    @Override
    public final void setPolicy(PolicyEnum p, URL url) {
        log.warn("setting {} policy to {}", p, url == null? "default" : url);
        pdp[p.ordinal()] = url == null? 
                newPDP(defaultPolicyNames[p.ordinal()])
                : newPDP(url);
        if(p == PolicyEnum.ON && inited) {
            Collection<UconSession> sessions = dal.listSessions(Status.ONGOING);
            if(sessions.size() > 0) {
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
        try {
            if(customId != null) {
                dal.getSessionByCustomId(customId);
                throw new RuntimeException("session "+customId+" already exists!");
            }
        } catch(NoResultException e) {
            // pass
        }
        URL pepUrl = new URL(pepUrlString);
        UconRequest uconRequest = new UconRequest((Document) accessRequest);
        
        // First enrich the request by calling the PIPs
        for (PIPInterface p : pip) {
            p.onBeforeTryAccess(uconRequest);
        }
        // Now send the enriched request to the PDP
        // UUID is attributed by the dal, as well as customId if not given.
        Document responseDocument = access(uconRequest, pdp[PolicyEnum.PRE.ordinal()]);
        UconSession session = new UconSession(responseDocument);
        session.setCustomId(customId);
        session.setPepUrl(pepUrlString);
        session.setUconUrl(myUrl);
        session.setStatus(session.getDecision() == PepResponse.DecisionEnum.Permit? 
                Status.TRY : Status.REJECTED);
        for (PIPInterface p : pip) {
            p.onAfterTryAccess(uconRequest, session);
        }
        if (session.getDecision() == PepResponse.DecisionEnum.Permit) {
           UconSession uconSession = dal.startSession(session, uconRequest);
           session.setUuid(uconSession.getUuid());
           session.setCustomId(uconSession.getCustomId());
           assert(session.getUuid() != null && session.getUuid().length() > 0);
           assert(session.getCustomId() != null  && session.getCustomId().length() > 0);
           assert(session.getStatus() == Status.TRY);
        }
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
        uuid = getUuid(uuid, customId);
        UconSession session = dal.getSession(uuid, myUrl);
        if (session == null) {
            throw new RuntimeException("no session with uuid: " + uuid);
        }
        if (session.getStatus() != Status.TRY) {
            throw new RuntimeException(session + " must be in TRY state to perform this operation");
        }
        assert(session.getUuid() != null && session.getUuid().length() > 0);
        session.setUconUrl(myUrl);
        assert(session.getUconUrl() != null);
        assert(session.getCustomId() != null  && session.getCustomId().length() > 0);
        
        // rebuild pepAccessRequest for PIP's onBeforeStartAccess argument
        UconRequest uconRequest = rebuildUconRequest(session);
        refreshUconRequest(uconRequest, session);
        for (PIPInterface p : pip) {
            p.onBeforeStartAccess(uconRequest, session);
        }
        Document responseDocument = access(uconRequest, pdp[PolicyEnum.TRYSTART.ordinal()]);
        session.setResponse(responseDocument);
        session.setStatus(session.getDecision() == PepResponse.DecisionEnum.Permit?
            Status.ONGOING : Status.TRY);
        dal.saveSession(session, uconRequest);
        for (PIPInterface p : pip) {
            p.onAfterStartAccess(uconRequest, session);
        }
        dal.saveSession(session, uconRequest);
        assert(session.getUuid() != null);
        assert(session.getUconUrl() != null);
        return session.toXacml3Element();
    }

    @Override
    public Node endAccess(String uuid, String customId) throws Exception {
        log.info("uuid={}, customId={}", uuid, customId);
        uuid = getUuid(uuid, customId);
        UconSession session = dal.getSession(uuid, myUrl);
        if (session == null) {
            throw new RuntimeException("no session with uuid: " + uuid);
        }
        PDP pdpInUse = null;
        switch(session.getStatus()) {
            case TRY:
                pdpInUse = pdp[PolicyEnum.TRYEND.ordinal()];
                break;
            case ONGOING:
                pdpInUse = pdp[PolicyEnum.POST.ordinal()];
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
        if(session.getStatus() == Status.REVOKED) {
            session.setDecision(PepResponse.DecisionEnum.Permit);
        } else {
            Document responseDocument = access(uconRequest, pdpInUse);
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
        return session.toXacml3Element();
    }

    @Override
    public Node heartbeat(String pepUrl, List<String> sessionsList) throws Exception {
        log.debug("called, with url: " + pepUrl);
        return heartbeat(new URL(pepUrl), sessionsList);
    }

    private Document access(PepRequest accessRequest, PDP p) {
        Document accessResponse = null;
        try {
            Element xacmlRequest = accessRequest.toElement();
            AbstractRequestCtx request = RequestCtxFactory.getFactory().getRequestCtx(xacmlRequest);
            ResponseCtx response = p.evaluate(request);
            String responseString = response.encode();
            log.info("ACCESS UCON {}", responseString);
            accessResponse = DomUtils.read(responseString);
            log.info("ACCESS UCON {}", DomUtils.toString(accessResponse));
        } catch (Exception ex) {
            log.error("Unexpected exception {}: {}", ex, ex.getMessage());
        }
        return accessResponse;
    }

    private Document revokeAccess(URL pepUrl, UconSession session) throws Exception {
        // revoke session on db
        UconRequest uconRequest = rebuildUconRequest(session);
        refreshUconRequest(uconRequest, session);
        for (PIPInterface p : pip) {
            p.onBeforeRevokeAccess(uconRequest, session);
        }
        UconSession uconSession = dal.revokeSession(session);
        for (PIPInterface p : pip) {
            p.onAfterRevokeAccess(uconRequest, uconSession);
        }
        uconSession = (UconSession) dal.save(uconSession);
        // create client
        log.warn("invoking PEP at " + pepUrl + " to revoke " + uconSession);
        Client client = new Client(pepUrl);
        // remote call. TODO: should consider error handling
        session.setStatus(Status.REVOKED);
        Object[] params = new Object[]{session.toXacml3Element()};
        Document doc = (Document) client.execute("PEP.revokeAccess", params);
        return doc;
    }

    private Document runObligations(URL pepUrl, UconSession uconSession) throws Exception {
        // revoke session on db
        UconRequest uconRequest = rebuildUconRequest(uconSession);
        refreshUconRequest(uconRequest, uconSession);
        for (PIPInterface p : pip) {
            p.onBeforeRunObligations(uconRequest, uconSession);
        }

        // create client
        log.warn("invoking PEP at " + pepUrl + " to send obligations for " + uconSession);
        Client client = new Client(pepUrl);
        // remote call. TODO: should consider error handling
        Object[] params = new Object[]{uconSession.toXacml3Element()};
        Document doc = (Document) client.execute("PEP.runObligations", params);
        // TODO: check error
        for (PIPInterface p : pip) {
            p.onAfterRunObligations(uconRequest, uconSession);
        }
        uconSession = (UconSession) dal.save(uconSession);
        return doc;
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
            for(Iterator<String> i = sessionsList.iterator(); isNotKnownByClient && i.hasNext(); ) {
                String uuid = i.next();
                isNotKnownByClient = !uuid.equals(uconSession.getUuid());
                if(!isNotKnownByClient)
                    i.remove();
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

    private void reevaluateSessions(Collection<UconSession> involvedSessions) {
        for (UconSession involvedSession : involvedSessions) {
            try {
                log.debug("involved session: {}", involvedSession);
                // Rebuild PEP request without expired attributes 
                UconRequest uconRequest = rebuildUconRequest(involvedSession);
                // refresh the request the ren-evaluate.
                refreshUconRequest(uconRequest, involvedSession);
                // Now make PDP evaluate the request
                log.debug("evaluating request");
                Document responseDocument = access(uconRequest, pdp[PolicyEnum.ON.ordinal()]);
                involvedSession.setResponse(responseDocument);
                boolean mustRevoke = involvedSession.getDecision() != PepResponse.DecisionEnum.Permit;
                // Explicitly revoke access if anything went wrong
                if (mustRevoke) {
                    log.warn("revoking {}", involvedSession);
                    URL pepUrl = new URL(involvedSession.getPepUrl());
                    revokeAccess(pepUrl, involvedSession);
                } else if(involvedSession.getObligations().size() > 0) {
                    URL pepUrl = new URL(involvedSession.getPepUrl());
                    runObligations(pepUrl, involvedSession);
                } else {
                    log.info("updating {}", involvedSession);
                    dal.saveSession(involvedSession, uconRequest);
                }
            } catch (Exception ex) {
                log.error(ex.getMessage());
            }
        }
    }

    @Override
    public void notifyChanges(Collection<PepAttributeInterface> changedAttributes) throws Exception {
        // Gather all the sessions that involve the changed attributes
        log.info("{} attributes changed, updating DAL", changedAttributes.size());
        Collection<UconSession> involvedSessions = new HashSet<>();
        for(PepAttributeInterface a: changedAttributes) {
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

        // Re-evaluating possible outdated sessions
        reevaluateSessions(outdatedSessions);

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
        if(sessions.size() > 0) {
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
        if(singleton == this)
            singleton = null;
        log.warn("completing shutdown procedure for the UCon service");
        super.term();
        log.warn("UCon shutdown");
    }

}
