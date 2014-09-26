/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.impl;

import it.cnr.iit.retrail.server.UConInterface;
import it.cnr.iit.retrail.server.UConProtocol;
import it.cnr.iit.retrail.commons.Client;
import it.cnr.iit.retrail.commons.DomUtils;
import it.cnr.iit.retrail.commons.PepAccessRequest;
import it.cnr.iit.retrail.commons.PepAccessResponse;
import it.cnr.iit.retrail.commons.PepRequestAttribute;
import it.cnr.iit.retrail.commons.PepSession;
import it.cnr.iit.retrail.commons.Server;
import it.cnr.iit.retrail.server.db.Attribute;
import it.cnr.iit.retrail.server.db.DAL;
import it.cnr.iit.retrail.server.db.UconSession;
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
import org.apache.commons.beanutils.BeanUtils;
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

    /**
     *
     * @return
     */
    public static UConInterface getInstance() {
        if (singleton == null) {
            try {
                log.warn("loading builtin policies (permit anything)");
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

    public static UConInterface getInstance(URL pre, URL on, URL post) {
        if (singleton == null) {
            try {
                singleton = new UCon(pre, on, post);
            } catch (XmlRpcException | IOException | URISyntaxException e) {
                log.error(e.getMessage());
            }
        } else {
            throw new RuntimeException("UCon already initialized!");
        }
        return singleton;
    }

    public static UConProtocol getProtocolInstance() {
        getInstance();
        return singleton;
    }
    
    private UCon(URL pre, URL on, URL post) throws UnknownHostException, XmlRpcException, IOException, URISyntaxException {
        super(new URL(defaultUrlString), UConProtocolProxy.class);
        log.info("pre policy URL: {}, on policy URL: {}", pre, on);
        pdp[PdpEnum.PRE] = newPDP(pre);
        pdp[PdpEnum.ON] = newPDP(on);
        pdp[PdpEnum.POST] = newPDP(post);
        dal = DAL.getInstance();
    }

    private UCon(InputStream pre, InputStream on, InputStream post) throws UnknownHostException, XmlRpcException, IOException, URISyntaxException {
        super(new URL(defaultUrlString), UConProtocolProxy.class);
        log.info("loading policies by streams");
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
    public Node echo(Node node) throws TransformerConfigurationException, TransformerException {
        return node;
    }

    @Override
    public Node tryAccess(Node accessRequest, String pepUrlString, String customId) throws Exception {
        log.info("pepUrl={}, customId={}", pepUrlString, customId);
        PepAccessRequest request = new PepAccessRequest((Document) accessRequest);
        URL pepUrl = new URL(pepUrlString);
 
        // First enrich the request by calling the PIPs
        for (PIPInterface p : pip) {
            p.onBeforeTryAccess(request);
        }
        // Now send the enriched request to the PDP
        Document responseDocument = access(request, pdp[PdpEnum.PRE]);
        PepSession pepSession = new PepSession(responseDocument);
        if (pepSession.getDecision() == PepAccessResponse.DecisionEnum.Permit) {
            UconSession uconSession = dal.startSession(request, pepUrl, customId);
            updatePepSession(pepSession, uconSession);
        } else {
            pepSession.setStatus(PepSession.Status.REJECTED);
        }
        
        for (PIPInterface p : pip) {
            p.onAfterTryAccess(request, pepSession);
        }

        return pepSession.toXacml3Element();
    }

    @Override
    public Node assignCustomId(String uuid, String oldCustomId, String newCustomId) throws Exception {
        log.info("uuid={}, oldCustomId={}, newCustomId={}", uuid, oldCustomId, newCustomId);
        uuid = getUuid(uuid, oldCustomId);
        if (newCustomId == null || newCustomId.length() == 0) {
            throw new RuntimeException("invalid customId: " + uuid);
        }
        UconSession uconSession = dal.getSession(uuid);
        uconSession.setCustomId(newCustomId);
        dal.save(uconSession);
        PepSession pepSession = new PepSession(PepAccessResponse.DecisionEnum.Permit, "new customId assigned");
        updatePepSession(pepSession, uconSession);
        return pepSession.toXacml3Element();
    }

    @Override
    public Node startAccess(String uuid, String customId) throws Exception {
        log.info("uuid={}, customId={}", uuid, customId);
        uuid = getUuid(uuid, customId);
        UconSession uconSession = dal.getSession(uuid);
        if (uconSession == null) {
            throw new RuntimeException("no session with uuid: " + uuid);
        }
        if (uconSession.getStatus() != PepSession.Status.TRY) {
            throw new RuntimeException(uconSession + " must be in TRY state to perform this operation");
        }
        PepAccessRequest pepAccessRequest = rebuildPepAccessRequest(uconSession);
        // rebuild pepSession for PIP's onBeforeStartAccess argument
        PepSession pepSession = new PepSession(PepAccessResponse.DecisionEnum.Permit, null);
        updatePepSession(pepSession, uconSession);
        refreshPepAccessRequest(pepAccessRequest, pepSession);
        for (PIPInterface p : pip) {
            p.onBeforeStartAccess(pepAccessRequest, pepSession);
        }
        Document responseDocument = access(pepAccessRequest, pdp[PdpEnum.ON]);
        pepSession = new PepSession(responseDocument);
        if (pepSession.getDecision() == PepAccessResponse.DecisionEnum.Permit) {
            uconSession.setStatus(PepSession.Status.ONGOING);
            dal.updateSession(uconSession, pepAccessRequest);
        }
        updatePepSession(pepSession, uconSession);
        for (PIPInterface p : pip) {
            p.onAfterStartAccess(pepAccessRequest, pepSession);
        }
        return pepSession.toXacml3Element();
    }

    @Override
    public Node endAccess(String uuid, String customId) throws Exception {
        log.info("uuid={}, customId={}", uuid, customId);
        uuid = getUuid(uuid, customId);
        UconSession session = dal.getSession(uuid);
        PepSession response = new PepSession(PepAccessResponse.DecisionEnum.NotApplicable, "session ended");
        if (session != null) {
            updatePepSession(response, session);
            PepAccessRequest request = rebuildPepAccessRequest(session);
            for (PIPInterface p : pip) {
                p.onBeforeEndAccess(request, response);
            }
            response.setStatus(PepSession.Status.DELETED);
            dal.endSession(session);
            for (PIPInterface p : pip) {
                p.onAfterEndAccess(request, response);
            }
        } else {
            log.error("unknown session with uuid: {}", uuid);
            throw new RuntimeException("unknown session with uuid: " + uuid);
        }
        return response.toXacml3Element();
    }

    @Override
    public Node heartbeat(String pepUrl, List<String> sessionsList) throws Exception {
        log.debug("called, with url: " + pepUrl);
        return heartbeat(new URL(pepUrl), sessionsList);
    }

    private Document access(PepAccessRequest accessRequest, PDP p) {
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
        UconSession uconSession = dal.getSession(pepSession.getUuid());
        if (uconSession == null) {
            throw new RuntimeException("cannot find session with uuid=" + pepSession.getUuid());
        }
        PepAccessRequest pepAccessRequest = rebuildPepAccessRequest(uconSession);
        refreshPepAccessRequest(pepAccessRequest, pepSession);
        for (PIPInterface p : pip) {
            p.onBeforeRevokeAccess(pepAccessRequest, pepSession);
        }
        uconSession = dal.revokeSession(uconSession);
        updatePepSession(pepSession, uconSession);
        for (PIPInterface p : pip) {
            p.onAfterRevokeAccess(pepAccessRequest, pepSession);
        }
        // create client
        log.warn("invoking PEP at " + pepUrl + " to revoke " + pepSession);
        Client client = new Client(pepUrl);
        // remote call. TODO: should consider error handling
        Object[] params = new Object[]{pepSession.toXacml3Element()};
        Document doc = (Document) client.execute("PEP.revokeAccess", params);
        return doc;
    }

    private PepAccessRequest rebuildPepAccessRequest(UconSession uconSession) {
        // Rebuild the PEP request with valid attributes only.
        // Puts expired ones in the expiredAttributes collection.
        log.debug("" + uconSession);
        Date now = new Date();
        PepAccessRequest accessRequest = new PepAccessRequest();
        for (Attribute a : uconSession.getAttributes()) {
            PepRequestAttribute pepAttribute = new PepRequestAttribute(a.getId(), a.getType(), a.getValue(), a.getIssuer(), a.getCategory(), a.getFactory());
            accessRequest.add(pepAttribute);
        }
        return accessRequest;
    }

    private void refreshPepAccessRequest(PepAccessRequest accessRequest, PepSession session) {
        // TODO: should call only pips for changed attributes
        log.debug("refreshing request attributes");

        for (PIPInterface p : pip) {
            p.refresh(accessRequest, session);
        }
    }

    private void updatePepSession(PepSession pepSession, UconSession uconSession) throws Exception {
        BeanUtils.copyProperties(pepSession, uconSession);
        pepSession.setUconUrl(myUrl);
    }

    protected Document heartbeat(URL pepUrl, List<String> sessionsList) throws Exception {
        Collection<UconSession> sessions = dal.listSessions(pepUrl);
        Document doc = DomUtils.newDocument();
        Element responses = doc.createElement("Responses");
        doc.appendChild(responses);
        Date now = new Date();
        // Permit sessions unknown to the client.
        for (UconSession session : sessions) {
            boolean isNotKnownByClient = !sessionsList.remove(session.getUuid());
            if (isNotKnownByClient) {
                PepAccessRequest pepAccessRequest = rebuildPepAccessRequest(session);
                // Reevaluate request for safety
                //PepSession pepSession = evaluateRequest(pepAccessRequest);
                PepSession pepSession = new PepSession(PepAccessResponse.DecisionEnum.Permit, "recoverable session");
                updatePepSession(pepSession, session);
                log.warn("found session unknown to client: " + pepSession);
                Node n = doc.adoptNode(pepSession.toXacml3Element());
                // including enriched request information
                n.appendChild(doc.adoptNode(pepAccessRequest.toElement()));
                responses.appendChild(n);
            }
            // "Touch" session
            session.setLastSeen(now);
            dal.save(session);
        }
        // Revoke sessions known by the client, but unknown to the server.
        for (String uuid : sessionsList) {
            PepSession pepSession = new PepSession(PepAccessResponse.DecisionEnum.NotApplicable, "Unexistent session");
            pepSession.setUuid(uuid);
            pepSession.setStatus(PepSession.Status.UNKNOWN);
            pepSession.setUconUrl(myUrl);
            Node n = doc.adoptNode(pepSession.toXacml3Element());
            responses.appendChild(n);
        }
        return doc;
    }

    private void reevaluateSessions(Collection<UconSession> involvedSessions) {
        for (UconSession involvedSession : involvedSessions) {
            PepSession pepSession = null;
            try {
                log.debug("involved session: {}", involvedSession);
                // Rebuild PEP request without expired attributes 
                PepAccessRequest pepAccessRequest = rebuildPepAccessRequest(involvedSession);
                // refresh the request the ren-evaluate.
                pepSession = new PepSession(PepAccessResponse.DecisionEnum.Permit, null);
                updatePepSession(pepSession, involvedSession);
                refreshPepAccessRequest(pepAccessRequest, pepSession);
                // Now make PDP evaluate the request
                log.debug("evaluating request");
                Document responseDocument = access(pepAccessRequest, pdp[PdpEnum.ON]);
                pepSession = new PepSession(responseDocument);
                updatePepSession(pepSession, involvedSession);
                log.debug("evaluated request: " + pepSession);
                boolean mustRevoke = pepSession.getDecision() != PepAccessResponse.DecisionEnum.Permit;
                // Explicitly revoke access if anything went wrong
                if (mustRevoke) {
                    log.warn("revoking {}", pepSession);
                    URL pepUrl = new URL(involvedSession.getPepUrl());
                    revokeAccess(pepUrl, pepSession);
                } else {
                    log.info("updating {}", pepSession);
                    dal.updateSession(involvedSession, pepAccessRequest);
                }
            } catch (Exception ex) {
                log.error(ex.getMessage());
            }
        }
    }

    @Override
    public void notifyChanges(Collection<PepRequestAttribute> changedAttributes) throws Exception {
        // Gather all the sessions that involve the changed attributes
        log.info("{} attributes changed, updating db", changedAttributes.size());
        Collection<UconSession> involvedSessions = dal.updateAttributes(changedAttributes);
        reevaluateSessions(involvedSessions);
        log.debug("done (total sessions: {})", dal.listSessions().size());
    }

    @Override
    public void notifyChanges(PepRequestAttribute changedAttribute) throws Exception {
        Collection<PepRequestAttribute> attributes = new ArrayList(1);
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

}
