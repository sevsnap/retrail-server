package it.cnr.iit.retrail.server;

import it.cnr.iit.retrail.commons.DomUtils;
import it.cnr.iit.retrail.commons.PepAccessRequest;
import it.cnr.iit.retrail.commons.PepAccessResponse;
import it.cnr.iit.retrail.commons.PepSession;
import it.cnr.iit.retrail.commons.Server;
import it.cnr.iit.retrail.server.db.DAL;
import it.cnr.iit.retrail.server.db.UconSession;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
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
import org.w3c.dom.Document;


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
    private final DAL dal;
    /**
     *
     * @return
     */
    public static UCon getInstance() {
        if (singleton == null) {
            try {
                singleton = new UCon();
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
        super(new URL(defaultUrlString), API.class);
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
            DomUtils.write(xacmlRequest);
            AbstractRequestCtx request = RequestCtxFactory.getFactory().getRequestCtx(xacmlRequest);
            ResponseCtx response = p.evaluate(request);
            String responseString = response.encode();
            System.out.println("*** RESPONSE: " + responseString);
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
        for (PIP p : pip) {
            p.process(accessRequest);
        }
        // Now send the enriched request to the PDP
        Document responseDocument = access(accessRequest, pdp[PdpEnum.PRE]);
        PepAccessResponse response = new PepAccessResponse(responseDocument);
        return response;
    }

    public PepSession startAccess(PepAccessRequest accessRequest) {
        System.out.println("*** STARTACCESS: ");
        // First enrich the request by calling the PIPs
        for (PIP p : pip) {
            p.process(accessRequest);
        }
        // Now send the enriched request to the PDP
        Document responseDocument = access(accessRequest, pdp[PdpEnum.ON]);
        PepSession pepSession = new PepSession(responseDocument);
        if(true || pepSession.decision == PepAccessResponse.DecisionEnum.Permit) {
            UconSession session = dal.startSession(accessRequest);
            pepSession.addSessionInfo(session.getId().toString(), session.getCookie());
        }
        return pepSession;
    }

    public void endAccess(String sessionId) {
        dal.endSession(Long.parseLong(sessionId));
    }
}
