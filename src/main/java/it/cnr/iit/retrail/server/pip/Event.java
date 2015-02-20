/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.pip;

import it.cnr.iit.retrail.commons.PepRequestInterface;
import it.cnr.iit.retrail.commons.PepSessionInterface;
import it.cnr.iit.retrail.server.impl.UConAction;
import it.cnr.iit.retrail.server.impl.UConAutomaton;

/**
 *
 * @author oneadmin
 */
public class Event {
    public enum EventType {
        none,
        beforeTryAccess, afterTryAccess,
        beforeStartAccess, afterStartAccess,
        beforeRunObligations, afterRunObligations,
        beforeApplyChanges, afterApplyChanges,
        beforeRevokeAccess, afterRevokeAccess,
        beforeEndAccess, afterEndAccess
    };
    
    public final EventType type;
    public final PepRequestInterface request;
    public final PepSessionInterface session;
    public final Object ack;
    public final UConAction action;
    public final UConAutomaton automaton;
    
    public Event(EventType t, PepRequestInterface request) {
        type = t;
        this.request = request;
        session = null;
        ack = null;
        action = null;
        automaton = null;
    }
    
    public Event(EventType t, PepRequestInterface request, PepSessionInterface session) {
        type = t;
        this.request = request;
        this.session = session;
        ack = null;
        action = null;
        automaton = null;
    }

    public Event(EventType t, PepRequestInterface request, PepSessionInterface session, Object ack) {
        type = t;
        this.request = request;
        this.session = session;
        this.ack = ack;
        action = null;
        automaton = null;
    }
    
    public Event(UConAutomaton automaton, UConAction action, PepRequestInterface request, PepSessionInterface session, Object ack) {
        this.automaton = automaton;
        this.action = action;
        this.request = request;
        this.session = session;
        this.ack = ack;
        type = EventType.none;
    }
}
