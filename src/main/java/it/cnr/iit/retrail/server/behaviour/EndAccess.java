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
public class EndAccess extends PDPAction {
    public static final String name = "endAccess";

    public EndAccess(StateInterface sourceState, StateInterface targetState, String n, Behaviour behaviour) {
        super(sourceState, targetState, name, behaviour);
        if(n != null && name.equals(n))
            throw new RuntimeException("action name cannot be changed for "+getClass().getSimpleName());
                if(((UConState)targetState).getType() != StateType.END)
            throw new RuntimeException("target "+targetState+" must be a final state for action endAccess");
    }
}
