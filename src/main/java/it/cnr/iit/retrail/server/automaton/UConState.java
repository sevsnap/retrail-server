/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.automaton;

import it.cnr.iit.retrail.commons.Status;
import it.cnr.iit.retrail.commons.automata.ActionInterface;
import it.cnr.iit.retrail.commons.automata.State;

/**
 *
 * @author oneadmin
 */
public class UConState extends State {
    private final String name;
    private final Status type;
    
    public UConState(String name, Status type) {
        super();
        this.name = name;
        this.type = type;
    }
    
    @Override
    public String getName() {
        return name;
    }

    public Status getType() {
        return type;
    }
}
