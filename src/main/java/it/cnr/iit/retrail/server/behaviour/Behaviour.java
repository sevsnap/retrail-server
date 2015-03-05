/*
 * CNR - IIT
 * Coded by: 2015 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.behaviour;

import it.cnr.iit.retrail.commons.DomUtils;
import it.cnr.iit.retrail.commons.Pool;
import it.cnr.iit.retrail.commons.StateType;
import it.cnr.iit.retrail.commons.automata.ActionInterface;
import it.cnr.iit.retrail.commons.automata.StateInterface;
import it.cnr.iit.retrail.commons.impl.PepResponse;
import it.cnr.iit.retrail.server.dal.UconSession;
import it.cnr.iit.retrail.server.impl.UCon;
import java.lang.reflect.Constructor;
import java.util.Collection;
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

    private final Map<String, PDPPool> poolMap = new HashMap<>();
    private long timeout = 1000;
    private final UCon ucon;
    private final Element behaviouralConfiguration;
    private final UConAutomaton archetype;
    private final Set<String> busyJar = new HashSet<>();

    public Behaviour(UCon ucon, Element behaviouralConfiguration) throws Exception {
        super(64);
        assert (behaviouralConfiguration != null);
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

    /**
     * setPolicy()
     *
     * changes the current policy by reading the new one from the stream. The
     * input format must be xacml3. If the UCon service has already been started
     * by init() and you are changing the ON policy, the current ongoing
     * sessions are re-evaluated.
     *
     * @param policyElement the element containing the new policy to be set, in
     * xacml3 format.
     * @throws java.lang.Exception
     */

    /**
     * setPolicy()
     *
     * changes the current policy by reading the new one from the stream. The
     * input format must be xacml3. If the UCon service has already been started
     * by init() and you are changing the ON policy, the current ongoing
     * sessions are re-evaluated.
     *
     * @param action the action.
     * @param policyElement the element containing the new policy to be set, in
     * xacml3 format.
     * @throws java.lang.Exception
     */
    protected synchronized void setPolicy(ActionInterface action, Element policyElement) throws Exception {
        assert (policyElement.getTagName().equals("Policy"));
        String policyName = action.getOriginState().getName() + "-" + action.getName();
        poolMap.put(policyName, new PDPPool(policyElement));
        if (((UConState) action.getOriginState()).getType() == StateType.ONGOING && ucon.isInited()) {
            Collection<UconSession> sessions = ucon.getDAL().listSessions(StateType.ONGOING);
            if (sessions.size() > 0) {
                log.warn("UCon already running, reevaluating {} currently opened sessions", sessions.size());
                ucon.reevaluateSessions(sessions);
            }
        }
    }

    protected synchronized PDPPool getPDPPool(ActionInterface action) {
        String policyName = action.getOriginState().getName() + "-" + action.getName();
        return poolMap.get(policyName);
    }

    private UConAutomaton newObject(boolean firstTime) throws Exception {
        UConAutomaton a = new UConAutomaton(ucon);
        // Create states
        NodeList stateNodes = behaviouralConfiguration.getElementsByTagNameNS(UCon.uri, "State");
        for (int i = 0; i < stateNodes.getLength(); i++) {
            Element stateElement = (Element) stateNodes.item(i);
            String name = stateElement.getAttribute("name");
            String type = stateElement.getAttribute("type");
            UConState uconState = new UConState(name, StateType.valueOf(type));
            DomUtils.setPropertyOnObjectNS(UCon.uri, "Property", stateElement, uconState);

            a.addState(uconState);
            switch (uconState.getType()) {
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
                    throw new RuntimeException("state type unknown: " + uconState.getType());
            }
            log.debug("created ", uconState);
        }

        // Create actions
        NodeList actionNodes = behaviouralConfiguration.getElementsByTagNameNS(UCon.uri, "Action");

        for (int i = 0; i < actionNodes.getLength(); i++) {
            Element actionElement = (Element) actionNodes.item(i);
            String name = actionElement.getAttribute("name");
            String className = actionElement.getAttribute("class");
            if (className == null) {
                throw new RuntimeException("class attribute is mandatory for Action element");
            }
            String targetStateName = actionElement.getAttribute("target");
            StateInterface target = a.getState(targetStateName);
            String sourceStateName = actionElement.getAttribute("source");
            StateInterface source = (UConState) a.getState(sourceStateName);
            log.warn("loading Action with class: {}", className);
            Class<?> clazz = Class.forName(className);
            if (!PDPAction.class.isAssignableFrom(clazz)) {
                throw new RuntimeException("class " + className + " has no PDPAction interface and cannot be used in a Behaviour");
            }
            Constructor<?> ctor;
            try {
                ctor = clazz.getConstructor(StateInterface.class, StateInterface.class, String.class, Behaviour.class);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Action class " + className + " has not the standard constructor call");
            }
            PDPAction action = (PDPAction) ctor.newInstance(new Object[]{source, target, name, this});
            DomUtils.setPropertyOnObjectNS(UCon.uri, "Property", actionElement, action);
            Collection<Element> targetList = DomUtils.findDirectChildrenWithTagNS(UCon.uri, "Target", actionElement);
            for(Element targetElement: targetList) {
                String auxTargetStateName = targetElement.getAttribute("state");
                StateInterface auxTarget = a.getState(auxTargetStateName);
                String decisionName = targetElement.getAttribute("decision");
                PepResponse.DecisionEnum decision = PepResponse.DecisionEnum.valueOf(decisionName);
                action.setTargetState(auxTarget, decision);
            }
                
            if (firstTime) {
                Element policyElement = (Element) actionElement.getElementsByTagName("Policy").item(0);
                if (policyElement != null) {
                    setPolicy(action, policyElement);
                }
            }
            source.addAction(action); // FIXME REMOVE THIS!
        }
        // ready to go
        a.setCurrentState(a.getBegin());
        if (firstTime) {
            a.checkIntegrity();
            a.printInfo();
        }
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
        log.debug("obtaining automaton for {}", session.getUuid());
        long start = System.currentTimeMillis();
        synchronized (busyJar) {
            while (busyJar.contains(session.getUuid())) {
                log.info("waiting for {}", session.getUuid());
                busyJar.wait(getTimeout());
                if (System.currentTimeMillis() - start >= getTimeout()) {
                    throw new RuntimeException("timeout waiting for " + session);
                }
            }
            automaton = super.obtain();
            automaton.setSession(session);
            busyJar.add(session.getUuid());
        }
        log.debug("automaton obtained for {}", session.getUuid());
        return automaton;
    }

    @Override
    public void release(UConAutomaton automaton) {
        String uuid = automaton.getSession().getUuid();
        log.debug("releasing automaton for {}", uuid);
        synchronized (busyJar) {
            super.release(automaton);
            boolean removed = busyJar.remove(uuid);
            // tell obtain(session) that the object may be now taken and used.
            log.debug("automaton released for {}, contains? {}, removed? {}", uuid, busyJar.contains(uuid), removed);
            busyJar.notifyAll();
        }
    }

    public final UconSession apply(UconSession session, String actionName, Object... args) throws InterruptedException {
        UConAutomaton automaton = obtain(session);
        UconSession response = null;
        try {
            response = automaton.doThenMove(actionName, args);
        } catch (InterruptedException e) {
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
