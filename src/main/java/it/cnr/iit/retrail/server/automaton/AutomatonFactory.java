/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.automaton;

import it.cnr.iit.retrail.commons.ActionEnum;
import it.cnr.iit.retrail.commons.Pool;
import it.cnr.iit.retrail.commons.Status;
import it.cnr.iit.retrail.commons.automata.ActionInterface;
import it.cnr.iit.retrail.commons.automata.StateInterface;
import it.cnr.iit.retrail.server.dal.UconSession;
import it.cnr.iit.retrail.server.impl.UCon;
import java.util.ArrayList;
import java.util.Collection;
import org.w3c.dom.Element;

/**
 *
 * @author oneadmin
 */
public final class AutomatonFactory extends Pool<UConAutomaton> {
    private final UCon ucon;
    private final UConAutomaton defaultBehaviouralAutomaton;
    private final Collection<PolicyDrivenAction> policyDrivenActions = new ArrayList<>();
    private final Collection<UConState> ongoingStates = new ArrayList<>();
    private final Collection<PolicyDrivenAction> ongoingAccessActions = new ArrayList<>();
    
    public AutomatonFactory(UCon ucon) throws Exception {
        super(64);
        this.ucon = ucon;
        log.warn("creating default behavioural automaton");
        defaultBehaviouralAutomaton = createDefault();
        log.warn("linking automaton to builtin policies (permit anything)");
        for (StateInterface state: defaultBehaviouralAutomaton.getStates()) {
            if(state.getName().equals("ONGOING")) // FIXME
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
    
    protected UConAutomaton createDefault() {
        // Create all states
        UConAutomaton a = new UConAutomaton(ucon);
        
        UConState INIT = new UConState(Status.INIT, a);
        UConState TRY = new UConState(Status.TRY, a);
        UConState ONGOING = new UConState(Status.ONGOING, a);
        UConState REVOKED = new UConState(Status.REVOKED, a);
        UConState REJECTED = new UConState(Status.REJECTED, a);
        UConState DELETED = new UConState(Status.DELETED, a);

        // Create all actions and link them to targets
        
        INIT.setActions(new ActionInterface[]{
            new PolicyDrivenAction(TRY, REJECTED, ActionEnum.tryAccess), 
        });
        TRY.setActions(new ActionInterface[]{
            new PolicyDrivenAction(ONGOING, TRY, ActionEnum.startAccess), 
            new PolicyDrivenAction(DELETED, TRY, ActionEnum.endAccess), 
        });
        ONGOING.setActions(new ActionInterface[]{
            new PolicyDrivenAction(DELETED, ONGOING, ActionEnum.endAccess), 
            new PolicyDrivenAction(ONGOING, REVOKED, ActionEnum.ongoingAccess),
        });
        REVOKED.setActions(new ActionInterface[]{
            new UconAction(DELETED, ActionEnum.endAccess),
        });

        a.init(INIT,
                new StateInterface[]{
                    REJECTED, DELETED
                },
                new StateInterface[]{
                    INIT, TRY, ONGOING, REVOKED, REJECTED, DELETED
                }
        );
       return a;
    }
    
    @Override
    protected UConAutomaton newObject() {
        return createDefault();
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
