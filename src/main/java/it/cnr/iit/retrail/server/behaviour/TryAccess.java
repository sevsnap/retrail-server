/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.iit.retrail.server.behaviour;

import it.cnr.iit.retrail.commons.StateType;
import it.cnr.iit.retrail.commons.automata.StateInterface;

/**
 *
 * @author kicco
 */
public class TryAccess extends PDPAction {
    public static final String name = "tryAccess";

    public TryAccess(StateInterface sourceState, StateInterface targetState, String n, Behaviour behaviour) {
        super(sourceState, targetState, name, behaviour);
        if(n != null && name.equals(n))
            throw new RuntimeException("action name cannot be changed for "+getClass().getSimpleName());
        if(((UConState)sourceState).getType() != StateType.BEGIN)
            throw new RuntimeException("origin "+sourceState+" must be the initial state for action tryAccess");
    }
}
