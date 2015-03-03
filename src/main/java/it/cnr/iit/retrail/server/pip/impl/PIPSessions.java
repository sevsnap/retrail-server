/*
 * CNR - IIT
 * Coded by: 2014-2015 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.pip.impl;

import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.commons.StateType;
import it.cnr.iit.retrail.server.UConInterface;
import it.cnr.iit.retrail.server.pip.ActionEvent;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kicco
 */
public class PIPSessions extends PIP {

    final public String id = "openSessions";
    final public String category = "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject";

    public PIPSessions() {
        super();
        this.log = LoggerFactory.getLogger(PIPSessions.class);
    }

    @Override
    public void init(UConInterface ucon) {
        super.init(ucon);
        int count = dal.listSessions(StateType.ONGOING).size()
                + dal.listSessions("REVOKED").size();
        log.info("creating shared attribute {} = {}", id, count);
        PepAttributeInterface sessions = newSharedAttribute(id, "http://www.w3.org/2001/XMLSchema#integer", Integer.toString(count), "http://localhost:8080/federation-id-prov/saml", category);
        log.debug("the new attribute is: {}", sessions);
        sessions = getSharedAttribute(category, id);
        assert (sessions != null);
    }

    public int getSessions() {
        PepAttributeInterface sessions = getSharedAttribute(category, id);
        assert (sessions != null);
        Integer v = Integer.parseInt(sessions.getValue());
        return v;
    }

    @Override
    public void fireAfterActionEvent(ActionEvent e) {
        log.info("originState = {}, e.session = {}", e.originState, e.session);
        if (e.originState.getType() != StateType.ONGOING
                && e.session.getStateType() == StateType.ONGOING) {
            PepAttributeInterface sessions = getSharedAttribute(category, id);
            assert (sessions != null);
            Integer v = Integer.parseInt(sessions.getValue()) + 1;
            log.info("incrementing sessions to {}", v);
            sessions.setValue(v.toString());
            e.request.replace(sessions);
        } else if ((e.originState.getName().equals("REVOKED") ||
                e.originState.getType()== StateType.ONGOING)
                && e.session.getStateType() == StateType.END) {
            PepAttributeInterface sessions = getSharedAttribute(category, id);
            assert (sessions != null);
            Integer v = Integer.parseInt(sessions.getValue()) - 1;
            log.info("decrementing sessions to {}", v);
            assert (v >= 0);
            sessions.setValue(v.toString());
            e.request.replace(sessions);
        }
    }

}
