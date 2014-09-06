package it.cnr.iit.retrail.server;

import it.cnr.iit.retrail.commons.DomUtils;
import it.cnr.iit.retrail.commons.PepAccessRequest;
import it.cnr.iit.retrail.commons.Server;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.xmlrpc.XmlRpcException;
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
        getInstance().pip.add(new PIP());
    }

    public UCon() throws UnknownHostException, XmlRpcException, IOException {
        // FIXME absolute paths should be settable
        super(new URL(defaultUrlString), API.class);
        pdp[PdpEnum.PRE] = newPDP("/etc/contrail/contrail-authz-core/policies/pre/");
        pdp[PdpEnum.ON] = newPDP("/etc/contrail/contrail-authz-core/policies/on/");
        pdp[PdpEnum.POST] = newPDP("/etc/contrail/contrail-authz-core/policies/post/");
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
    
    public Node tryAccess(PepAccessRequest accessRequest) {
        System.out.println("*** TRYACCESS: ");
        Node result = null;
        // First enrich the request by calling the PIPs
        for(PIP p: pip)
            p.process(accessRequest);
        // Now send the enriched request to the PDP
        try {
            Element xacmlRequest = accessRequest.toElement();
            DomUtils.write(xacmlRequest);
            AbstractRequestCtx request = RequestCtxFactory.getFactory().getRequestCtx(xacmlRequest);
            ResponseCtx response = pdp[PdpEnum.PRE].evaluate(request);
            result = DomUtils.read(response.encode());
            System.out.println("*** RESPONSE: " + response.encode());
        } catch (ParsingException ex) {
            System.out.println("*** STICAZZI");
            Logger.getLogger(UCon.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            System.out.println("*** STICAZZI GRAVE");
            Logger.getLogger(UCon.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    public Node startAccess(PepAccessRequest request) {
        return null;
    }

    public Node endAccess(PepAccessRequest request) {
        return null;
    }
}
