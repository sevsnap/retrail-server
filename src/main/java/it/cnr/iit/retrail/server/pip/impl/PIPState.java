/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.pip.impl;

import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.commons.PepRequestInterface;
import it.cnr.iit.retrail.server.dal.UconSession;
import it.cnr.iit.retrail.server.pip.Event;

/**
 *
 * @author kicco
 */
public class PIPState extends PIP {
    private String stateAttributeName = "sessionState";
    private String stateAttributeValue = "init";
    final public String category = "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject";

    @Override
    public void fireEvent(Event e) {
        switch(e.type) {
            case beforeTryAccess:
                log.info("State attribute name: " + stateAttributeName);
                PepAttributeInterface state = newSharedAttribute(stateAttributeName, "http://www.w3.org/2001/XMLSchema#string", stateAttributeValue, "http://localhost:8080/federation-id-prov/saml", category);
                e.request.replace(state);
                break;
            case afterEndAccess:
            case afterRevokeAccess:
            case afterRunObligations:
            case afterStartAccess:
            case afterTryAccess:
                for(String obligation: e.session.getObligations()) {
                    log.warn("FOUND OBLIGATION: {}", obligation);
                    if(obligation.startsWith(stateAttributeName+"=")) {
                        String newStateAttributeValue = obligation.split("=")[1];
                        log.warn("FOUND OBLIGATION FOR STATE CHANGE: {}", newStateAttributeValue);
                        PepAttributeInterface updatedState = newSharedAttribute(stateAttributeName, "http://www.w3.org/2001/XMLSchema#string", newStateAttributeValue, "http://localhost:8080/federation-id-prov/saml", category);
                        e.request.replace(updatedState);
                        break;
                    }
                }
                break;
            default:
                break;
        }
    }
    
    public void setStateAttributeName(String stateAttributeName) {
        this.stateAttributeName = stateAttributeName;
    }
    
    public String getStateAttributeName() {
        return stateAttributeName;
    }
    
    public String getStateAttributeValue() {
        return stateAttributeValue;
    }

    public void setStateAttributeValue(String stateAttributeValue) {
        this.stateAttributeValue = stateAttributeValue;
    }
}
