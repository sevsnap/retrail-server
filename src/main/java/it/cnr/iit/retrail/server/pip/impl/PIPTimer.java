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
import java.util.Date;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kicco
 */
public class PIPTimer extends StandAlonePIP {

    protected double resolution = 1.0;
    protected StateType forStateType = StateType.ONGOING;
    private String attributeId = "timer";

    public PIPTimer() {
        super();
        this.log = LoggerFactory.getLogger(PIPTimer.class);
    }

    @Override
    public void fireBeforeActionEvent(ActionEvent e) {
        if (e.originState.getType() != forStateType && e.targetState.getType() == forStateType) {
            PepAttributeInterface subject = e.request.getAttributes(PepAttribute.CATEGORIES.SUBJECT, PepAttribute.IDS.SUBJECT).iterator().next();
            PepAttributeInterface a = newPrivateAttribute(getAttributeId(), PepAttribute.DATATYPES.DOUBLE, 0.0, subject);
            log.warn("setting {} because target status = {}", a, e.targetState.getType());
            e.request.replace(a);
        }
    }

    @Override
    public void fireAfterActionEvent(ActionEvent e) {
        if (e.originState.getType() == forStateType && e.targetState.getType() != forStateType) {
            PepAttributeInterface subject = e.request.getAttributes(PepAttribute.CATEGORIES.SUBJECT, PepAttribute.IDS.SUBJECT).iterator().next();
            PepAttributeInterface a = newPrivateAttribute(getAttributeId(), PepAttribute.DATATYPES.DOUBLE, 0.0, subject);
            log.warn("removing {} because target status = {}", a, e.targetState.getType());
            a.setExpires(new Date());
            e.request.replace(a);
        }
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

    public String getAttributeId() {
        return attributeId;
    }

    public void setAttributeId(String attributeId) {
        this.attributeId = attributeId;
    }

    @Override
    public void run() {
        boolean interrupted = false;
        while (!interrupted) {
            try {
                Thread.sleep((int) (1000 * resolution));
                for (PepAttributeInterface a : listManagedAttributes(forStateType)) {
                    UconAttribute u = (UconAttribute) a;
                    Double elapsed = Double.parseDouble(a.getValue());
                    elapsed += resolution;
                    a.setValue(elapsed.toString());
                    log.debug("awaken {}", a);
                    notifyChanges(a);
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
