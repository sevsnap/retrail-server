/*
 * CNR - IIT
 * Coded by: 2015 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.behaviour;

import it.cnr.iit.retrail.commons.automata.Action;
import it.cnr.iit.retrail.commons.automata.StateInterface;
import it.cnr.iit.retrail.commons.impl.PepResponse;
import it.cnr.iit.retrail.server.dal.UconRequest;
import it.cnr.iit.retrail.server.dal.UconSession;
import org.w3c.dom.Document;

/**
 *
 * @author oneadmin
 */
public class PDPAction extends Action {
    private StateInterface denyState;
    private StateInterface indeterminateState;
    private StateInterface notApplicableState;
    protected final Behaviour behaviour;
    private final String name;

    public PDPAction(StateInterface sourceState, StateInterface targetState, String name, Behaviour behaviour) {
        super(sourceState, targetState);
        this.behaviour = behaviour;
        this.name = name;
        denyState = indeterminateState = notApplicableState = sourceState;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setTargetState(StateInterface state, PepResponse.DecisionEnum decision) {
        switch(decision) {
            case Deny:
                denyState = state;
                break;
            case NotApplicable:
                notApplicableState = state;
                break;
            case Indeterminate:
                indeterminateState = state;
                break;
            default:
                throw new RuntimeException("cannot set target state for decision "+decision);
        }
    }
    
    public StateInterface getTargetState(PepResponse.DecisionEnum decision) {
        switch(decision) {
            case Permit:
                return targetState;
            case Deny:
                return denyState;
            case NotApplicable:
                return notApplicableState;
            case Indeterminate:
                return indeterminateState;
            default:
                throw new RuntimeException("unknown decision "+decision);
        }
    }

    public UconSession execute(UconRequest uconRequest, UconSession uconSession, Object[] args) {
        PDPPool pdpPool = behaviour.getPDPPool(this);
        if (pdpPool != null) {
            log.debug("executing {} with {}, {}", this, uconRequest, uconSession);
            Document rv = pdpPool.access(uconRequest);
            uconSession.setResponse(rv);
        } else {
            log.debug("executing {} with no policy applied (Permit)", this);
        }
        return uconSession;
    }

    @Override
    public String toString() {
        String t  = getName() + "() -> " + targetState;
        PDPPool p = behaviour.getPDPPool(this);
        if(p != null)
            t += " (on deny: " + denyState + "; " + p + ")";
        return t;
    }

}
