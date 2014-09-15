/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package it.cnr.iit.retrail.server.test;

import it.cnr.iit.retrail.commons.PepAccessRequest;
import it.cnr.iit.retrail.commons.PepRequestAttribute;
import it.cnr.iit.retrail.server.PIP;
import java.util.Date;

/**
 *
 * @author kicco
 */
public class TestPIPbalance extends PIP {
    private int balance = 10;
    
    public TestPIPbalance(int balance) {
        super();
        this.balance = balance;
    }
    
    @Override
    public void enrich(PepAccessRequest request) {
        if(balance > 0) 
            balance--;
        PepRequestAttribute test = new PepRequestAttribute("balance", "http://www.w3.org/2001/XMLSchema#integer", Integer.toString(balance), "http://localhost:8080/federation-id-prov/saml", "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject");
        test.expires = new Date();
        test.factory = getUUID();
        request.add(test);
    }
    
}
