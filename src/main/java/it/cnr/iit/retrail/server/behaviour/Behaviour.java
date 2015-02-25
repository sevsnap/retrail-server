/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.behaviour;

import it.cnr.iit.retrail.commons.DomUtils;
import it.cnr.iit.retrail.commons.Pool;
import it.cnr.iit.retrail.commons.Status;
import it.cnr.iit.retrail.commons.automata.ActionInterface;
import it.cnr.iit.retrail.commons.automata.StateInterface;
import it.cnr.iit.retrail.server.dal.UconRequest;
import it.cnr.iit.retrail.server.dal.UconSession;
import it.cnr.iit.retrail.server.impl.UCon;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author oneadmin
 */
public final class Behaviour extends Pool<UConAutomaton> {
    public final String uri = "http://security.iit.cnr.it/retrail/ucon";
    private final UCon ucon;
    private final Collection<PolicyDrivenAction> policyDrivenActions = new ArrayList<>();
    private final Collection<UConState> ongoingStates = new ArrayList<>();
    private final Collection<PolicyDrivenAction> ongoingAccessActions = new ArrayList<>();
    private final Document behaviouralConfiguration;
    private final UConAutomaton archetype;
    
    public Behaviour(UCon ucon, InputStream uconConfigStream) throws Exception {
        super(64);
        assert(uconConfigStream != null);
        this.ucon = ucon;
        log.warn("loading behavioural automaton {}", uconConfigStream);
        behaviouralConfiguration = DomUtils.read(uconConfigStream);
        log.warn("building behavioural automaton with builtin policies (permit anything)");
        archetype = newObject(true);
    }
    
    @Deprecated
    public StateInterface getBegin() {
        return archetype.getBegin();
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

    private UConAutomaton newObject(boolean firstTime) throws Exception {
        UConAutomaton a = new UConAutomaton(ucon);
        // Create states
        NodeList stateNodes = behaviouralConfiguration.getElementsByTagNameNS(uri, "State");
        for(int i = 0; i < stateNodes.getLength(); i++) {
            Element stateElement = (Element) stateNodes.item(i);
            String name = stateElement.getAttribute("name");
            String type = stateElement.getAttribute("type");
            UConState uconState = new UConState(name, Status.valueOf(type));
            a.addState(uconState);
            switch(uconState.getType()) {
                case BEGIN:
                    a.setBegin(uconState);
                    break;
                case ONGOING:
                    if(firstTime)
                        ongoingStates.add(uconState);
                    break;
                case END:
                    a.addEnd(uconState);
                    break;
                default:
                    log.warn("state type {} ignored", uconState.getType());
                    break;
            }
            log.debug("created ", uconState);
        }

        // Create actions
        
        NodeList actionNodes = behaviouralConfiguration.getElementsByTagNameNS(uri, "Action");
        for(int i = 0; i < actionNodes.getLength(); i++) {
            Element actionElement = (Element) actionNodes.item(i);
            String name = actionElement.getAttribute("name");
            String klass = actionElement.getAttribute("class");
            String targetStateName = actionElement.getAttribute("target");
            StateInterface target = a.getState(targetStateName);
            String sourceStateName = actionElement.getAttribute("source");
            StateInterface source = (UConState) a.getState(sourceStateName);
            UconAction action;
            switch(klass) {
                case "UconAction":
                    action = new UconAction(source, target, name, ucon);
                    break;
                case "OngoingAccessAction":
                case "PolicyDrivenAction": {
                    String failTargetStateName = actionElement.getAttribute("failTarget");
                    StateInterface failTarget = a.getState(failTargetStateName);
                    Element policyElement = (Element) actionElement.getElementsByTagName("Policy").item(0);
                    PolicyDrivenAction act = klass.equals("OngoingAccessAction")?
                            new OngoingAccess(source, target, failTarget, name, ucon) :
                            new PolicyDrivenAction(source, target, failTarget, name, ucon);
                    if(firstTime) {
                        act.setPolicy(policyElement);
                        policyDrivenActions.add(act);
                        if(klass.equals("OngoingAccessAction"))
                            ongoingAccessActions.add(act);
                    }
                    action = act;
                    break;
                }
                default:
                    throw new RuntimeException("unknown action class "+klass);
            }
            source.addAction(action); // FIXME REMOVE THIS!
        }
        // ready to go
        a.setCurrentState(a.getBegin());
        if(firstTime)
            a.printInfo();
        return a;
    }

    public final UconSession apply(UconSession session, String actionName, Object... args) {
        UConAutomaton automaton = obtain();
        automaton.setSession(session);
        UconSession response = null;
        try {
            response = automaton.doThenMove(actionName, args);
        } catch (Exception e) {
            throw new RuntimeException("while executing action " + actionName, e);
        } finally {
            release(automaton);
        }
        return response;
    }

    @Override
    protected UConAutomaton newObject() {
        try {
            return newObject(false);
        } catch (Exception ex) {
            log.error("while creating new automaton: {}", ex);
        }
        return null;
    }
}
