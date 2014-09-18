/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and onTryAccess the template in the editor.
 */

package it.cnr.iit.retrail.server.test;

import it.cnr.iit.retrail.commons.PepAccessRequest;
import it.cnr.iit.retrail.commons.PepRequestAttribute;
import it.cnr.iit.retrail.server.pip.PIP;
import java.util.Date;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kicco
 */

public class TestPIPReputation extends PIP {
    private String reputation = "bronze";
    
    public TestPIPReputation(String reputation) {
        super();
        this.log = LoggerFactory.getLogger(TestPIPReputation.class);
        this.reputation = reputation;
    }
    
    @Override
    public void onTryAccess(PepAccessRequest request) {
        PepRequestAttribute test = newAttribute("reputation", "http://www.w3.org/2001/XMLSchema#string", reputation, "http://localhost:8080/federation-id-prov/saml", "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject");
        test.expires = new Date();
        request.add(test);
    }
    
    @Override
    protected void refresh(PepRequestAttribute pepAttribute) {
        pepAttribute.expires = new Date();
    }
}
