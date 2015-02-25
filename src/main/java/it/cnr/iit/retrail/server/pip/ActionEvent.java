/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.pip;

import it.cnr.iit.retrail.commons.PepRequestInterface;
import it.cnr.iit.retrail.commons.PepSessionInterface;
import it.cnr.iit.retrail.server.behaviour.UConAutomaton;
import it.cnr.iit.retrail.server.behaviour.UconAction;

/**
 *
 * @author oneadmin
 */
public class ActionEvent extends Event {

    public final UconAction action;
    public final UConAutomaton automaton;

    public ActionEvent(UConAutomaton automaton, UconAction action, PepRequestInterface request, PepSessionInterface session, Object ack) {
        super(request, session, ack);
        this.automaton = automaton;
        this.action = action;
    }
}
