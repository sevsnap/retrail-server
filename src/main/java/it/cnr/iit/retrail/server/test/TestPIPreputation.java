/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package it.cnr.iit.retrail.server.test;

import it.cnr.iit.retrail.commons.PepAccessRequest;
import it.cnr.iit.retrail.commons.PepRequestAttribute;
import it.cnr.iit.retrail.server.PIP;

/**
 *
 * @author kicco
 */
public class TestPIPreputation extends PIP {
    private String reputation = "bronze";
    
    public TestPIPreputation(String reputation) {
        super();
        this.reputation = reputation;
    }
    

    @Override
    public void enrich(PepAccessRequest request) {
        PepRequestAttribute test = new PepRequestAttribute("reputation", "http://www.w3.org/2001/XMLSchema#string", reputation, "http://localhost:8080/federation-id-prov/saml", "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject");
        request.add(test);
    }
    
}
