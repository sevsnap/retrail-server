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
import it.cnr.iit.retrail.server.dal.Attribute;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private static UCon singleton;

    private static class PdpEnum {

        static final int PRE = 0;
        static final int ON = 1;
        static final int POST = 2;
    }

    private final PDP pdp[] = new PDP[3];
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
                singleton = new UCon(
                        UCon.class.getResourceAsStream("/META-INF/default-policies/pre.xml"),
                        UCon.class.getResourceAsStream("/META-INF/default-policies/on.xml"),
                        UCon.class.getResourceAsStream("/META-INF/default-policies/post.xml")
                );
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

    private UCon(InputStream pre, InputStream on, InputStream post) throws UnknownHostException, XmlRpcException, IOException, URISyntaxException {
        super(new URL(defaultUrlString), UConProtocolProxy.class);
        log.warn("loading builtin policies (permit anything)");
        pdp[PdpEnum.PRE] = newPDP(pre);
        pdp[PdpEnum.ON] = newPDP(on);
        pdp[PdpEnum.POST] = newPDP(post);
        dal = DAL.getInstance();
    }

    private PDP newPDP(URL location) {
        PolicyFinder policyFinder = new PolicyFinder();
        Set<PolicyFinderModule> policyFinderModules = new HashSet<>();
        Set<URL> locationSet = new HashSet<>();
        locationSet.add(location); //set the correct policy location
        URLBasedPolicyFinderModule URLBasedPolicyFinderModule = new URLBasedPolicyFinderModule(locationSet);
        policyFinderModules.add(URLBasedPolicyFinderModule);
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

    private PDP newPDP(InputStream stream) {
        PolicyFinder policyFinder = new PolicyFinder();
        Set<PolicyFinderModule> policyFinderModules = new HashSet<>();
        StreamBasedPolicyFinderModule streamBasedPolicyFinderModule = new StreamBasedPolicyFinderModule(stream);
        policyFinderModules.add(streamBasedPolicyFinderModule);
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

    @Override
    public void setPreauthPolicy(InputStream pre) {
        log.warn("changing preauthorization policy");
        pdp[PdpEnum.PRE] = newPDP(pre);
    }
    
    @Override
    public void setOngoingPolicy(InputStream pre) {
        log.warn("changing ongoing policy");
        pdp[PdpEnum.ON] = newPDP(pre);
        if(inited) {
            Collection<UconSession> sessions = dal.listSessions(Status.ONGOING);
            if(sessions.size() > 0) {
               log.warn("UCon already running, reevaluating {} currently opened sessions", sessions.size());
                reevaluateSessions(sessions);
            }
        }
    }
    
    @Override
    public void setPreauthPolicy(URL pre) {
        log.warn("changing preauthorization policy to {}", pre);
        pdp[PdpEnum.PRE] = newPDP(pre);
    }
    
    @Override
    public void setOngoingPolicy(URL on) {
        log.warn("changing ongoing policy to {}", on);
        pdp[PdpEnum.ON] = newPDP(on);
        if(inited) {
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
        URL pepUrl = new URL(pepUrlString);
        UconRequest uconRequest = new UconRequest((Document) accessRequest);
        
        // First enrich the request by calling the PIPs
        for (PIPInterface p : pip) {
            p.onBeforeTryAccess(uconRequest);
        }
        // Now send the enriched request to the PDP
        // UUID is attributed by the dal, as well as customId if not given.
        Document responseDocument = access(uconRequest, pdp[PdpEnum.PRE]);
        UconSession uconSession = new UconSession(responseDocument);
        uconSession.setCustomId(customId);
        uconSession.setUconUrl(myUrl);
        uconSession.setStatus(uconSession.getDecision() == PepResponse.DecisionEnum.Permit? Status.TRY : Status.REJECTED);
        for (PIPInterface p : pip) {
            p.onAfterTryAccess(uconRequest, uconSession);
        }
        if (uconSession.getDecision() == PepResponse.DecisionEnum.Permit) {
           uconSession = dal.startSession(uconSession, uconRequest);
           assert(uconSession.getUuid() != null && uconSession.getUuid().length() > 0);
           assert(uconSession.getUconUrl() != null);
           assert(uconSession.getCustomId() != null  && uconSession.getCustomId().length() > 0);
           assert(uconSession.getStatus() == Status.TRY);
        }
        log.error("**** TOXACML: {}", DomUtils.toString(uconSession.toXacml3Element()));
        return uconSession.toXacml3Element();
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
        log.error("**** XXXAAA TOXACML: {}", DomUtils.toString(uconSession.toXacml3Element()));
        return uconSession.toXacml3Element();
    }

    @Override
    public Node startAccess(String uuid, String customId) throws Exception {
        log.info("uuid={}, customId={}", uuid, customId);
        uuid = getUuid(uuid, customId);
        UconSession uconSession = dal.getSession(uuid, myUrl);
        if (uconSession == null) {
            throw new RuntimeException("no session with uuid: " + uuid);
        }
        if (uconSession.getStatus() != Status.TRY) {
            throw new RuntimeException(uconSession + " must be in TRY state to perform this operation");
        }
        assert(uconSession.getUuid() != null && uconSession.getUuid().length() > 0);
        uconSession.setUconUrl(myUrl);
        assert(uconSession.getUconUrl() != null);
        assert(uconSession.getCustomId() != null  && uconSession.getCustomId().length() > 0);
        
        // rebuild pepAccessRequest for PIP's onBeforeStartAccess argument
        UconRequest uconRequest = rebuildUconRequest(uconSession);
        refreshUconRequest(uconRequest, uconSession);
        for (PIPInterface p : pip) {
            p.onBeforeStartAccess(uconRequest, uconSession);
        }
        Document responseDocument = access(uconRequest, pdp[PdpEnum.ON]);
        PepResponse r = new PepResponse(responseDocument);
        if (r.getDecision() == PepResponse.DecisionEnum.Permit) {
            uconSession.setStatus(Status.ONGOING);
        }
        for (PIPInterface p : pip) {
            p.onAfterStartAccess(uconRequest, uconSession);
        }
        dal.updateSession(uconSession, uconRequest);
        assert(uconSession.getUuid() != null);
        assert(uconSession.getUconUrl() != null);
        return uconSession.toXacml3Element();
    }

    @Override
    public Node endAccess(String uuid, String customId) throws Exception {
        log.info("uuid={}, customId={}", uuid, customId);
        uuid = getUuid(uuid, customId);
        UconSession uconSession = dal.getSession(uuid, myUrl);
        if (uconSession == null) {
            throw new RuntimeException("no session with uuid: " + uuid);
        }
        UconRequest uconRequest = rebuildUconRequest(uconSession);
        refreshUconRequest(uconRequest, uconSession);
        for (PIPInterface p : pip) {
            p.onBeforeEndAccess(uconRequest, uconSession);
        }
        if(uconSession.getStatus() != Status.REVOKED) {
            Document responseDocument = access(uconRequest, pdp[PdpEnum.POST]);
            PepResponse r = new PepResponse(responseDocument);
            uconSession.setDecision(r.getDecision());
            uconSession.setMessage(r.getMessage());
        } else 
            uconSession.setDecision(PepResponse.DecisionEnum.Permit);
        if (uconSession.getDecision() == PepResponse.DecisionEnum.Permit) {
            uconSession.setStatus(Status.DELETED);
            dal.endSession(uconSession);
        }
        for (PIPInterface p : pip) {
            p.onAfterEndAccess(uconRequest, uconSession);
        }
        return uconSession.toXacml3Element();
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
            accessResponse = DomUtils.read(responseString);
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
        return accessResponse;
    }

    private Document revokeAccess(URL pepUrl, PepSession pepSession) throws Exception {
        // revoke session on db
        UconSession uconSession = dal.getSession(pepSession.getUuid(), myUrl);
        if (uconSession == null) {
            throw new RuntimeException("cannot find session with uuid=" + pepSession.getUuid());
        }
        UconRequest uconRequest = rebuildUconRequest(uconSession);
        refreshUconRequest(uconRequest, uconSession);
        for (PIPInterface p : pip) {
            p.onBeforeRevokeAccess(uconRequest, uconSession);
        }
        uconSession = dal.revokeSession(uconSession);
        for (PIPInterface p : pip) {
            p.onAfterRevokeAccess(uconRequest, uconSession);
        }
        // create client
        log.warn("invoking PEP at " + pepUrl + " to revoke " + pepSession);
        Client client = new Client(pepUrl);
        // remote call. TODO: should consider error handling
        Object[] params = new Object[]{pepSession.toXacml3Element()};
        Document doc = (Document) client.execute("PEP.revokeAccess", params);
        return doc;
    }

    private UconRequest rebuildUconRequest(UconSession uconSession) {
        log.debug("" + uconSession);
        UconRequest accessRequest = new UconRequest();
        for (Attribute a : uconSession.getAttributes()) {
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
        Element responses = doc.createElement("Responses");
        doc.appendChild(responses);
        Date now = new Date();
        // Permit sessions unknown to the client.
        for (UconSession uconSession : sessions) {
            boolean isNotKnownByClient = !sessionsList.remove(uconSession.getUuid());
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
            log.error("*********** HEARTBEAT REVOKING: {}", myUrl);
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
                Document responseDocument = access(uconRequest, pdp[PdpEnum.ON]);
                PepResponse r = new PepResponse(responseDocument);
                log.debug("evaluated request: " + r);
                boolean mustRevoke = r.getDecision() != PepResponse.DecisionEnum.Permit;
                // Explicitly revoke access if anything went wrong
                if (mustRevoke) {
                    log.warn("revoking {}", involvedSession);
                    URL pepUrl = new URL(involvedSession.getPepUrl());
                    revokeAccess(pepUrl, involvedSession);
                } else {
                    log.info("updating {}", involvedSession);
                    dal.updateSession(involvedSession, uconRequest);
                }
            } catch (Exception ex) {
                log.error(ex.getMessage());
            }
        }
    }

    @Override
    public void notifyChanges(Collection<PepAttributeInterface> changedAttributes) throws Exception {
        // Gather all the sessions that involve the changed attributes
        log.info("{} attributes changed, updating db", changedAttributes.size());
        Collection<UconSession> involvedSessions = dal.updateAttributes(changedAttributes);
        reevaluateSessions(involvedSessions);
        log.debug("done (total sessions: {})", dal.listSessions().size());
    }

    @Override
    public void notifyChanges(PepAttributeInterface changedAttribute) throws Exception {
        Collection<PepAttributeInterface> attributes = new ArrayList(1);
        attributes.add(changedAttribute);
        notifyChanges(attributes);
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
                log.error("could not properly end {}: {}", expiredSession, ex.getMessage());
            }
        }
        // Gather all the sessions that involve expired attributes
        Collection<UconSession> outdatedSessions = dal.listOutdatedSessions();

        // Re-evaluating possible outdated sessions
        reevaluateSessions(outdatedSessions);

        log.debug("OK (sessions: {})", dal.listSessions().size());
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
        log.warn("completing shutdown procedure for the UCon service");
        super.term();
        log.warn("UCon shutdown");
    }

}
