/*
 *  KMcC;) Balana policy finder module able to read files from a URL.
 *  Coded by: Enrico KMcC;) Carniani for iit.cnr.it.
 */

package org.wso2.balana.finder.impl;

import org.wso2.balana.finder.PolicyFinder;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is file based policy repository.  Policies can be inside the directory in a file system.
 * Then you can set directory location using "org.wso2.balana.PolicyDirectory" JAVA property   
 */
public class StreamBasedPolicyFinderModule extends AbstractStreamPolicyFinderModule {
    private final InputStream policyStream;

    public StreamBasedPolicyFinderModule(InputStream stream) {
        super();
        log = LoggerFactory.getLogger(StreamBasedPolicyFinderModule.class);
        this.policyStream = stream;
    }
    
    @Override
    public void loadPolicies() {
        loadPolicy(policyStream);
    }
}
