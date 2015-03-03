/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.behaviour;

import it.cnr.iit.retrail.commons.DomUtils;
import it.cnr.iit.retrail.commons.Pool;
import it.cnr.iit.retrail.commons.impl.PepRequest;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wso2.balana.PDP;
import org.wso2.balana.PDPConfig;
import org.wso2.balana.XACMLConstants;
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

/**
 *
 * @author oneadmin
 */
public final class PDPPool extends Pool<PDP> {
    final private String policyString;
    final private String policyId;

    public PDPPool(Element policyElement) {
        super();
        this.policyId = policyElement.getAttribute("PolicyId");
        this.policyString = DomUtils.toString(policyElement);
        log.info("setting policy {}", policyId);
        // Allocate one PDP at least, in order to perform policy syntax checking on start.
        release(obtain());
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
/*
    private PDP newPDP(URL location) {
        Set<URL> locationSet = new HashSet<>();
        locationSet.add(location); //set the correct policy policyURL
        URLBasedPolicyFinderModule URLBasedPolicyFinderModule = new URLBasedPolicyFinderModule(locationSet);
        return newPDP(URLBasedPolicyFinderModule);
    }
*/
    private PDP newPDP(InputStream stream) {
        StreamBasedPolicyFinderModule streamBasedPolicyFinderModule = new StreamBasedPolicyFinderModule(stream);
        return newPDP(streamBasedPolicyFinderModule);
    }

    @Override
    protected PDP newObject() {
        PDP pdp = null;
        InputStream stream = new ByteArrayInputStream(policyString.getBytes());
        pdp = newPDP(stream);
        return pdp;
    }

    private Document access(PepRequest accessRequest, PDP p) {
        Document accessResponse = null;
        long start = System.nanoTime();
        try {
            Element xacmlRequest = accessRequest.toElement();
            log.debug("xacml request {}", DomUtils.toString(xacmlRequest));
            AbstractRequestCtx request = RequestCtxFactory.getFactory().getRequestCtx(xacmlRequest);
            ResponseCtx response = p.evaluate(request);
            String responseString = response.encode();
            accessResponse = DomUtils.read(responseString);
            DecimalFormat df = new DecimalFormat("#.###");
            String ms = df.format((System.nanoTime() - start) / 1.0e+6);
            accessResponse.getDocumentElement().setAttribute("xmlns", XACMLConstants.XACML_3_0_IDENTIFIER);
            accessResponse.getDocumentElement().setAttribute("ms", ms);
            log.debug("xacml response {}", DomUtils.toString(accessResponse));
        } catch (Exception ex) {
            log.error("while querying balana: {}", ex);
        }
        return accessResponse;
    }

    public Document access(PepRequest accessRequest) {
        PDP pdp = obtain();
        Document doc = null;
        try {
            doc = access(accessRequest, pdp);
        } finally {
            release(pdp);
        }
        return doc;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+" [PolicyId="+policyId+"]";
    }
}
