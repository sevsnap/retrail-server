/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.behaviour;

import it.cnr.iit.retrail.commons.automata.Automaton;
import it.cnr.iit.retrail.commons.automata.StateInterface;
import it.cnr.iit.retrail.server.dal.UconRequest;
import it.cnr.iit.retrail.server.dal.UconSession;
import it.cnr.iit.retrail.server.impl.UCon;
import it.cnr.iit.retrail.server.pip.ActionEvent;
import java.util.Collection;

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
/*
    @Override
    public void checkIntegrity() {
        super.checkIntegrity();
        log.info("performing additional automaton integrity checks");
        for(StateInterface s: getStates()) {
            UConState us = (UConState)s;
            for()
        }
        log.info("additional integrity checks ok");
    }
  */  
    public void setSession(UconSession session) {
        // Set current state
        if(session == null)
            throw new NullPointerException("automaton session cannot be null");
        this.session = session;
        setCurrentState(session.getStateName());
    }
    
    public UconSession getSession() {
        return session;
    }
    
    public UconSession doThenMove(String actionName, Object[] args) throws Exception {
        long start = System.currentTimeMillis();
        // rebuild pepAccessRequest for PIP's onBeforeStartAccess argument
        UconRequest uconRequest = ucon.getDAL().rebuildUconRequest(session);
        ucon.getPIPChain().refresh(uconRequest, session);
        //log.warn("current state = {}", getCurrentState());
        //log.warn("action name = {}", actionName);
        StateInterface s = getCurrentState();
        PDPAction action = (PDPAction) s.getAction(actionName);
        log.info("action = {}", action);
        ActionEvent event = new ActionEvent(action, uconRequest, session);
        ucon.getPIPChain().fireBeforeActionEvent(event);
        action.execute(uconRequest, session, args);
        // moving automaton
        s = action.getTargetState(session.getDecision());
        setCurrentState(s);
        session.setState((UConState) s);
        ucon.getDAL().saveSession(session, uconRequest);
        ucon.getPIPChain().fireAfterActionEvent(event);
        session.setMs(System.currentTimeMillis() - start);
        session.setUconUrl(ucon.myUrl);
        UconSession response = session;
        if(isFinished()) {
            log.warn("TERMINATING, action {}, state {}", action.getName(), getCurrentState().getName());
            // FIXME should not reload session!
            session = ucon.getDAL().getSession(session.getUuid(), ucon.myUrl);
            ucon.getDAL().endSession(session);
        } else 
            ucon.getDAL().saveSession(session, uconRequest); // FIXME SLOW
        return response;
    }

}
