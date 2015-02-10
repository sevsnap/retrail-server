/*
 *  KMcC;) Balana policy finder module able to read files from a URL.
 *  Coded by: Enrico KMcC;) Carniani for iit.cnr.it.
 */
package org.wso2.balana.finder.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import org.slf4j.LoggerFactory;

/**
 * This is file based policy repository. Policies can be inside the directory in
 * a file system. Then you can set directory location using
 * "org.wso2.balana.PolicyDirectory" JAVA property
 */
public class URLBasedPolicyFinderModule extends AbstractStreamPolicyFinderModule {

    private Set<URL> policyLocations;
    public static final String POLICY_URL_PROPERTY = "org.wso2.balana.PolicyUrlDirectory";

    public URLBasedPolicyFinderModule() throws MalformedURLException {
        super();
        log = LoggerFactory.getLogger(URLBasedPolicyFinderModule.class);
        policies = new LinkedHashMap<>();
        if (System.getProperty(POLICY_URL_PROPERTY) != null) {
            policyLocations = new HashSet<>();
            policyLocations.add(new URL(System.getProperty(POLICY_URL_PROPERTY)));
        }
    }

    public URLBasedPolicyFinderModule(Set<URL> policyLocations) {
        super();
        log = LoggerFactory.getLogger(URLBasedPolicyFinderModule.class);
        this.policyLocations = policyLocations;
    }

    /**
     * Re-sets the policies known to this module to those contained in the given
     * files.
     *
     */
    @Override
    public void loadPolicies() {
        policies.clear();
        for (URL policyLocation : policyLocations) {
            try {
                log.debug("Reading policy location: {}", policyLocation);
                URI policyUri = policyLocation.toURI();
                File file = new File(policyUri);
                if (!file.exists()) {
                    log.error("URL {} does not exist", policyLocation);
                    continue;
                }
                if (file.isDirectory()) {
                    String[] files = file.list();
                    for (String policyFile : files) {
                        URI uri = new URI(policyLocation + File.separator + policyFile);
                        File fileLocation = new File(uri);
                        if (!fileLocation.isDirectory()) {
                            InputStream stream = uri.toURL().openStream();
                            loadPolicy(stream);
                        }
                    }
                } else {
                    InputStream stream = policyUri.toURL().openStream();
                    loadPolicy(stream);
                }
            } catch (URISyntaxException ex) {
                log.error("{}: {}", policyLocation, ex.getMessage());
            } catch (IOException ex) {
                log.error("{}: {}", policyLocation, ex.getMessage());
            }
        }
    }
}
