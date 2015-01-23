/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import org.eclipse.persistence.internal.helper.IdentityHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.balana.PDP;
import org.wso2.balana.PDPConfig;
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
    
    public synchronized PDP obtainPDP() {
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
    
    public synchronized void returnPDP(PDP pdp) {
        if(busy.remove(pdp) && available.size() < maxPoolSize)
            available.add(pdp);
    }
    
}
