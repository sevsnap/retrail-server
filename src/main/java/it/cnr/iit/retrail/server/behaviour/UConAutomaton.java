/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.behaviour;

import it.cnr.iit.retrail.commons.DomUtils;
import it.cnr.iit.retrail.commons.Status;
import it.cnr.iit.retrail.commons.automata.Automaton;
import it.cnr.iit.retrail.commons.impl.PepResponse;
import it.cnr.iit.retrail.server.dal.UconRequest;
import it.cnr.iit.retrail.server.dal.UconSession;
import it.cnr.iit.retrail.server.impl.UCon;
import it.cnr.iit.retrail.server.pip.Event;
import java.net.URL;
import javax.persistence.NoResultException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 * @author oneadmin
 */
public class UConAutomaton extends Automaton {

    protected final UCon ucon;
    private UconSession session;
    
    UConAutomaton(UCon ucon) {
        super();
        this.ucon = ucon;
    }

    public void setSession(UconSession session) {
        // Set current state
        if(session == null)
            throw new NullPointerException("automaton session cannot be null");
        this.session = session;
        setCurrentState(session.getStateName());
    }
    
    public Element doThenMove(String actionName, Object[] args) throws Exception {
        long start = System.currentTimeMillis();
        // rebuild pepAccessRequest for PIP's onBeforeStartAccess argument
        UconRequest uconRequest = ucon.getDAL().rebuildUconRequest(session);
        ucon.getPIPChain().refresh(uconRequest, session);
        log.warn("current state = {}", getCurrentState());
        log.warn("action name = {}", actionName);
        PolicyDrivenAction action = (PolicyDrivenAction) getCurrentState().getAction(actionName);
        log.warn("action = {}", action);
        Event event = new Event(this, action, uconRequest, session, null);
        ucon.getPIPChain().fireBeforeActionEvent(event);
        action.execute(uconRequest, session, args);
        move(actionName);
        session.setStatus(((UConState)getCurrentState()).getType());
        session.setStateName(getCurrentState().getName());
        ucon.getDAL().saveSession(session, uconRequest);
        ucon.getPIPChain().fireAfterActionEvent(event);
        if(isFinished()) {
            log.warn("TERMINATING, action {}, state {}", action.getName(), getCurrentState().getName());
            ucon.getDAL().endSession(session);
        } else 
            ucon.getDAL().saveSession(session, uconRequest);
        session.setMs(System.currentTimeMillis() - start);
        session.setUconUrl(ucon.myUrl);

        return session.toXacml3Element();
    }

}
