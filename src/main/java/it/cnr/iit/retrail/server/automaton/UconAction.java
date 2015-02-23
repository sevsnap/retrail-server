/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.automaton;

import it.cnr.iit.retrail.commons.ActionEnum;
import it.cnr.iit.retrail.commons.automata.Action;
import it.cnr.iit.retrail.commons.automata.StateInterface;
import it.cnr.iit.retrail.server.dal.UconRequest;
import it.cnr.iit.retrail.server.dal.UconSession;
import it.cnr.iit.retrail.server.impl.UCon;
import org.w3c.dom.Document;

/**
 *
 * @author oneadmin
 */

public class UconAction extends Action {
    protected final UCon ucon;
    private final String name;
    
    public UconAction(StateInterface targetState, ActionEnum action) {
        super(targetState);
        ucon = (UCon) targetState.getAutomaton();
        this.name = action.name();
    }
    
    public void execute(UconRequest uconRequest, UconSession uconSession, Object[] args) {
        log.warn("action executed");
    }
}
