/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.pip.impl;

import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.commons.PepRequestInterface;
import it.cnr.iit.retrail.commons.PepSessionInterface;
import it.cnr.iit.retrail.server.UConInterface;
import it.cnr.iit.retrail.server.pip.ActionEvent;
import it.cnr.iit.retrail.server.pip.Event;
import it.cnr.iit.retrail.server.pip.PIPChainInterface;
import it.cnr.iit.retrail.server.pip.PIPInterface;
import it.cnr.iit.retrail.server.pip.SystemEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.LoggerFactory;

/**
 *
 * @author oneadmin
 */
public class PIPChain extends ArrayList<PIPInterface> implements PIPChainInterface {

    static final org.slf4j.Logger log = LoggerFactory.getLogger(PIPChain.class);
    private final Map<String, PIPInterface> pipNameToInstanceMap = new HashMap<>();
    private UConInterface ucon;

    @Override
    public boolean isInited() {
        return ucon != null;
    }

    @Override
    public synchronized boolean add(PIPInterface p) {
        String uuid = p.getUUID();
        if (!pipNameToInstanceMap.containsKey(uuid)) {
            if (isInited()) {
                p.init(ucon);
            }
            pipNameToInstanceMap.put(uuid, p);
            super.add(p);
        } else {
            throw new RuntimeException("already in filter chain: " + p);
        }
        return true;
    }

    @Override
    public synchronized boolean remove(Object pipInterface) {
        PIPInterface p = (PIPInterface) pipInterface;
        String uuid = p.getUUID();
        if (pipNameToInstanceMap.containsKey(uuid)) {
            if (isInited()) {
                p.term();
            }
            pipNameToInstanceMap.remove(uuid);
            super.remove(p);
        } else {
            log.warn("{} not in filter chain -- ignoring", p);
            return false;
        }
        return true;
    }

    @Override
    public String getUUID() {
        throw new UnsupportedOperationException("Not supported for a chain.");
    }

    @Override
    public void init(UConInterface uconInterface) {
        if (!isInited()) {
            ucon = uconInterface;
            for (PIPInterface p : this) {
                p.init(ucon);
            }
        }
    }

    private void lockIfNeeded() {
        log.debug("called");
    }

    private void unlockIfNeeded() {
        log.debug("called");
    }

    @Override
    public void fireBeforeActionEvent(ActionEvent e) {
        lockIfNeeded();
        for (PIPInterface p : this) {
            p.fireBeforeActionEvent(e);
        }
    }

    @Override
    public void fireAfterActionEvent(ActionEvent e) {
        try {
            for (PIPInterface p : this) {
                p.fireAfterActionEvent(e);
            }
        } finally {
            unlockIfNeeded();
        }
    }

    @Override
    public void fireSystemEvent(SystemEvent e) {
        switch (e.type) {
            case beforeApplyChanges:
            case beforeRevokeAccess:
            case beforeRunObligations:
                lockIfNeeded();
                for (PIPInterface p : this) {
                    p.fireSystemEvent(e);
                }
                break;
            case afterApplyChanges:
            case afterRevokeAccess:
            case afterRunObligations:
                try {
                    for (PIPInterface p : this) {
                        p.fireSystemEvent(e);
                    }
                } finally {
                    unlockIfNeeded();
                }
                break;
            default:
                throw new RuntimeException("while firing " + e + ": type " + e.type + " is unknown!");
        }
    }

    @Override
    public PepAttributeInterface newSharedAttribute(String id, String type, String value, String issuer, String category) {
        throw new UnsupportedOperationException("Not supported for a chain.");
    }

    @Override
    public PepAttributeInterface newPrivateAttribute(String id, String type, String value, String issuer, PepAttributeInterface parent) {
        throw new UnsupportedOperationException("Not supported for a chain.");
    }

    @Override
    public Collection<PepAttributeInterface> listManagedAttributes() {
        throw new UnsupportedOperationException("Not supported for a chain.");
    }

    @Override
    public Collection<PepAttributeInterface> listUnmanagedAttributes() {
        throw new UnsupportedOperationException("Not supported for a chain.");
    }

    @Override
    public void refresh(PepRequestInterface accessRequest, PepSessionInterface session) throws Exception {
        // TODO: should call only pips for changed attributes
        log.debug("refreshing request attributes");
        lockIfNeeded();
        try {
            for (PIPInterface p : this) {
                p.refresh(accessRequest, session);
            }
        } finally {
            unlockIfNeeded();
        }
    }

    @Override
    public synchronized void term() {
        while (size() > 0) {
            remove(get(0));
        }
    }

    @Override
    public void printInfo() {
        log.info("Current PIPs:");
        for (PIPInterface pip : this) {
            log.info("\t{}", pip);
        }
    }
}
