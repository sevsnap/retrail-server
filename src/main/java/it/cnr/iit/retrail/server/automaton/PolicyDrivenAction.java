/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.automaton;

import it.cnr.iit.retrail.commons.ActionEnum;
import it.cnr.iit.retrail.commons.Status;
import it.cnr.iit.retrail.commons.automata.StateInterface;
import it.cnr.iit.retrail.commons.impl.PepResponse;
import it.cnr.iit.retrail.server.dal.UconRequest;
import it.cnr.iit.retrail.server.dal.UconSession;
import it.cnr.iit.retrail.server.impl.UCon;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.w3c.dom.Document;

/**
 *
 * @author oneadmin
 */

public class PolicyDrivenAction extends UconAction {
    private static final String defaultPolicyPath = "/META-INF/default-policies/";
    private static final Map<String, PDPPool> poolMap = new HashMap<>();
    private final StateInterface targetFailState;
    private PepResponse.DecisionEnum lastDecision;
    
    public PolicyDrivenAction(StateInterface targetState, StateInterface targetFailState, String name) {
        super(targetState, name);
        this.targetFailState = targetFailState;
    }
    
    private String getPolicyName() {
        return getOriginState().getName() + "-" + getName();
    }

    public PDPPool getPDPPool() {
        return poolMap.get(getPolicyName());
    }
    
    /**
     * setPolicy()
     *
     * changes the current policy by reading the new one from the stream. The
     * input format must be xacml3. If the UCon service has already been started
     * by init() and you are changing the ON policy, the current ongoing
     * sessions are re-evaluated.
     *
     * @param stream the stream containing the new policy to be set, in xacml3
     * format.
     * @throws java.lang.Exception
     */

    public synchronized void setPolicy(InputStream stream) throws Exception {
        if (stream == null) {
            log.warn("creating pool with default policy {}", getPolicyName());
            stream = UCon.class.getResourceAsStream(defaultPolicyPath + getPolicyName() + ".xml");
            poolMap.put(getPolicyName(), new PDPPool(stream));
        } else {
            log.warn("creating pool policy {} from url: {}", getPolicyName(), stream);
            poolMap.put(getPolicyName(), new PDPPool(stream));
        }
        if (getOriginState().getName().equals("ONGOING") && ucon.isInited()) { // FIXME
            Collection<UconSession> sessions = ucon.getDAL().listSessions(Status.ONGOING);
            if (sessions.size() > 0) {
                log.warn("UCon already running, reevaluating {} currently opened sessions", sessions.size());
                ucon.reevaluateSessions(sessions);
            }
        }
    }
    
    @Override
    public StateInterface getTargetState() {
        return lastDecision == PepResponse.DecisionEnum.Permit? targetState : targetFailState;
    }
    
    @Override
    public void execute(UconRequest uconRequest, UconSession uconSession, Object[] args) {
        log.warn("executing action");
        Document rv = getPDPPool().access(uconRequest);
        uconSession.setResponse(rv);
        lastDecision = uconSession.getDecision();
    }
}
