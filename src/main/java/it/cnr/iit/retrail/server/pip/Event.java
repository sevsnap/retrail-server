/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.pip;

import it.cnr.iit.retrail.commons.PepRequestInterface;
import it.cnr.iit.retrail.commons.PepSessionInterface;

/**
 *
 * @author oneadmin
 */
public class Event {
    public enum EventType {
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
    
    public Event(EventType t, PepRequestInterface request) {
        type = t;
        this.request = request;
        session = null;
        ack = null;
    }
    
    public Event(EventType t, PepRequestInterface request, PepSessionInterface session) {
        type = t;
        this.request = request;
        this.session = session;
        ack = null;
    }

    public Event(EventType t, PepRequestInterface request, PepSessionInterface session, Object ack) {
        type = t;
        this.request = request;
        this.session = session;
        this.ack = ack;
    }
}
