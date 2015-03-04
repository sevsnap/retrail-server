/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.iit.retrail.server.behaviour;

import it.cnr.iit.retrail.commons.automata.StateInterface;
import it.cnr.iit.retrail.server.impl.UCon;

/**
 *
 * @author kicco
 */
public class EndAccess extends PolicyDrivenAction {
    public static final String name = "endAccess";

    public EndAccess(StateInterface sourceState, StateInterface targetState, StateInterface targetFailState, String n, UCon ucon) {
        super(sourceState, targetState, targetFailState, name, ucon);
        if(n != null && name.equals(n))
            throw new RuntimeException("action name cannot be changed for "+getClass().getSimpleName());
    }
}
