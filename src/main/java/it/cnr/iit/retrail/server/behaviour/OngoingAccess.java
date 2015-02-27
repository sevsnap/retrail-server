/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.iit.retrail.server.behaviour;

import it.cnr.iit.retrail.commons.automata.StateInterface;
import it.cnr.iit.retrail.server.dal.UconRequest;
import it.cnr.iit.retrail.server.dal.UconSession;
import it.cnr.iit.retrail.server.impl.UCon;

/**
 *
 * @author kicco
 */
public class OngoingAccess extends PolicyDrivenAction {
    public static final String name = "ongoingAccess";

    public OngoingAccess(StateInterface sourceState, StateInterface targetState, StateInterface targetFailState, UCon ucon) {
        super(sourceState, targetState, targetFailState, name, ucon);
    }
    
    @Override
    public UconSession onFail(UconRequest uconRequest, UconSession uconSession, Object[] args) {
        // must revoke access. the real mass revocation is done by the system,
        // we simply must report the revoked session here.
        return uconSession;
    }
    
    @Override
    public UconSession onPermit(UconRequest uconRequest, UconSession uconSession, Object[] args) {
        // must send obligations if any. Since runObligations is done by the
        // system, we simply report the session if any obligation is present.
        return uconSession.getObligations().size() > 0? uconSession : null;
    }
    
}
