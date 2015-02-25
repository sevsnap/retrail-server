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

    public final PepRequestInterface request;
    public final PepSessionInterface session;
    public final Object ack;
    
    public Event(PepRequestInterface request, PepSessionInterface session, Object ack) {
        this.request = request;
        this.session = session;
        this.ack = ack;
    }
}
