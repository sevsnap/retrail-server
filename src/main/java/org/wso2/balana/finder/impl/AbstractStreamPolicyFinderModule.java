/*
 *  KMcC;) Balana policy finder module able to read files from a URL.
 *  Coded by: Enrico KMcC;) Carniani for iit.cnr.it.
 */
package org.wso2.balana.finder.impl;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wso2.balana.*;
import org.wso2.balana.combine.PolicyCombiningAlgorithm;
import org.wso2.balana.ctx.EvaluationCtx;
import org.wso2.balana.ctx.Status;
import org.wso2.balana.finder.PolicyFinder;
import org.wso2.balana.finder.PolicyFinderResult;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import javax.xml.parsers.ParserConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.balana.combine.xacml2.DenyOverridesPolicyAlg;
import org.wso2.balana.finder.PolicyFinderModule;
import org.xml.sax.SAXException;

/**
 * This is file based policy repository. Policies can be inside the directory in
 * a file system. Then you can set directory location using
 * "org.wso2.balana.PolicyDirectory" JAVA property
 */
public abstract class AbstractStreamPolicyFinderModule extends PolicyFinderModule {

    protected PolicyFinder finder = null;
    protected Map<URI, AbstractPolicy> policies;
    protected PolicyCombiningAlgorithm combiningAlg;

    /**
     * the logger we'll use for all messages
     */
    protected static Logger log = LoggerFactory.getLogger(AbstractStreamPolicyFinderModule.class);

    public AbstractStreamPolicyFinderModule() {
        policies = new HashMap<>();
    }

    @Override
    public void init(PolicyFinder finder) {
        this.finder = finder;
        loadPolicies();
        combiningAlg = new DenyOverridesPolicyAlg();
    }

    public abstract void loadPolicies();

    @Override
    public PolicyFinderResult findPolicy(EvaluationCtx context) {

        ArrayList<AbstractPolicy> selectedPolicies = new ArrayList<>();
        Set<Map.Entry<URI, AbstractPolicy>> entrySet = policies.entrySet();

        // iterate through all the policies we currently have loaded
        for (Map.Entry<URI, AbstractPolicy> entry : entrySet) {

            AbstractPolicy policy = entry.getValue();
            MatchResult match = policy.match(context);
            int result = match.getResult();

            // if target matching was indeterminate, then return the error
            if (result == MatchResult.INDETERMINATE) {
                return new PolicyFinderResult(match.getStatus());
            }

            // see if the target matched
            if (result == MatchResult.MATCH) {

                if ((combiningAlg == null) && (selectedPolicies.size() > 0)) {
                    // we found a match before, so this is an error
                    ArrayList<String> code = new ArrayList<>();
                    code.add(Status.STATUS_PROCESSING_ERROR);
                    Status status = new Status(code, "too many applicable "
                            + "top-level policies");
                    return new PolicyFinderResult(status);
                }

                // this is the first match we've found, so remember it
                selectedPolicies.add(policy);
            }
        }

        // no errors happened during the search, so now take the right
        // action based on how many policies we found
        switch (selectedPolicies.size()) {
            case 0:
                if (log.isDebugEnabled()) {
                    log.debug("No matching XACML policy found");
                }
                return new PolicyFinderResult();
            case 1:
                return new PolicyFinderResult((selectedPolicies.get(0)));
            default:
                return new PolicyFinderResult(new PolicySet(null, combiningAlg, null, selectedPolicies));
        }
    }

    @Override
    public PolicyFinderResult findPolicy(URI idReference, int type, VersionConstraints constraints,
            PolicyMetaData parentMetaData) {

        AbstractPolicy policy = policies.get(idReference);
        if (policy != null) {
            if (type == PolicyReference.POLICY_REFERENCE) {
                if (policy instanceof Policy) {
                    return new PolicyFinderResult(policy);
                }
            } else {
                if (policy instanceof PolicySet) {
                    return new PolicyFinderResult(policy);
                }
            }
        }

        // if there was an error loading the policy, return the error
        ArrayList<String> code = new ArrayList<>();
        code.add(Status.STATUS_PROCESSING_ERROR);
        Status status = new Status(code,
                "couldn't load referenced policy");
        return new PolicyFinderResult(status);
    }

    @Override
    public boolean isIdReferenceSupported() {
        return true;
    }

    @Override
    public boolean isRequestSupported() {
        return true;
    }

    /**
     * Private helper that tries to load the given file-based policy, and
     * returns null if any error occurs.
     *
     * @param stream
     * @return  <code>AbstractPolicy</code>
     */
    protected AbstractPolicy loadPolicy(InputStream stream) {
        AbstractPolicy policy = null;

        try {
            // create the factory
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringComments(true);
            factory.setNamespaceAware(true);
            factory.setValidating(false);

            // create a builder based on the factory & try to load the policy
            DocumentBuilder db = factory.newDocumentBuilder();
            Document doc = db.parse(stream);

            // handle the policy, if it's a known type
            Element root = doc.getDocumentElement();
            String name = DOMHelper.getLocalName(root);

            switch (name) {
                case "Policy":
                    policy = Policy.getInstance(root);
                    break;
                case "PolicySet":
                    policy = PolicySet.getInstance(root, finder);
                    break;
            }
        } catch (ParserConfigurationException | IOException | SAXException | ParsingException e) {
            // just only logs
            log.error("Failed to load policy : {}" + e.getMessage());
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    log.error("Error while closing input stream");
                }
            }
        }

        if (policy != null) {
            policies.put(policy.getId(), policy);
        }

        return policy;
    }

}
