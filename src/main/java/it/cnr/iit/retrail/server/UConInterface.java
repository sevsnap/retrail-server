/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server;

import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.commons.RecorderInterface;
import it.cnr.iit.retrail.commons.automata.ActionInterface;
import it.cnr.iit.retrail.commons.automata.State;
import it.cnr.iit.retrail.commons.automata.StateInterface;
import it.cnr.iit.retrail.server.dal.DALInterface;
import it.cnr.iit.retrail.server.pip.PIPChainInterface;
import it.cnr.iit.retrail.server.pip.PIPInterface;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;

/**
 *
 * @author Enrico Carniani
 */
public interface UConInterface extends RecorderInterface {

    /**
     * init()
     *
     * starts the UCon service. Any open session is reevaluated during this
     * bootstrap phase. Must be called once, any other method call is allowed at
     * any time except for term().
     *
     * @throws Exception if anything goes wrong.
     */
    void init() throws Exception;

    void loadBehaviour(InputStream s) throws Exception;
    void defaultBehaviour() throws Exception;
    
    PIPChainInterface getPIPChain();
    DALInterface getDAL();

    /**
     * notifyChanges()
     *
     * informs the UCon that some changes have been applied to the given
     * attribute. This typically makes the UCon re-evaluate all requests
     * affected by the attribute changes because some policies may revoke access
     * to the respective ongoing sessions, that are in turn being notified to
     * the PEP holding the rights via the revokeAccess() method.
     *
     * @param changedAttribute the changed attribute to be notified.
     * @throws Exception if anything goes wrong.
     */
    void notifyChanges(PepAttributeInterface changedAttribute) throws Exception;

    /**
     * notifyChanges()
     *
     * informs the UCon that some changes have been applied to the given
     * attributes. This typically makes the UCon re-evaluate all requests
     * affected by the changed attributes because some policies may revoke
     * access to the respective ongoing sessions, that are in turn being
     * notified to the PEP holding the rights via the revokeAccess() method.
     *
     * @param changedAttributes the changed attributes collection to be
     * notified.
     * @throws Exception if anything goes wrong.
     */
    void notifyChanges(Collection<PepAttributeInterface> changedAttributes) throws Exception;

    /**
     * term()
     *
     * shuts down the UCon service. It also terminates all PIP modules by
     * removing them; may be overloaded.
     *
     * @throws InterruptedException if the main program has, for instance, asked
     * for thread interruption.
     */
    void term() throws InterruptedException;

    void setWatchdogPeriod(int watchdogPeriod);
    int getWatchdogPeriod();
}
