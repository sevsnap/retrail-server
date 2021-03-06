/*
 * CNR - IIT
 * Coded by: 2015 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.pip.impl;

import it.cnr.iit.retrail.commons.DomUtils;
import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.commons.PepRequestInterface;
import it.cnr.iit.retrail.commons.PepSessionInterface;
import it.cnr.iit.retrail.commons.StateType;
import it.cnr.iit.retrail.server.UConInterface;
import it.cnr.iit.retrail.server.impl.UCon;
import it.cnr.iit.retrail.server.pip.ActionEvent;
import it.cnr.iit.retrail.server.pip.PIPChainInterface;
import it.cnr.iit.retrail.server.pip.PIPInterface;
import it.cnr.iit.retrail.server.pip.SystemEvent;
import static it.cnr.iit.retrail.server.pip.impl.PIP.dal;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author oneadmin
 */
public class PIPChain extends ArrayList<PIPInterface> implements PIPChainInterface {

    static final org.slf4j.Logger log = LoggerFactory.getLogger(PIPChain.class);
    private final Map<String, PIPInterface> pipNameToInstanceMap = new HashMap<>();
    private UConInterface ucon;
    private String issuer = UCon.uri;
    
    @Override
    public boolean isInited() {
        return ucon != null;
    }

    @Override
    public final synchronized boolean add(PIPInterface p) {
        String uuid = p.getUuid();
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
        String uuid = p.getUuid();
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
    public String getUuid() {
        throw new UnsupportedOperationException("Not supported for a chain.");
    }

    @Override
    public void setUuid(String uuid) {
        throw new UnsupportedOperationException("Not supported for a chain.");
    }

    @Override
    public void init(UConInterface uconInterface) {
        dal.begin();
        try {
            if (!isInited()) {
                ucon = uconInterface;
                for (PIPInterface p : this) {
                    p.init(ucon);
                }
            }
            dal.commit();
        } catch (Exception e) {
            dal.rollback();
            log.error("while initializing PIPchain: {}", e);
            throw e;
        }
    }

    private void lockIfNeeded() {
        log.debug("called");
    }

    private void unlockIfNeeded() {
        log.debug("called");
    }

    @Override
    public void fireBeforeActionEvent(ActionEvent e) throws Exception {
        lockIfNeeded();
        for (PIPInterface p : this) {
            try {
                p.fireBeforeActionEvent(e);
            } catch (Exception ex) {
                log.error("PIP {} canceled {} by throwing exception: {}", p, e, ex);
                unlockIfNeeded();
                throw ex;
            }
        }
    }

    @Override
    public void fireAfterActionEvent(ActionEvent e) {
        for (PIPInterface p : this) {
            try {
                p.fireAfterActionEvent(e);
            } catch (Exception ex) {
                log.error("PIP {}, ignoring exception: {}", p, ex);
            }
        }
        unlockIfNeeded();
    }

    @Override
    public void fireSystemEvent(SystemEvent e) {
        switch (e.type) {
            case beforeApplyChanges:

                lockIfNeeded();
                for (PIPInterface p : this) {
                    p.fireSystemEvent(e);
                }
                break;
            case afterApplyChanges:
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
    public PepAttributeInterface newSharedAttribute(String id, String type, Object value, String category) {
        throw new UnsupportedOperationException("Not supported for a chain.");
    }

    @Override
    public PepAttributeInterface getSharedAttribute(String category, String id) {
        return dal.getSharedAttribute(category, id);
    }

    @Override
    public PepAttributeInterface newPrivateAttribute(String id, String type, Object value, PepAttributeInterface parent) {
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
        while (!isEmpty()) {
            remove(get(0));
        }
    }

    public PIPChain() {
        super();
    }

    public PIPChain(Element configElement) throws Exception {
        if (configElement != null) {
            NodeList nl = configElement.getElementsByTagNameNS(UCon.uri, "PIP");
            for (int i = 0; i < nl.getLength(); i++) {
                Element pipElement = (Element) nl.item(i);
                String className = pipElement.getAttributeNS(null, "class");
                log.warn("loading PIP with class: {}", className);
                Class<?> clazz = Class.forName(className);
                if (!PIPInterface.class.isAssignableFrom(clazz)) {
                    throw new RuntimeException("class " + className + " has no PIP interface and cannot be used in a PIPChain");
                }
                Constructor<?> ctor;
                try {
                    ctor = clazz.getConstructor();
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException("PIP class " + className + " does not implement a default constructor with no parameters and cannot be instanced");
                }
                PIPInterface pip = (PIPInterface) ctor.newInstance(new Object[]{});
                DomUtils.setPropertyOnObjectNS(UCon.uri, "Property", pipElement, pip);
                add(pip);
            }
            printInfo();
        }
    }

    @Override
    public PIPInterface get(String uuid) {
        PIPInterface rv = pipNameToInstanceMap.get(uuid);
        if(rv == null)
            throw new RuntimeException("PIP with uuid=" + uuid + " not found in PIPChain");
        return rv;
    } 
    
    @Override
    public final void printInfo() {
        log.info("Current PIPs:");
        for (PIPInterface pip : this) {
            log.info("\t{}", pip);
        }
    }

    @Override
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    @Override
    public String getIssuer() {
        return issuer;
    }

    @Override
    public Collection<PepAttributeInterface> listManagedAttributes(StateType stateType) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
