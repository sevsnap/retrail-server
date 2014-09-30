/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */

package it.cnr.iit.retrail.server;

import it.cnr.iit.retrail.commons.PepRequestAttribute;
import it.cnr.iit.retrail.server.pip.PIPInterface;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;

/**
 *
 * @author Enrico Carniani
 */
public interface UConInterface {

    /**
     * init()
     * 
     * starts the UCon service. Any open session is reevaluated during this
     * bootstrap phase.
     * Must be called once, any other method call is allowed at any time except
     * for term().
     * 
     * @throws Exception if anything goes wrong.
     */
    void init() throws Exception;
    
    /**
     * addPIP()
     * 
     * adds a PIP to the PIP chain. The UCon may have 0 or more PIPs in order
     * to add or alter attributes for each request (see the PIPInterface 
     * documentation for further information).
     * @param p the PIP to be added to the chain.
     */
    void addPIP(PIPInterface p);

    /**
     * removePIP()
     * 
     * removes a PIP from the PIP chain.
     * The UCon may have 0 or more PIP in order to add or alter attributes for
     * each request (see the PIPInterface documentation for further 
     * information).
     * 
     * @param p the PIP to be removed from the chain.
     * @return the removed PIP instance.
     */
    PIPInterface removePIP(PIPInterface p);
    
    /**
     * notifyChanges()
     * 
     * informs the UCon that some changes have been applied to the given 
     * attribute.
     * This typically makes the UCon re-evaluate all requests affected by the
     * attribute changes because some policies may revoke access to the 
     * respective ongoing sessions, that are in turn being notified to the
     * PEP holding the rights via the revokeAccess() method.
     * 
     * @param changedAttribute the changed attribute to be notified.
     * @throws Exception if anything goes wrong.
     */
    void notifyChanges(PepRequestAttribute changedAttribute) throws Exception;

    /**
     * notifyChanges()
     * 
     * informs the UCon that some changes have been applied to the given 
     * attributes.
     * This typically makes the UCon re-evaluate all requests affected by the
     * changed attributes because some policies may revoke access to the 
     * respective ongoing sessions, that are in turn being notified to the
     * PEP holding the rights via the revokeAccess() method.
     * 
     * @param changedAttributes the changed attributes collection to be notified.
     * @throws Exception if anything goes wrong.
     */
    void notifyChanges(Collection<PepRequestAttribute> changedAttributes) throws Exception;

    /**
     * term()
     * 
     * shuts down the UCon service.
     * It also terminates all PIP modules by removing them; may be overloaded.
     * 
     * @throws InterruptedException if the main program has, for instance, asked
     * for thread interruption.
     */
    void term() throws InterruptedException;
    
    /**
     * setOngoingPolicy()
     * 
     * changes the current ongoing policy by reading the new one from the
     * stream. The input format must be xacml3.
     * If the UCon service has already been started by init(), the current 
     * ongoing sessions are re-evaluated.
     * 
     * @param s the input stream containing the new policy to be set, in
     * xacml3 format.
     */
    void setOngoingPolicy(InputStream s);

    /**
     * setOngoingPolicy()
     * 
     * changes the current ongoing policy by reading the new one from the
     * URL. The input format must be xacml3.
     * If the UCon service has already been started by init(), the current 
     * ongoing sessions are re-evaluated.
     * 
     * @param url the url containing the new policy to be set, in
     * xacml3 format.
     */
    void setOngoingPolicy(URL url);

    /**
     * setPreauthPolicy()
     * 
     * changes the current preauth policy by reading the new one from the
     * stream. The input format must be xacml3.
     * 
     * @param s the input stream containing the new policy to be set, in
     * xacml3 format.
     */
    void setPreauthPolicy(InputStream s);

    /**
     *     * setPreauthPolicy()
     * 
     * changes the current preauth policy by reading the new one from the
     * URL. The input format must be xacml3.
     * 
     * @param url the url containing the new policy to be set, in
     * xacml3 format.
     */
    void setPreauthPolicy(URL url);
}
