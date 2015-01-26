/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.impl;

import it.cnr.iit.retrail.commons.DomUtils;
import it.cnr.iit.retrail.commons.impl.PepRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import org.eclipse.persistence.internal.helper.IdentityHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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

/**
 *
 * @author oneadmin
 */
public final class PDPPool {
    protected static final Logger log = LoggerFactory.getLogger(PDPPool.class);
    protected static final int maxPoolSize = 10;
    final private URL policyURL;
    final private String policyString;
    private final IdentityHashSet busy;
    private final LinkedList<PDP> available;

    public PDPPool(URL policyURL) {
        log.debug("setting policy to {}", policyURL);
        this.policyURL = policyURL;
        busy = new IdentityHashSet(maxPoolSize);
        available = new LinkedList();
        policyString = null;
        // Allocate one PDP at least, in order to perform policy syntax checking on start.
        returnPDP(obtainPDP());
    }
    
    public PDPPool(InputStream stream) throws IOException {
        log.debug("reading policy from stream");
        policyString = new Scanner(stream).useDelimiter("\\A").next();
        policyURL = null;
        busy = new IdentityHashSet(maxPoolSize);
        available = new LinkedList();
        // Allocate one PDP at least, in order to perform policy syntax checking on start.
        returnPDP(obtainPDP());
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
        locationSet.add(location); //set the correct policy policyURL
        URLBasedPolicyFinderModule URLBasedPolicyFinderModule = new URLBasedPolicyFinderModule(locationSet);
        return newPDP(URLBasedPolicyFinderModule);
    }
    
    private PDP newPDP(InputStream stream) {
        StreamBasedPolicyFinderModule streamBasedPolicyFinderModule = new StreamBasedPolicyFinderModule(stream);
        return newPDP(streamBasedPolicyFinderModule);
    }
    
    private synchronized PDP obtainPDP() {
        PDP pdp = null;
        if(available.isEmpty()) {
            if(policyURL == null) {
               InputStream stream = new ByteArrayInputStream(policyString.getBytes());
               pdp = newPDP(stream);
            } else pdp = newPDP(policyURL);
        } else pdp = available.removeFirst();
        busy.add(pdp);
        if(busy.size() > maxPoolSize)
            log.warn("running PDPs > {}, consider enlarging PDPPool.maxPoolSize value (policy URL: {})", maxPoolSize, policyURL);
        return pdp;
    }
    
    private synchronized void returnPDP(PDP pdp) {
        if(busy.remove(pdp) && available.size() < maxPoolSize)
            available.add(pdp);
    }
    
    private Document access(PepRequest accessRequest, PDP p) {
        Document accessResponse = null;
        long start = System.nanoTime();
        try {
            Element xacmlRequest = accessRequest.toElement();
            AbstractRequestCtx request = RequestCtxFactory.getFactory().getRequestCtx(xacmlRequest);
            ResponseCtx response = p.evaluate(request);
            String responseString = response.encode();
            accessResponse = DomUtils.read(responseString);
            DecimalFormat df = new DecimalFormat("#.###");
            String ms = df.format((System.nanoTime() - start) / 1.0e+6);
            accessResponse.getDocumentElement().setAttribute("ms", ms);
            //log.info("ACCESS UCON {}", DomUtils.toString(accessResponse));
        } catch (Exception ex) {
            log.error("Unexpected exception {}: {}", ex, ex.getMessage());
        }
        return accessResponse;
    }

    public Document access(PepRequest accessRequest) {
        PDP pdp = obtainPDP();
        Document doc = null;
        try {
            doc = access(accessRequest, pdp);
        } finally {
            returnPDP(pdp);
        }
        return doc;
    }

    
}
