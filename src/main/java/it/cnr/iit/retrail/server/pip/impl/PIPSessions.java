/*
 * CNR - IIT
 * Coded by: 2014-2015 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.pip.impl;

import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.commons.StateType;
import it.cnr.iit.retrail.commons.impl.PepAttribute;
import it.cnr.iit.retrail.server.UConInterface;
import it.cnr.iit.retrail.server.dal.UconAttribute;
import it.cnr.iit.retrail.server.dal.UconSession;
import it.cnr.iit.retrail.server.pip.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kicco
 */
public class PIPSessions extends PIP {
    final public String category = PepAttribute.CATEGORIES.SUBJECT;
    private String attributeId = "openSessions";
    final private String attributeId2 = "CNRSessions";

    public PIPSessions() {
        super();
        this.log = LoggerFactory.getLogger(PIPSessions.class);
    }

    @Override
    public void init(UConInterface ucon) {
        super.init(ucon);
        int count = dal.listSessions(StateType.ONGOING).size()
                + dal.listSessions("REVOKED").size();
        log.info("creating shared attribute {} = {}", getAttributeId(), count);
        PepAttributeInterface sessions = newSharedAttribute(getAttributeId(), PepAttribute.DATATYPES.INTEGER, count, category);
        log.debug("the new attribute is: {}", sessions);
        sessions = getSharedAttribute(category, attributeId);
        assert (sessions != null);
        
/*        Collection<UconSession> session = dal.listSessions(StateType.ONGOING);
        count = 0;
        for(UconSession s:session){
            Collection<UconAttribute> attributes = s.getAttributes();
            for(UconAttribute a:attributes){
                if(a.getId().equals(attributeId2)) count++;
            }
        }*/
        log.info("creating shared attribute {} = {}", attributeId2, count);
        PepAttributeInterface CNRsessions = newSharedAttribute(attributeId2, PepAttribute.DATATYPES.INTEGER, count, category);
        CNRsessions = getSharedAttribute(category, attributeId2);
        log.debug("the new attribute is: {}", CNRsessions);
        assert (CNRsessions != null);
    }

    public int getSessions() {
        PepAttributeInterface sessions = getSharedAttribute(category, attributeId);
        assert (sessions != null);
        Integer v = Integer.parseInt(sessions.getValue());
        return v;
    }
    
    public String getAttributeId() {
        return attributeId;
    }

    public void setAttributeId(String attributeId) {
        this.attributeId = attributeId;
    }

    @Override
    public void fireAfterActionEvent(ActionEvent e) {
        log.info("originState = {}, e.session = {}", e.originState, e.session);
        if (e.originState.getType() != StateType.ONGOING
                && e.session.getStateType() == StateType.ONGOING) {
            PepAttributeInterface sessions = getSharedAttribute(category, attributeId);
            assert (sessions != null);
            Integer v = Integer.parseInt(sessions.getValue()) + 1;
            log.info("incrementing sessions to {}", v);
            sessions.setValue(v.toString());
            e.request.replace(sessions);
            
            if(((ArrayList<PepAttributeInterface>)(e.request.getAttributes(PepAttribute.CATEGORIES.SUBJECT, "role"))).get(0).getValue().equals("CNR")){
                PepAttributeInterface CNRsessions  = getSharedAttribute(category, attributeId2);
                assert (CNRsessions != null);
                v = Integer.parseInt(CNRsessions.getValue()) + 1;
                    log.info("incrementing CNRsessions to {}", v);
                    CNRsessions.setValue(v.toString());
                    e.request.replace(CNRsessions);
            }
                   
        } else if ((e.originState.getName().equals("REVOKED") ||
                e.originState.getType()== StateType.ONGOING)
                && e.session.getStateType() == StateType.END) {
            PepAttributeInterface sessions = getSharedAttribute(category, attributeId);
            assert (sessions != null);
            Integer v = Integer.parseInt(sessions.getValue()) - 1;
            log.info("decrementing sessions to {}", v);
            assert (v >= 0);
            sessions.setValue(v.toString());
            e.request.replace(sessions);
            
            if(((ArrayList<PepAttributeInterface>)(e.request.getAttributes(PepAttribute.CATEGORIES.SUBJECT, "role"))).get(0).getValue().equals("CNR")){
                PepAttributeInterface CNRsessions = getSharedAttribute(category, attributeId2);
                assert (sessions != null);
                v = Integer.parseInt(CNRsessions.getValue()) - 1;
                log.info("decrementing CNRsessions to {}", v);
                assert (v >= 0);
                CNRsessions.setValue(v.toString());
                e.request.replace(CNRsessions);
                try {
                    ucon.notifyChanges(CNRsessions);
                } catch (Exception ex) {
                    Logger.getLogger(PIPSessions.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

}
