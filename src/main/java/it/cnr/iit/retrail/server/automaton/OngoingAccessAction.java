/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.iit.retrail.server.automaton;

import it.cnr.iit.retrail.commons.automata.StateInterface;

/**
 *
 * @author kicco
 */
public class OngoingAccessAction extends PolicyDrivenAction {

    public OngoingAccessAction(StateInterface targetState, StateInterface targetFailState, String name) {
        super(targetState, targetFailState, name);
    }
    
}
