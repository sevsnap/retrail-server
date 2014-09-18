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
import java.net.MalformedURLException;
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
            try {
                singleton = new UCon();
                singleton.init();
            } catch (IOException | XmlRpcException e) {
                log.error(e.getMessage());
            }
        }
        return singleton;
    }

    private UCon() throws UnknownHostException, XmlRpcException, IOException {
        // FIXME absolute paths should be settable
        super(new URL(defaultUrlString), XmlRpc.class);
        pdp[PdpEnum.PRE] = newPDP("/etc/contrail/contrail-authz-core/policies/pre/");
        pdp[PdpEnum.ON] = newPDP("/etc/contrail/contrail-authz-core/policies/on/");
        pdp[PdpEnum.POST] = newPDP("/etc/contrail/contrail-authz-core/policies/post/");
        dal = DAL.getInstance();
    }

    private PDP newPDP(String location) {
        PolicyFinder policyFinder = new PolicyFinder();
        Set<PolicyFinderModule> policyFinderModules = new HashSet<>();
        Set<String> locationSet = new HashSet<>();
        locationSet.add(location); //set the correct policy location
        FileBasedPolicyFinderModule fileBasedPolicyFinderModule = new FileBasedPolicyFinderModule(locationSet);
        policyFinderModules.add(fileBasedPolicyFinderModule);
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

    public PepSession tryAccess(PepAccessRequest accessRequest, URL pepUrl, String customId) throws MalformedURLException {
        // First enrich the request by calling the PIPs
        for (PIPInterface p : pip) {
            p.onTryAccess(accessRequest);
        }
        // Now send the enriched request to the PDP
        Document responseDocument = access(accessRequest, pdp[PdpEnum.PRE]);
        PepSession pepSession = new PepSession(responseDocument);
        if (pepSession.decision == PepAccessResponse.DecisionEnum.Permit) {
            UconSession session = dal.startSession(accessRequest, pepUrl, customId);
            pepSession.addSessionElement(session.getUuid(), session.getCustomId(), session.getStatus(), myUrl);
        } else {
            for (PIPInterface p : pip) {
                p.onEndAccess(accessRequest);
            }
        }
        return pepSession;
    }

    public Node assignCustomId(String uuid, String customId) {
        UconSession uconSession = dal.getSession(uuid);
        uconSession.setCustomId(customId);
        dal.save(uconSession);
        return null;
    }
    
    public PepSession startAccess(String uuid) throws MalformedURLException {
        // Now send the enriched request to the PDP
        UconSession uconSession = dal.getSession(uuid);
        if(uconSession == null)
            throw new RuntimeException("no session with uuid: "+uuid);
        if(uconSession.getStatus() != PepSession.Status.TRY)
            throw new RuntimeException(uconSession+" must be in TRY state to perform this operation");
        PepAccessRequest pepAccessRequest = rebuildPepAccessRequest(uconSession);
        refreshPepAccessRequest(pepAccessRequest);
        for(PIPInterface p: pip)
            p.onStartAccess(pepAccessRequest);
        Document responseDocument = access(pepAccessRequest, pdp[PdpEnum.ON]);
        PepSession pepSession = new PepSession(responseDocument);
        if (pepSession.decision == PepAccessResponse.DecisionEnum.Permit) {
            pepSession.setStatus(PepSession.Status.ONGOING);
            uconSession.setStatus(pepSession.getStatus());
            dal.updateSession(uconSession, pepAccessRequest);
        }
        return pepSession;
    }

    private Document revokeAccess(URL pepUrl, PepSession pepSession) throws XmlRpcException {
        // revoke session on db
        UconSession uconSession = dal.getSession(pepSession.getUuid());
        if(uconSession == null)
            throw new RuntimeException("cannot find session with uuid=" + pepSession.getUuid());
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

    private PepAccessRequest rebuildCleanPepAccessRequest(UconSession uconSession) {
        // Rebuild the PEP request with valid attributes only.
        // Puts expired ones in the expiredAttributes collection.
        log.debug("" + uconSession);
        Date now = new Date();
        PepAccessRequest accessRequest = new PepAccessRequest();
        for (Attribute a : uconSession.getAttributes()) {
            boolean isAttributeExpired = a.getExpires() != null && now.after(a.getExpires());
            if (!isAttributeExpired) {
                PepRequestAttribute pepAttribute = new PepRequestAttribute(a.getId(), a.getType(), a.getValue(), a.getIssuer(), a.getCategory());
                accessRequest.add(pepAttribute);
            }
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
                PepSession pepSession = new PepSession(PepAccessResponse.DecisionEnum.Permit, "Ongoing session");
                pepSession.addSessionElement(session.getUuid(), session.getCustomId(), session.getStatus(), myUrl);
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
            pepSession.addSessionElement(uuid, null, PepSession.Status.DELETED, myUrl);
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
                pepSession.addSessionElement(involvedSession.getUuid(), involvedSession.getCustomId(), involvedSession.getStatus(), myUrl);
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
            } catch (MalformedURLException | XmlRpcException ex) {
                log.error(ex.toString());
            }
        }
    }

    public void notifyChanges(Collection<PepRequestAttribute> changedAttributes) {
        // Gather all the sessions that involve the changed attributes
        log.info("{} attributes changed, updating db", changedAttributes.size());
        Collection<UconSession> involvedSessions = dal.updateAttributes(changedAttributes);
        reevaluateSessions(involvedSessions);
        log.debug("done (total sessions: {})", dal.listSessions().size());
    }

    public void notifyChanges(PepRequestAttribute changedAttribute) {
        Collection<PepRequestAttribute> attributes = new ArrayList(1);
        attributes.add(changedAttribute);
        notifyChanges(attributes);
    }

    public Node endAccess(String uuid) {
        UconSession session = dal.getSession(uuid);
        if (session != null) {
            PepAccessRequest request = rebuildPepAccessRequest(session);
            for (PIPInterface p : pip) {
                p.onEndAccess(request);
            }
            dal.endSession(session);
        } else {
            log.error("session {} is unknown, ignoring call", uuid);
        }
        return null;
    }

    public String getUuid(String uuid, String customId) {
        if(uuid != null)
            return uuid;
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
            log.warn("removing stale session " + expiredSession);
            endAccess(expiredSession.getUuid());
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
