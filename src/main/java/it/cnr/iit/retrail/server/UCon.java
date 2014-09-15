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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.wso2.balana.PDP;
import org.wso2.balana.PDPConfig;
import org.wso2.balana.ParsingException;
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
    public List<PIP> pip = new ArrayList<>();
    public Map<String, PIP> pipNameToInstanceMap = new HashMap<>();
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
            } catch (IOException e) {

            } catch (XmlRpcException e) {
            }
        }
        return singleton;
    }

    private UCon() throws UnknownHostException, XmlRpcException, IOException {
        // FIXME absolute paths should be settable
        super(new URL(defaultUrlString), APIImpl.class);
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
        } catch (ParsingException ex) {
            log.error("*** STICAZZI");
            Logger.getLogger(UCon.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            log.error("*** STICAZZI GRAVE");
            Logger.getLogger(UCon.class.getName()).log(Level.SEVERE, null, ex);
        }
        return accessResponse;

    }

    public PepSession tryAccess(PepAccessRequest accessRequest, URL pepUrl) {
        // First enrich the request by calling the PIPs
        enrichPepAccessRequest(accessRequest);
        // Now send the enriched request to the PDP
        Document responseDocument = access(accessRequest, pdp[PdpEnum.PRE]);
        PepSession pepSession = new PepSession(responseDocument);
        if (pepSession.decision == PepAccessResponse.DecisionEnum.Permit) {
            UconSession session = dal.startSession(accessRequest, pepUrl);
            pepSession.addSession(session.getId().toString(), session.getCookie(), session.getStatus());
        }
        return pepSession;
    }

    public PepSession startAccess(Long sessionId) {
        // Now send the enriched request to the PDP
        UconSession uconSession = dal.getSession(sessionId);
        PepAccessRequest pepAccessRequest = rebuildPepAccessRequest(uconSession);
        enrichPepAccessRequest(pepAccessRequest);
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
        dal.revokeSession(Long.parseLong(pepSession.getId()));
        // create client
        log.warn("invoking PEP at " + pepUrl + " to revoke " + pepSession);
        Client client = new Client(pepUrl);
        // remote call. TODO: should consider error handling
        Object[] params = new Object[]{pepSession.toElement(), myUrl.toString()};
        Document doc = (Document) client.execute("PEP.revokeAccess", params);
        return doc;
    }

    private PepAccessRequest rebuildPepAccessRequest(UconSession uconSession) {
        // Rebuild the PEP request with valid attributes only.
        // Puts expired ones in the expiredAttributes collection.
        log.info("" + uconSession);
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

    private void enrichPepAccessRequest(PepAccessRequest accessRequest) {
        // TODO: should call only pips for changed attributes
        log.info("enriching request");
        for (PIP p : pip) {
            p.enrich(accessRequest);
        }
    }
    
    private PepSession evaluateRequest(PepAccessRequest accessRequest) throws MalformedURLException, XmlRpcException {
        PepSession pepSession = null;
        // enrich the request the re-evaluate.
        enrichPepAccessRequest(accessRequest);
        // Now make PDP evaluate the request
        log.info("evaluating request");
        Document responseDocument = access(accessRequest, pdp[PdpEnum.ON]);
        pepSession = new PepSession(responseDocument);
        log.info("done, " + pepSession);
        return pepSession;
    }

    public Document heartbeat(URL pepUrl, List<String> sessionsList) throws Exception {
        Collection<UconSession> sessions = dal.listSessions(pepUrl);
        Document doc = DomUtils.newDocument();
        Element responses = doc.createElement("Responses");
        doc.appendChild(responses);
        Date now = new Date();
        // Permit sessions unknown to the client.
        for (UconSession session : sessions) {
            boolean isNotKnownByClient = !sessionsList.remove(session.getId().toString());
            if (isNotKnownByClient) {
                PepAccessRequest pepAccessRequest = rebuildPepAccessRequest(session);
                // Reevaluate request for safety
                //PepSession pepSession = evaluateRequest(pepAccessRequest);
                PepSession pepSession = new PepSession(PepAccessResponse.DecisionEnum.Permit, "Ongoing session");
                pepSession.addSession(session.getId().toString(), session.getCookie(), session.getStatus());
                log.warn("found session unknown to client: "+pepSession);
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
        for (String id : sessionsList) {
            PepSession pepSession = new PepSession(PepAccessResponse.DecisionEnum.NotApplicable, "Unexistent session");
            pepSession.addSession(id, null, PepSession.Status.REVOKED);
            Node n = doc.adoptNode(pepSession.toElement());
            responses.appendChild(n);
        }
        return doc;
    }

    public Node endAccess(String sessionId) {
        Long id = Long.parseLong(sessionId);
        dal.endSession(id);
        return null;
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
            dal.endSession(expiredSession.getId());
        }
        // Gather all the sessions that involve expired attributes
        Collection<Attribute> expiredAttributes = dal.listAttributes(now);
        Collection<UconSession> involvedSessions = new HashSet<>();
        for (Attribute expiredAttribute : expiredAttributes) {
            log.info("changed attribute: {}", expiredAttribute);
            involvedSessions.addAll(expiredAttribute.getSessions());
        }
        // Re-evaluating possible changed sessions
        for (UconSession involvedSession : involvedSessions) {
            try {
                log.warn("involved session: {}", involvedSession);
                // Rebuild PEP request without expired attributes 
                PepAccessRequest pepAccessRequest = rebuildPepAccessRequest(involvedSession);
                PepSession pepSession = evaluateRequest(pepAccessRequest);
                pepSession.addSession(involvedSession.getId().toString(), involvedSession.getCookie(), involvedSession.getStatus());
                boolean mustRevoke = pepSession.decision != PepAccessResponse.DecisionEnum.Permit;
                // Explicitly revoke access if anything went wrong
                if (mustRevoke) {
                    log.warn("revoking {}", pepSession);
                    URL pepUrl = new URL(involvedSession.getPepUrl());
                    revokeAccess(pepUrl, pepSession);
                } else {
                    log.warn("updating {}", pepSession);
                    dal.updateSession(involvedSession, pepAccessRequest);
                }
            } catch (MalformedURLException | XmlRpcException ex) {
                log.error(ex.toString());
            }

        }
        log.info("OK (sessions: {})", dal.listSessions().size());
    }

    public synchronized void addPIP(PIP p) {
        pip.add(p);
        pipNameToInstanceMap.put(p.getClass().getCanonicalName(), p);
    }

    public synchronized PIP removePIP(PIP p) {
        pipNameToInstanceMap.remove(p.getClass().getCanonicalName());
        return pip.remove(p) ? p : null;
    }

}
