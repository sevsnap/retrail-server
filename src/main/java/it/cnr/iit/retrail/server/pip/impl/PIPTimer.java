/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.pip.impl;

import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.commons.StateType;
import it.cnr.iit.retrail.commons.impl.PepAttribute;
import it.cnr.iit.retrail.server.dal.UconAttribute;
import it.cnr.iit.retrail.server.dal.UconSession;
import it.cnr.iit.retrail.server.pip.ActionEvent;
import it.cnr.iit.retrail.server.pip.impl.StandAlonePIP;
import java.util.Date;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kicco
 */
public class PIPTimer extends StandAlonePIP {

    protected int maxDuration;
    protected double resolution = 1.0;
    protected StateType forStateType = StateType.ONGOING;
    
    public PIPTimer() {
        super();
        this.log = LoggerFactory.getLogger(PIPTimer.class);
        this.maxDuration = 3600;
    }

    @Override
    public void fireBeforeActionEvent(ActionEvent e) {
        if (e.originState.getType() != forStateType && e.targetState.getType() == forStateType) {
            log.warn("setting timer attribute because target status = {}", e.targetState.getType());
            PepAttributeInterface subject = e.request.getAttributes(PepAttribute.CATEGORIES.SUBJECT, PepAttribute.IDS.SUBJECT).iterator().next();
            PepAttributeInterface a = newPrivateAttribute("timer", "http://www.w3.org/2001/XMLSchema#double", Double.toString(maxDuration), "http://localhost:8080/federation-id-prov/saml", subject);
            e.request.replace(a);
       }
    }
    
    @Override
    public void fireAfterActionEvent(ActionEvent e) {
        if (e.originState.getType() == forStateType && e.session.getStateType() != forStateType) {
            log.warn("removing timer attribute because session status = {}", e.session.getStateType());
            PepAttributeInterface subject = e.request.getAttributes(PepAttribute.CATEGORIES.SUBJECT, PepAttribute.IDS.SUBJECT).iterator().next();
            PepAttributeInterface a = newPrivateAttribute("timer", "http://www.w3.org/2001/XMLSchema#double", "0", "http://localhost:8080/federation-id-prov/saml", subject);
            a.setExpires(new Date());
            e.request.replace(a);
        }
    }
    
    public int getMaxDuration() {
        return maxDuration;
    }

    public void setMaxDuration(int maxDuration) {
        log.info("setting maxDuration = {}", maxDuration);
        this.maxDuration = maxDuration;
    }

    public double getResolution() {
        return resolution;
    }

    public void setResolution(double resolution) {
        log.info("setting resolution = {}", resolution);
        this.resolution = resolution;
    }

    public StateType getForStateType() {
        return forStateType;
    }

    public void setForStateType(StateType forStateType) {
        log.info("setting forStateType = {}", forStateType);
        this.forStateType = forStateType;
    }

    @Override
    public void run() {
        boolean interrupted = false;
        while (!interrupted) {
            try {
                Thread.sleep((int) (1000 * resolution));
                for (PepAttributeInterface a : listManagedAttributes()) {
                    UconAttribute u = (UconAttribute) a;
                    UconSession s = u.getSession();
                    if (s.getStateType() == forStateType) {
                        Double ttg = Double.parseDouble(a.getValue());
                        if (ttg > 0) {
                            ttg = Double.max(0, ttg - resolution);
                            a.setValue(ttg.toString());
                            log.debug("awaken {}", a);
                            notifyChanges(a);
                        }
                    }
                }
            } catch (InterruptedException ex) {
                log.warn("interrupted");
                interrupted = true;
            } catch (Exception ex) {
                log.error("while timer running: {}", ex);
            }
        }
        log.info("exiting");
    }

}
