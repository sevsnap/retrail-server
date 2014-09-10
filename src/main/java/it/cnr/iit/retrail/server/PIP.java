/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package it.cnr.iit.retrail.server;

import it.cnr.iit.retrail.commons.PepAccessRequest;
import it.cnr.iit.retrail.commons.PepRequestAttribute;
import java.util.Date;

/**
 *
 * @author kicco
 */
public class PIP {
    int count = 0;
    public void enrich(PepAccessRequest request) {
        System.out.println("PIP.enrich(): dummy PIP processor called, ignoring");
        if(count < 2) {
            PepRequestAttribute test = new PepRequestAttribute("reputation", "http://www.w3.org/2001/XMLSchema#string", "bronze", "http://localhost:8080/federation-id-prov/saml", "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject");
            count++;
            test.expires = new Date();
            request.add(test);
        }
    }
}
