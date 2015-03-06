/*
 * CNR - IIT
 * Coded by: 2015 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.pip.impl;

import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.commons.StateType;
import it.cnr.iit.retrail.commons.impl.PepAttribute;
import it.cnr.iit.retrail.server.UConInterface;
import it.cnr.iit.retrail.server.pip.ActionEvent;

/**
 *
 * @author kicco
 */
public class PIPState extends PIP {
    final public String category = PepAttribute.CATEGORIES.SUBJECT;

    private String attributeId = "sessionState";
    private String attributeValue = "init";

    @Override
    public void init(UConInterface ucon) {
        super.init(ucon);
        log.info("creating shared attribute {} = {}", attributeId, attributeValue);
        newSharedAttribute(attributeId, PepAttribute.DATATYPES.STRING, attributeValue, category);
    }
    
    @Override
    public void fireBeforeActionEvent(ActionEvent e) {
        if (e.session.getStateType() == StateType.BEGIN) {
            log.info("State attribute name: " + attributeId);
            PepAttributeInterface state = newSharedAttribute(getAttributeId(), PepAttribute.DATATYPES.STRING, getAttributeValue(), category);
            e.request.replace(state);
        }
    }

    @Override
    public void fireAfterActionEvent(ActionEvent e) {
        for (String obligation : e.session.getObligations()) {
            log.warn("FOUND OBLIGATION: {}", obligation);
            if (obligation.startsWith(attributeId + "=")) {
                String newStateAttributeValue = obligation.split("=")[1];
                log.warn("FOUND OBLIGATION FOR STATE CHANGE: {}", newStateAttributeValue);
                PepAttributeInterface updatedState = newSharedAttribute(getAttributeId(), PepAttribute.DATATYPES.STRING, newStateAttributeValue, category);
                e.request.replace(updatedState);
                break;
            }
        }
    }

    public void setAttributeId(String attributeId) {
        this.attributeId = attributeId;
    }

    public String getAttributeId() {
        return attributeId;
    }

    public String getAttributeValue() {
        return attributeValue;
    }

    public void setAttributeValue(String attributeValue) {
        this.attributeValue = attributeValue;
    }

}
