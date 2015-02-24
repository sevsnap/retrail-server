/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.automaton;

import it.cnr.iit.retrail.commons.ActionEnum;
import it.cnr.iit.retrail.commons.DomUtils;
import it.cnr.iit.retrail.commons.Pool;
import it.cnr.iit.retrail.commons.Status;
import it.cnr.iit.retrail.commons.automata.ActionInterface;
import it.cnr.iit.retrail.commons.automata.StateInterface;
import it.cnr.iit.retrail.server.dal.UconSession;
import it.cnr.iit.retrail.server.impl.UCon;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author oneadmin
 */
public final class AutomatonFactory extends Pool<UConAutomaton> {
    public final String uri = "http://security.iit.cnr.it/retrail/ucon";
    private final UCon ucon;
    private final Collection<PolicyDrivenAction> policyDrivenActions = new ArrayList<>();
    private final Collection<UConState> ongoingStates = new ArrayList<>();
    private final Collection<PolicyDrivenAction> ongoingAccessActions = new ArrayList<>();
    private final Document behaviouralConfiguration;
    
    public AutomatonFactory(UCon ucon, InputStream uconConfigStream) throws Exception {
        super(64);
        this.ucon = ucon;
        log.debug("loading behavioural automaton");
        behaviouralConfiguration = DomUtils.read(uconConfigStream);
        log.warn("building behavioural automaton with builtin policies (permit anything)");
        for (StateInterface state: newObject().getStates()) {
            
            if(((UConState)state).getType() == Status.ONGOING)
                ongoingStates.add((UConState)state);
            for(ActionInterface action: state.getNextActions())
                if(action instanceof PolicyDrivenAction) {
                    PolicyDrivenAction a = (PolicyDrivenAction)action;
                    policyDrivenActions.add(a);
                    a.setPolicy(null);
                    if(a.getName().equals("ongoingAccess")) // FIXME
                        ongoingAccessActions.add(a);
                }
        }
    }
    
    public Collection<PolicyDrivenAction> getPolicyDrivenActions() {
        return policyDrivenActions;
    }
    
    @Deprecated
    public UConState getOngoingState() {
        return ongoingStates.iterator().next();
    }
    
    @Deprecated
    public PDPPool getOngoingAccessPDPPool() {
        return ongoingAccessActions.iterator().next().getPDPPool();
    }

    @Override
    protected UConAutomaton newObject() {
        UConAutomaton a = new UConAutomaton(ucon);
        // Create states
        StateInterface begin = null;
        Collection<StateInterface> end = new ArrayList<>();
        NodeList stateNodes = behaviouralConfiguration.getElementsByTagNameNS(uri, "State");
        for(int i = 0; i < stateNodes.getLength(); i++) {
            Element stateElement = (Element) stateNodes.item(i);
            String name = stateElement.getAttribute("name");
            String type = stateElement.getAttribute("type");
            UConState uconState = new UConState(name, Status.valueOf(type));
            switch(uconState.getType()) {
                case INIT:
                    if(begin != null)
                        throw new RuntimeException("too many initial states. exactly one must be the initial state.");
                    begin = uconState;
                    break;
                case DELETED:
                    end.add(uconState);
                    break;
                default:
                    log.warn("state type {} ignored");
            }
            log.debug("created ", uconState);
        }
        if(begin == null)
            throw new RuntimeException("no initial state defined; use attribute type=\"BEGIN\" to set it");
        if(end.isEmpty())
            throw new RuntimeException("no final states defined; use attribute type=\"END\" to set one or more final states");
        a.setBegin(begin);
        a.setEnd(end);

        // Create actions
        
        NodeList actionNodes = behaviouralConfiguration.getElementsByTagNameNS(uri, "Action");
        for(int i = 0; i < actionNodes.getLength(); i++) {
            Element actionElement = (Element) actionNodes.item(i);
            String name = actionElement.getAttribute("name");
            String klass = actionElement.getAttribute("class");
            String targetStateName = actionElement.getAttribute("target");
            StateInterface target = a.getState(targetStateName);
            String sourceStateName = actionElement.getAttribute("target");
            StateInterface source = (UConState) a.getState(sourceStateName);
            UconAction action;
            switch(klass) {
                case "UconAction":
                    action = new UconAction(target, name);
                    break;
                case "PolicyDriveAction": {
                    String failTargetStateName = actionElement.getAttribute("failTarget");
                    StateInterface failTarget = a.getState(failTargetStateName);
                    action = new PolicyDrivenAction(target, failTarget, name);
                    break;
                }
                case "OngoingAccessAction": {
                    String failTargetStateName = actionElement.getAttribute("failTarget");
                    StateInterface failTarget = a.getState(failTargetStateName);
                    action = new OngoingAccessAction(target, failTarget, name);
                    break;
                }
                default:
                    throw new RuntimeException("unknown action class "+klass);
            }
            source.addAction(action);
        }
        log.info("automaton {} created", a);
        return a;
    }

    public final Element apply(UconSession session, String actionName, Object... args) {
        UConAutomaton automaton = obtain();
        automaton.setSession(session);
        Element response = null;
        try {
            response = automaton.doThenMove(actionName, args);
        } catch (Exception e) {
            throw new RuntimeException("while executing action " + actionName, e);
        } finally {
            release(automaton);
        }
        return response;
    }
}
