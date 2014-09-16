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
// Expiring attribute handler
public class TestPIPSessions extends PIP {

    private int sessions = 0;
    final private int maxSessions;
    
    public TestPIPSessions(int maxSessions) {
        super();
        this.maxSessions = maxSessions;
    }

    @Override
    public void open(PepAccessRequest request) {
        sessions++;
        log.info("Number of open sessions: " + sessions);
        PepRequestAttribute test = newAttribute("openSessions", "http://www.w3.org/2001/XMLSchema#integer", Integer.toString(sessions), "http://localhost:8080/federation-id-prov/saml", "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject");
        request.add(test);
    }

    @Override
    public void close(PepAccessRequest request) {
        sessions--;
        log.info("Number of open sessions: " + sessions);
    }
}
