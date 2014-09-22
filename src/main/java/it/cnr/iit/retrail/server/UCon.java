package it.cnr.iit.retrail.server;

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
import org.wso2.balana.finder.impl.FileBasedPolicyFinderModule;
import org.wso2.balana.finder.impl.SelectorModule;
import org.wso2.balana.finder.impl.URLBasedPolicyFinderModule;

public class UCon extends Server {

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
    public static UCon getInstance() {
        if (singleton == null) {
            getInstance(
                    UCon.class.getResource("/META-INF/default-policies/pre"), 
                    UCon.class.getResource("/META-INF/default-policies/on"), 
                    UCon.class.getResource("/META-INF/default-policies/post")
            );
        }
        return singleton;
    }

    public static UCon getInstance(URL pre, URL on, URL post) {
        if (singleton == null) {
            try {
                singleton = new UCon(pre, on, post);
            } catch (XmlRpcException | IOException | URISyntaxException e) {
                log.error(e.getMessage());
            }
        } else throw new RuntimeException("UCon already initialized!");
        return singleton;
    }

    private UCon(URL pre, URL on, URL post) throws UnknownHostException, XmlRpcException, IOException, URISyntaxException {
        super(new URL(defaultUrlString), XmlRpc.class);
        log.info("pre policy URL: {}, on policy URL: {}", pre, on);
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

    public PepSession tryAccess(PepAccessRequest accessRequest, URL pepUrl, String customId) throws Exception {
        // First enrich the request by calling the PIPs
        for (PIPInterface p : pip) {
            p.onTryAccess(accessRequest);
        }
        // Now send the enriched request to the PDP
        Document responseDocument = access(accessRequest, pdp[PdpEnum.PRE]);
        PepSession pepSession = new PepSession(responseDocument);
        if (pepSession.decision == PepAccessResponse.DecisionEnum.Permit) {
            UconSession uconSession = dal.startSession(accessRequest, pepUrl, customId);
            updatePepSession(pepSession, uconSession);
        } else {
            for (PIPInterface p : pip) {
                p.onEndAccess(accessRequest);
            }
        }
        return pepSession;
    }

    public Node assignCustomId(String uuid, String customId) throws Exception {
        if (customId == null || customId.length() == 0) {
            throw new RuntimeException("invalid customId: " + uuid);
        }
        UconSession uconSession = dal.getSession(uuid);
        uconSession.setCustomId(customId);
        dal.save(uconSession);
        PepSession pepSession = new PepSession(PepAccessResponse.DecisionEnum.Permit, "new customId assigned");
        updatePepSession(pepSession, uconSession);
        return pepSession.toElement();
    }

    public PepSession startAccess(String uuid) throws Exception {
        // Now send the enriched request to the PDP
        UconSession uconSession = dal.getSession(uuid);
        if (uconSession == null) {
            throw new RuntimeException("no session with uuid: " + uuid);
        }
        if (uconSession.getStatus() != PepSession.Status.TRY) {
            throw new RuntimeException(uconSession + " must be in TRY state to perform this operation");
        }
        PepAccessRequest pepAccessRequest = rebuildPepAccessRequest(uconSession);
        refreshPepAccessRequest(pepAccessRequest);
        for (PIPInterface p : pip) {
            p.onStartAccess(pepAccessRequest);
        }
        Document responseDocument = access(pepAccessRequest, pdp[PdpEnum.ON]);
        PepSession pepSession = new PepSession(responseDocument);
        if (pepSession.decision == PepAccessResponse.DecisionEnum.Permit) {
            uconSession.setStatus(PepSession.Status.ONGOING);
            dal.updateSession(uconSession, pepAccessRequest);
        }
        updatePepSession(pepSession, uconSession);
        return pepSession;
    }

    private Document revokeAccess(URL pepUrl, PepSession pepSession) throws XmlRpcException {
        // revoke session on db
        UconSession uconSession = dal.getSession(pepSession.getUuid());
        if (uconSession == null) {
            throw new RuntimeException("cannot find session with uuid=" + pepSession.getUuid());
        }
        PepAccessRequest pepAccessRequest = rebuildPepAccessRequest(uconSession);
        refreshPepAccessRequest(pepAccessRequest);
        for (PIPInterface p : pip) {
            p.onRevokeAccess(pepAccessRequest);                
        }
        dal.revokeSession(uconSession);
        // create client
        log.warn("invoking PEP at " + pepUrl + " to revoke " + pepSession);
        Client client = new Client(pepUrl);
        // remote call. TODO: should consider error handling
        Object[] params = new Object[]{pepSession.toElement()};
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

    private void refreshPepAccessRequest(PepAccessRequest accessRequest) {
        // TODO: should call only pips for changed attributes
        log.debug("refreshing request attributes");
        for (PIPInterface p : pip) {
            p.refresh(accessRequest);
        }
    }

    private void updatePepSession(PepSession pepSession, UconSession uconSession) throws Exception {
        BeanUtils.copyProperties(pepSession, uconSession);
        pepSession.setUconUrl(myUrl);
    }

    public Document heartbeat(URL pepUrl, List<String> sessionsList) throws Exception {
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
                Node n = doc.adoptNode(pepSession.toElement());
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
            Node n = doc.adoptNode(pepSession.toElement());
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
                refreshPepAccessRequest(pepAccessRequest);
                // Now make PDP evaluate the request
                log.debug("evaluating request");
                Document responseDocument = access(pepAccessRequest, pdp[PdpEnum.ON]);
                pepSession = new PepSession(responseDocument);
                updatePepSession(pepSession, involvedSession);
                log.debug("evaluated request: " + pepSession);
                boolean mustRevoke = pepSession.decision != PepAccessResponse.DecisionEnum.Permit;
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

    public void notifyChanges(Collection<PepRequestAttribute> changedAttributes) throws Exception {
        // Gather all the sessions that involve the changed attributes
        log.info("{} attributes changed, updating db", changedAttributes.size());
        Collection<UconSession> involvedSessions = dal.updateAttributes(changedAttributes);
        reevaluateSessions(involvedSessions);
        log.debug("done (total sessions: {})", dal.listSessions().size());
    }

    public void notifyChanges(PepRequestAttribute changedAttribute) throws Exception {
        Collection<PepRequestAttribute> attributes = new ArrayList(1);
        attributes.add(changedAttribute);
        notifyChanges(attributes);
    }

    public Node endAccess(String uuid) throws Exception {
        UconSession session = dal.getSession(uuid);
        PepSession response = new PepSession(PepAccessResponse.DecisionEnum.NotApplicable, "session ended");
        if (session != null) {
            PepAccessRequest request = rebuildPepAccessRequest(session);
            for (PIPInterface p : pip) {
                p.onEndAccess(request);
            }
            updatePepSession(response, session);
            response.setStatus(PepSession.Status.DELETED);
            dal.endSession(session);
        } else {
            log.error("unknown session with uuid: {}", uuid);
            throw new RuntimeException("unknown session with uuid: "+uuid);
        }
        return response.toElement();
    }

    public String getUuid(String uuid, String customId) {
        if (uuid != null) {
            return uuid;
        }
        return dal.getSessionByCustomId(customId).getUuid();
    }

    @Override
    protected void watchdog() {
        // List all sessions that were not heartbeaten since at least 2 periods
        Date now = new Date();
        Date lastSeenBefore = new Date(now.getTime() - 1000 * 2 * watchdogPeriod);
        Collection<UconSession> expiredSessions = dal.listSessions(lastSeenBefore);
        // Remove them
        for (UconSession expiredSession : expiredSessions) {
            try {
                log.warn("removing stale " + expiredSession);
                endAccess(expiredSession.getUuid());
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
