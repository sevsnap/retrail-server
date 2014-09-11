package it.cnr.iit.retrail.server;

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
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.w3c.dom.Element;
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

import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class UCon extends Server implements Runnable {
    private static final int heartbeatPeriod = 15;
    private static final String defaultUrlString = "http://localhost:8080";
    private static UCon singleton;

    private static class PdpEnum {

        static final int PRE = 0;
        static final int ON = 1;
        static final int POST = 2;
    }

    private final PDP pdp[] = new PDP[3];
    public List<PIP> pip = new ArrayList<>();
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

    public static void main(String[] args) throws Exception {
        UCon ucon = getInstance();
        ucon.pip.add(new PIP());
    }

    private UCon() throws UnknownHostException, XmlRpcException, IOException {
        // FIXME absolute paths should be settable
        super(new URL(defaultUrlString), APIImpl.class);
        pdp[PdpEnum.PRE] = newPDP("/etc/contrail/contrail-authz-core/policies/pre/");
        pdp[PdpEnum.ON] = newPDP("/etc/contrail/contrail-authz-core/policies/on/");
        pdp[PdpEnum.POST] = newPDP("/etc/contrail/contrail-authz-core/policies/on/");   //FIXME
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
            System.out.println("*** STICAZZI");
            Logger.getLogger(UCon.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            System.out.println("*** STICAZZI GRAVE");
            Logger.getLogger(UCon.class.getName()).log(Level.SEVERE, null, ex);
        }
        return accessResponse;

    }

    public PepAccessResponse tryAccess(PepAccessRequest accessRequest) {
        System.out.println("*** TRYACCESS: ");
        // First enrich the request by calling the PIPs
        for (PIP p : pip) 
            p.enrich(accessRequest);
        // Now send the enriched request to the PDP
        Document responseDocument = access(accessRequest, pdp[PdpEnum.PRE]);
        PepAccessResponse response = new PepAccessResponse(responseDocument);
        return response;
    }

    public PepSession startAccess(PepAccessRequest accessRequest, URL pepUrl) {
        System.out.println("*** PepSession.startAccess(): ");
        // First enrich the request by calling the PIPs
        for (PIP p : pip) 
            p.enrich(accessRequest);
        System.out.println("*** PepSession.startAccess(): PIP done");
        // Now send the enriched request to the PDP
        Document responseDocument = access(accessRequest, pdp[PdpEnum.ON]);
        PepSession pepSession = new PepSession(responseDocument);
        if (pepSession.decision == PepAccessResponse.DecisionEnum.Permit) {
            UconSession session = dal.startSession(accessRequest, pepUrl);
            pepSession.addSession(session.getId().toString(), session.getCookie());
        }
        return pepSession;
    }

    private Document revokeAccess(URL pepUrl, PepSession pepSession) throws XmlRpcException {
        // create configuration
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(pepUrl);
        config.setEnabledForExtensions(true);
        config.setConnectionTimeout(60 * 1000);
        config.setReplyTimeout(60 * 1000);

        XmlRpcClient client = new XmlRpcClient();
        // use Commons HttpClient as transport
        client.setTransportFactory(
                new XmlRpcCommonsTransportFactory(client));
        // set configuration
        client.setConfig(config);
        Object[] params = new Object[]{pepSession.toElement(), myUrl.toString()};
        Document doc = (Document) client.execute("PEP.revokeAccess", params);
        return doc;
    }
    
    private PepAccessRequest buildPurifiedPepAccessRequest(UconSession uconSession, Collection<Attribute> expiredAttributes) {
        // Rebuild the PEP request with valid attributes only
        System.out.println("*** UCON.rebuildAndPurifyPepAccessRequest(): "+uconSession);
        expiredAttributes.clear();
        Date now = new Date();
        PepAccessRequest accessRequest = new PepAccessRequest();
        for (Attribute a : uconSession.getAttributes()) {
            boolean isAttributeExpired = a.getExpires() != null && now.after(a.getExpires());
            if(!isAttributeExpired) {           
                PepRequestAttribute pepAttribute = new PepRequestAttribute(a.getId(), a.getType(), a.getValue(), a.getIssuer(), a.getCategory());
                accessRequest.add(pepAttribute);
            } else
                expiredAttributes.add(a);
        }
        return accessRequest;
    }
    
    private PepSession checkSession(UconSession uconSession, boolean forceEvaluation) throws MalformedURLException, XmlRpcException {
        // may return null if no evaluation is done (equivalent to Permit)
        // Rebuild the PEP request with valid attributes only
        System.out.println("*** UCON.checkSession(): "+uconSession);
        Collection<Attribute> expiredAttributes = new HashSet<>();
        PepAccessRequest accessRequest = buildPurifiedPepAccessRequest(uconSession, expiredAttributes);
        PepSession pepSession = null;
        // If some attribute expired, re-enrich the request the re-evaluate.
        if(expiredAttributes.size() > 0 || forceEvaluation)  {
            System.out.println("*** UCON.checkSession(): needs reevaluation -- enriching request");
            for (PIP p : pip) 
                p.enrich(accessRequest);
            // Now make PDP evaluate the request
            System.out.println("*** UCON.checkSession(): reevaluating request");
            Document responseDocument = access(accessRequest, pdp[PdpEnum.POST]);
            pepSession = new PepSession(responseDocument);
            pepSession.addSession(uconSession.getId().toString(), uconSession.getCookie());
            System.out.println("*** UCON.checkSession(): done, "+pepSession);
        } 
        return pepSession;
    }

    public void updateAccess(String sessionId) throws MalformedURLException, XmlRpcException {
        Long id = Long.parseLong(sessionId);
        UconSession uconSession = dal.getSession(id);
        PepSession pepSession = checkSession(uconSession, false);
        // Explicitly revoke access if anything went wrong
        if (pepSession != null && pepSession.decision != PepAccessResponse.DecisionEnum.Permit) {
            URL pepUrl = new URL(uconSession.getPepUrl());
            revokeAccess(pepUrl, pepSession);
            dal.endSession(id);
        }
    }

    public Document heartbeat(URL pepUrl, List<String> sessionsList) throws ParserConfigurationException, MalformedURLException, XmlRpcException, SAXException, IOException {
        Collection<UconSession> sessions = dal.listSessions(pepUrl);
        // Revoke access to any sessions opened by the registered url if no more allowed.
        Document doc = DomUtils.newDocument();
        Element responses = doc.createElement("Responses");
        doc.appendChild(responses);
        Date now = new Date();
        for(UconSession session: sessions) {
            boolean isNotKnownByClient = !sessionsList.remove(session.getId().toString());
            
            PepSession pepSession = checkSession(session, isNotKnownByClient);
            boolean mustRemove = pepSession != null && pepSession.decision != PepAccessResponse.DecisionEnum.Permit;
            if (isNotKnownByClient || mustRemove) {
                Node n = doc.adoptNode(pepSession.toElement());
                responses.appendChild(n);
            } 
            if(mustRemove)
                dal.endSession(session.getId());
            else  {
                // "Touch" session
                session.setLastSeen(now);
                dal.save(session);
            }
        }
        // Process sessions known by the client, but unknown to the server.
        for(String id: sessionsList) {
            PepSession pepSession = new PepSession(PepAccessResponse.DecisionEnum.NotApplicable, "Unexistent session");
            pepSession.addSession(id, null);
            Node n = doc.adoptNode(pepSession.toElement());
            responses.appendChild(n);
        }
        return doc;
    }
    
    public void endAccess(String sessionId) {
        Long id = Long.parseLong(sessionId);
        dal.endSession(id);
    }
    
    public void init() {        // start watchdog
        (new Thread(this)).start();
    }

    private void watchdog() {
        // List all sessions that were not heartbeaten since at least 2 periods
        Date lastSeenBefore = new Date(new Date().getTime() - 1000*2*heartbeatPeriod);
        Collection<UconSession> sessions = dal.listSessions(lastSeenBefore);
        // Remove them
        for(UconSession session: sessions) {
            System.out.println("UCon.watchdog(): removing stale session "+session);
            dal.endSession(session.getId());
        }
        System.out.println("UCon.watchdog(): OK");
    }
    
    @Override
    public void run() {
        // Watchdog
        while (true) {
            try {
                watchdog();
                Thread.sleep(heartbeatPeriod * 1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(UCon.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
        }
    }

}
