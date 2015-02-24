/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.behaviour;

import it.cnr.iit.retrail.commons.DomUtils;
import it.cnr.iit.retrail.commons.Status;
import it.cnr.iit.retrail.commons.automata.StateInterface;
import it.cnr.iit.retrail.commons.impl.PepResponse;
import it.cnr.iit.retrail.server.dal.UconRequest;
import it.cnr.iit.retrail.server.dal.UconSession;
import it.cnr.iit.retrail.server.impl.UCon;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author oneadmin
 */

public class PolicyDrivenAction extends UconAction {
    private static final String defaultPolicyPath = "/META-INF/default-policies/";
    private static final Map<String, PDPPool> poolMap = new HashMap<>();
    private final StateInterface targetFailState;
    private PepResponse.DecisionEnum lastDecision;
    private String policyId;
    
    public PolicyDrivenAction(StateInterface sourceState, StateInterface targetState, StateInterface targetFailState, String name, UCon ucon) {
        super(sourceState, targetState, name, ucon);
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
     * @param policyElement  the element containing the new policy to be set, 
     * in xacml3 format.
     * @throws java.lang.Exception
     */

    public synchronized void setPolicy(Element policyElement) throws Exception {
        policyId = policyElement.getAttribute("PolicyId");
        log.warn("creating pool policy {} (PolicyId: {})", getPolicyName(), policyId);
        String policyString = DomUtils.toString(policyElement);
        poolMap.put(getPolicyName(), new PDPPool(policyString));
        if (((UConState)getOriginState()).getType() == Status.ONGOING && ucon.isInited()) { 
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
    
    public void onFail(UconRequest uconRequest, UconSession uconSession, Object[] args) {
        log.warn("doing nothing");
    }
    
    @Override
    public void execute(UconRequest uconRequest, UconSession uconSession, Object[] args) {
        log.warn("executing action {} with {}, {}", getName(), uconRequest, uconSession);
        Document rv = getPDPPool().access(uconRequest);
        uconSession.setResponse(rv);
        lastDecision = uconSession.getDecision();
        if(lastDecision != PepResponse.DecisionEnum.Permit)
            onFail(uconRequest, uconSession, args);
    }
    
    @Override 
    public String toString() {
        return getName()+"() -> "+targetState+" (on fail: "+targetFailState+"; PolicyId: "+policyId+")";
    }
    
}
