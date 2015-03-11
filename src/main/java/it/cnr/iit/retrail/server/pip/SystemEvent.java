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
public class SystemEvent extends Event {  
    
    public enum EventType {
        beforeApplyChanges, afterApplyChanges,
    };
    
    public final EventType type;

    public SystemEvent(EventType t, PepRequestInterface request) {
        super(request, null, null);
        type = t;
    }
    
    public SystemEvent(EventType t, PepRequestInterface request, PepSessionInterface session) {
        super(request, session, null);
        type = t;
    }

    public SystemEvent(EventType t, PepRequestInterface request, PepSessionInterface session, Object ack) {
        super(request, session, ack);
        type = t;
    }

}
