/*
 * CNR - IIT
 * Coded by: 2015 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.behaviour;

import it.cnr.iit.retrail.commons.Pool;
import it.cnr.iit.retrail.commons.StateType;
import it.cnr.iit.retrail.commons.automata.StateInterface;
import it.cnr.iit.retrail.server.dal.UconSession;
import it.cnr.iit.retrail.server.impl.UCon;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author oneadmin
 */
public final class Behaviour extends Pool<UConAutomaton> {
    private final UCon ucon;
    private final Element behaviouralConfiguration;
    private final UConAutomaton archetype;
    
    public Behaviour(UCon ucon, Element behaviouralConfiguration) throws Exception {
        super(64);
        assert(behaviouralConfiguration != null);
        this.ucon = ucon;
        this.behaviouralConfiguration = behaviouralConfiguration;
        log.warn("building archetype behaviour");
        archetype = newObject(true);
    }
    
    @Deprecated
    public StateInterface getBegin() {
        return archetype.getBegin();
    }
    
    private UConAutomaton newObject(boolean firstTime) throws Exception {
        UConAutomaton a = new UConAutomaton(ucon);
        // Create states
        NodeList stateNodes = behaviouralConfiguration.getElementsByTagNameNS(UCon.uri, "State");
        for(int i = 0; i < stateNodes.getLength(); i++) {
            Element stateElement = (Element) stateNodes.item(i);
            String name = stateElement.getAttribute("name");
            String type = stateElement.getAttribute("type");
            UConState uconState = new UConState(name, StateType.valueOf(type));
            a.addState(uconState);
            switch(uconState.getType()) {
                case BEGIN:
                    a.setBegin(uconState);
                    break;
                case PASSIVE:
                case ONGOING:
                case REVOKED:
                    break;
                case END:
                    a.addEnd(uconState);
                    break;
                default:
                    throw new RuntimeException("state type unknown: "+ uconState.getType());
            }
            log.debug("created ", uconState);
        }

        // Create actions
        
        NodeList actionNodes = behaviouralConfiguration.getElementsByTagNameNS(UCon.uri, "Action");
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
                case "":
                case "UconAction":
                    action = new UconAction(source, target, name, ucon);
                    break;
                case "OngoingAccess":
                    if(name != null && name.length() > 0 && !name.equals(OngoingAccess.name))
                        throw new RuntimeException("action name cannot be specified for OngoingAccess");
                case "PolicyDrivenAction": {
                    String failTargetStateName = actionElement.getAttribute("failTarget");
                    StateInterface failTarget = a.getState(failTargetStateName);
                    PolicyDrivenAction act = klass.equals("OngoingAccess")?
                            new OngoingAccess(source, target, failTarget, ucon) :
                            new PolicyDrivenAction(source, target, failTarget, name, ucon);
                    if(firstTime) {
                        Element policyElement = (Element) actionElement.getElementsByTagName("Policy").item(0);
                        act.setPolicy(policyElement);
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
            log.error("while creating new behavioural automaton: {}", ex);
        }
        return null;
    }
}
