/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.behaviour;

import it.cnr.iit.retrail.commons.StateType;
import it.cnr.iit.retrail.commons.automata.State;

/**
 *
 * @author oneadmin
 */
public class UConState extends State {
    private final String name;
    private final StateType type;
    
    public UConState(String name, StateType type) {
        super();
        this.name = name;
        this.type = type;
    }
    
    @Override
    public String getName() {
        return name;
    }

    public StateType getType() {
        return type;
    }
    
    @Override
    public String toString() {
        return name+"[type="+type+"]";
    }
}
