/*
 * CNR - IIT
 * Coded by: 2015 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.behaviour;

import it.cnr.iit.retrail.commons.DomUtils;
import it.cnr.iit.retrail.commons.Pool;
import it.cnr.iit.retrail.commons.StateType;
import it.cnr.iit.retrail.commons.automata.StateInterface;
import it.cnr.iit.retrail.server.dal.UconSession;
import it.cnr.iit.retrail.server.impl.UCon;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author oneadmin
 */
public final class Behaviour extends Pool<UConAutomaton> {
    private long timeout = 1000;
    private final UCon ucon;
    private final Element behaviouralConfiguration;
    private final UConAutomaton archetype;
    private final Set<String> busyJar = new HashSet<>();
    
    public Behaviour(UCon ucon, Element behaviouralConfiguration) throws Exception {
        super(64);
        assert(behaviouralConfiguration != null);
        this.ucon = ucon;
        this.behaviouralConfiguration = behaviouralConfiguration;
        log.warn("building archetype behaviour");
        archetype = newObject(true);
        DomUtils.setPropertyOnObjectNS(ucon.uri, "Property", behaviouralConfiguration, this);
    }
    
    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {   // FIXME should be long
        log.warn("timeout set to {} ms", timeout);
        this.timeout = timeout;
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
    
    @Override
    public UConAutomaton obtain() {
        throw new UnsupportedOperationException("obtain() is not supported because it's not synchronized. Please use obtain(session) instead.");
    }
    
    // This implementation is responsible of automaton synchronization.
    // Once obtained, the automaton is waited for it to be released, until a 
    // timeout is reached; if not released in time, an exception is thrown 
    // and the whole underlying operation will fail.
    
    public UConAutomaton obtain(UconSession session) throws InterruptedException {
        UConAutomaton automaton;
        long start = System.currentTimeMillis();
        synchronized(busyJar) {
            while(busyJar.contains(session.getUuid())) {
                log.info("waiting for {}", session);
                busyJar.wait(getTimeout());
                if(System.currentTimeMillis()-start > getTimeout())
                    throw new RuntimeException("timeout waiting for "+session);
            }
            automaton = super.obtain();
            automaton.setSession(session);
            busyJar.add(session.getUuid());
        }
        return automaton;
    }
    
    @Override
    public void release(UConAutomaton automaton) {
        synchronized(busyJar) {
            super.release(automaton);
            assert(busyJar.remove(automaton.getSession().getUuid()));
            // tell obtain(session) that the object may be now taken and used.
            busyJar.notify();
        }
    }
    
    public final UconSession apply(UconSession session, String actionName, Object... args) throws InterruptedException {
        UConAutomaton automaton = obtain(session);
        UconSession response = null;
        try {
            response = automaton.doThenMove(actionName, args);
        } catch(InterruptedException e) {
            log.warn("interrupted while executing action {} on {}", actionName, automaton);
            throw e;
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
