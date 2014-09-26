/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */

package it.cnr.iit.retrail.server;

import it.cnr.iit.retrail.commons.PepRequestAttribute;
import it.cnr.iit.retrail.server.pip.PIPInterface;
import java.util.Collection;

/**
 *
 * @author oneadmin
 */
public interface UConInterface {

    void init() throws Exception;
    
    void addPIP(PIPInterface p);

    PIPInterface removePIP(PIPInterface p);
    
    void notifyChanges(PepRequestAttribute changedAttribute) throws Exception;

    void notifyChanges(Collection<PepRequestAttribute> changedAttributes) throws Exception;

    void term() throws Exception;
}
