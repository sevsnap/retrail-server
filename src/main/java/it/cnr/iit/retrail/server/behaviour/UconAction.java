/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.behaviour;

import it.cnr.iit.retrail.commons.automata.Action;
import it.cnr.iit.retrail.commons.automata.StateInterface;
import it.cnr.iit.retrail.server.dal.UconRequest;
import it.cnr.iit.retrail.server.dal.UconSession;
import it.cnr.iit.retrail.server.impl.UCon;

/**
 *
 * @author oneadmin
 */

public class UconAction extends Action {
    protected final UCon ucon;
    private final String name;
    
    public UconAction(StateInterface sourceState, StateInterface targetState, String name, UCon ucon) {
        super(sourceState, targetState);
        this.ucon = ucon;
        this.name = name;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    public void execute(UconRequest uconRequest, UconSession uconSession, Object[] args) {
        log.warn("action executed");
    }

}
