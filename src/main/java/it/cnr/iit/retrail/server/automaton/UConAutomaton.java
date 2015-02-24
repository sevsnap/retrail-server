/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.automaton;

import it.cnr.iit.retrail.commons.DomUtils;
import it.cnr.iit.retrail.commons.Status;
import it.cnr.iit.retrail.commons.automata.Automaton;
import it.cnr.iit.retrail.commons.impl.PepResponse;
import it.cnr.iit.retrail.server.dal.UconRequest;
import it.cnr.iit.retrail.server.dal.UconSession;
import it.cnr.iit.retrail.server.impl.UCon;
import it.cnr.iit.retrail.server.pip.Event;
import java.net.URL;
import javax.persistence.NoResultException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 * @author oneadmin
 */
public class UConAutomaton extends Automaton {

    protected final UCon ucon;
    private UconSession session;
    
    UConAutomaton(UCon ucon) {
        super();
        this.ucon = ucon;
    }

    public void setSession(UconSession session) {
        // Set current state
        this.session = session;
        if(session != null) 
            setCurrentState(session.getStatus().name());
    }
    
    public Element doThenMove(String actionName, Object[] args) throws Exception {
        long start = System.currentTimeMillis();
        // rebuild pepAccessRequest for PIP's onBeforeStartAccess argument
        UconRequest uconRequest = ucon.getDAL().rebuildUconRequest(session);
        ucon.getPIPChain().refresh(uconRequest, session);
        PolicyDrivenAction action = (PolicyDrivenAction) getCurrentState().getAction(actionName);
        Event event = new Event(this, action, uconRequest, session, null);
        ucon.getPIPChain().fireBeforeActionEvent(event);
        action.execute(uconRequest, session, args);
        move(actionName);
        session.setStatus(getCurrentState().getName());
        ucon.getDAL().saveSession(session, uconRequest);
        ucon.getPIPChain().fireAfterActionEvent(event);
        if(isFinished())
            ucon.getDAL().endSession(session);
        else 
            ucon.getDAL().saveSession(session, uconRequest);
        session.setMs(System.currentTimeMillis() - start);
        return session.toXacml3Element();
    }

    public Node tryAccess(Node accessRequest, String pepUrlString, String customId) throws Exception {
        log.info("pepUrl={}, customId={}", pepUrlString, customId);
        long start = System.currentTimeMillis();
        try {
            if (customId != null) {
                ucon.getDAL().getSessionByCustomId(customId);
                throw new RuntimeException("session " + customId + " already exists!");
            }
        } catch (NoResultException e) {
            // pass
        }
        URL pepUrl = new URL(pepUrlString);
        log.debug("xacml RAW request: {}", DomUtils.toString(accessRequest));
        UconRequest uconRequest = new UconRequest((Document) accessRequest);
        log.debug("xacml request BEFORE enrichment: {}", DomUtils.toString(uconRequest.toElement()));

        // First enrich the request by calling the PIPs
        ucon.getPIPChain().fireEvent(new Event(Event.EventType.beforeTryAccess, uconRequest));
        // Now send the enriched request to the PDP
        // UUID is attributed by the dal, as well as customId if not given.

        //Document responseDocument = pdpPool[UConInterface.PolicyEnum.PRE.ordinal()].access(uconRequest);
        //UconSession session = new UconSession(responseDocument);
        session.setCustomId(customId);
        session.setPepUrl(pepUrlString);
        session.setUconUrl(ucon.myUrl);
        session.setStatus(session.getDecision() == PepResponse.DecisionEnum.Permit
                ? Status.TRY : Status.REJECTED);
        ucon.getPIPChain().fireEvent(new Event(Event.EventType.afterTryAccess, uconRequest, session));
        if (session.getDecision() == PepResponse.DecisionEnum.Permit) {
            UconSession uconSession = ucon.getDAL().startSession(session, uconRequest);
            session.setUuid(uconSession.getUuid());
            session.setCustomId(uconSession.getCustomId());
            assert (session.getUuid() != null && session.getUuid().length() > 0);
            assert (session.getCustomId() != null && session.getCustomId().length() > 0);
            assert (session.getStatus() == Status.TRY);
        }
        session.setMs(System.currentTimeMillis() - start);
        return session.toXacml3Element();
    }
}