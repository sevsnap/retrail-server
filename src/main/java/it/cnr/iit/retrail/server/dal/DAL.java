/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.dal;

import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.commons.StateType;
import it.cnr.iit.retrail.commons.impl.PepAttribute;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.persistence.config.PersistenceUnitProperties;

/**
 *
 * @author oneadmin
 */
public class DAL implements DALInterface {

    //Create entity manager, this step will connect to database, please check 
    //JDBC driver on classpath, jdbc URL, jdbc driver name on persistence.xml
    private static final String PERSISTENCE_UNIT_NAME = "retrail";
    private static EntityManagerFactory factory;

    protected static final Logger log = LoggerFactory.getLogger(DAL.class);
    final private ThreadLocal entityManager;
    final private ThreadLocal entityManagerCount;

    private static DAL instance;

    public static DAL getInstance() {
        if (instance == null) {
            if (factory == null) {
                Map<String, String> properties = new HashMap<>();
                properties.put(PersistenceUnitProperties.WEAVING, "dynamic");
          
                String ddlGeneration = properties.getOrDefault(PersistenceUnitProperties.DDL_GENERATION, "create-tables");
                properties.put(PersistenceUnitProperties.DDL_GENERATION, ddlGeneration);
                properties.put(PersistenceUnitProperties.DDL_GENERATION_MODE, "database");
                properties.put(PersistenceUnitProperties.DDL_GENERATION_INDEX_FOREIGN_KEYS, "true");
                properties.put(PersistenceUnitProperties.SESSION_CUSTOMIZER, UUIDSequence.class.getCanonicalName());
                factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, properties);
            }
            instance = new DAL();
        }
        return instance;
    }

    private EntityManager getEm() {
        EntityManager em = (EntityManager) entityManager.get();
        // clearing the entity manager with no transaction is fundamental to 
        // avoid stale objects that may have been updated earlier!
        if(!em.isJoinedToTransaction())
            em.clear();
        return em;
    }
    
    @Override
    public void begin() {
        EntityManager em = (EntityManager) entityManager.get();
        int count = (int) entityManagerCount.get();
        // clearing the entity manager is fundamental to avoid stale objects that
        // may have been updated by other threads!
        if(count == 0) {
            em.clear();
            em.getTransaction().begin();    
            entityManagerCount.set(count+1);
        }
        
    }

    @Override
    public boolean hasBegun() {
        int count = (int) entityManagerCount.get();
        return count > 0;
    }
    
    @Override
    public void commit() {
        EntityManager em = (EntityManager) entityManager.get();
        int count = (int) entityManagerCount.get()-1;
        entityManagerCount.set(count);
        em.getTransaction().commit();
        if(count > 0)
            em.getTransaction().begin();
    }

    @Override
    public void rollback() {
        EntityManager em = (EntityManager) entityManager.get();
        em.getTransaction().rollback();
        entityManagerCount.set(0);
    }

    public void debugDump() {
        log.info("current open sessions:");
        Collection<UconSession> q = listSessions();
        for (UconSession s : q) {
            log.info("\t" + s);
            for (UconAttribute a : s.attributes) {
                log.info("\t\t" + a);
            }
        }
        //System.out.println("DAL: all attributes:");
        //for (UconAttribute a: listAttributesByFactoryUUID())
        //        System.out.println("\t" + a);
    }

    public void debugDumpAttributes(Collection<UconAttribute> al) {
        log.info("debugging attributes");
        for (UconAttribute a : al) {
            UconAttribute parent = (UconAttribute) a.getParent();
            log.info("\t{} (parent: {})", a, parent == null ? null : parent.getRowId());
            log.info("\t\t{}", a.session);
        }
    }
    
    public void debugDumpAttributes(UconRequest al) {
        log.info("debugging attributes");
        for (PepAttributeInterface aa : al) {
            UconAttribute a = (UconAttribute) aa;
            UconAttribute parent = (UconAttribute) a.getParent();
            log.info("\t{} (parent: {})", a, parent == null ? null : parent.getRowId());
            log.info("\t\t{}", a.session);
        }
    }

    private DAL() {
        log.info("creating Data Access Layer");

        this.entityManager = new ThreadLocal() {
            @Override
            protected synchronized Object initialValue() {
                try {
                    return factory.createEntityManager();
                } catch (Exception e) {
                    log.error("while creating thread local entity manager: {}", e);
                    throw e;
                }
            }
        };
        this.entityManagerCount = new ThreadLocal() {
            @Override
            protected synchronized Object initialValue() {
                try {
                    return 0;
                } catch (Exception e) {
                    log.error("while creating thread local entity manager count: {}", e);
                    throw e;
                }
            }
        };        
        debugDump();
    }

    @Override
    public Collection<UconSession> listSessions() {
        EntityManager em = getEm();
        TypedQuery<UconSession> q = em.createQuery(
                "select s from UconSession s",
                UconSession.class);
        Collection<UconSession> sessions = q.getResultList();
        return sessions;
    }

    @Override
    public Collection<UconSession> listSessions(StateType stateType) {
        EntityManager em = getEm();
        TypedQuery<UconSession> q = em.createQuery(
                "select s from UconSession s where s.stateType = :stateType",
                UconSession.class)
                .setParameter("stateType", stateType);
        Collection<UconSession> sessions = q.getResultList();
        return sessions;
    }

    @Override
    public Collection<UconSession> listSessions(String stateName) {
        EntityManager em = getEm();
        TypedQuery<UconSession> q = em.createQuery(
                "select s from UconSession s where s.stateName = :stateName",
                UconSession.class)
                .setParameter("stateName", stateName);
        Collection<UconSession> sessions = q.getResultList();
        return sessions;
    }
    
    @Override
    public Collection<UconSession> listSessions(Date lastSeenBefore) {
        EntityManager em = getEm();
        TypedQuery<UconSession> q = em.createQuery(
                "select s from UconSession s where s.lastSeen < :lastSeen",
                UconSession.class)
                .setParameter("lastSeen", lastSeenBefore);
        Collection<UconSession> sessions = q.getResultList();
        return sessions;
    }

    @Override
    public Collection<UconSession> listSessions(URL pepUrl) {
        EntityManager em = getEm();
        String url = pepUrl.toString();
        TypedQuery<UconSession> q = em.createQuery(
                "select s from UconSession s where s.pepUrl = :pepUrl",
                UconSession.class)
                .setParameter("pepUrl", url);
        Collection<UconSession> sessions = q.getResultList();
        return sessions;
    }

    @Override
    public Collection<UconAttribute> listAttributes(URL pepUrl) {
        EntityManager em = getEm();
        TypedQuery<UconAttribute> q = em.createQuery(
                "select distinct a from UconAttribute a, UconSession s where s.pepUrl = :pepUrl and s = a.session",
                UconAttribute.class)
                .setParameter("pepUrl", pepUrl.toString());
        Collection<UconAttribute> attributes = q.getResultList();
        return attributes;
    }

    @Override
    public Collection<UconAttribute> listAttributes() {
        EntityManager em = getEm();
        TypedQuery<UconAttribute> q = em.createQuery(
                "select a from UconAttribute a",
                UconAttribute.class);
        Collection<UconAttribute> attributes = q.getResultList();
        return attributes;
    }

    @Override
    public Collection<UconAttribute> listAttributes(String category, String id) {
        EntityManager em = getEm();
        TypedQuery<UconAttribute> q = em.createQuery(
                "select a from UconAttribute a where a.category = :category and a.id = :id",
                UconAttribute.class)
                .setParameter("category", category)
                .setParameter("id", id);
        Collection<UconAttribute> attributes = q.getResultList();
        return attributes;
    }

    @Override
    public Collection<PepAttributeInterface> listManagedAttributes(String factory) {
        EntityManager em = getEm();
        TypedQuery<PepAttributeInterface> q = em.createQuery(
                "select a from UconAttribute a where a.factory = :factory and a.expires is null",
                PepAttributeInterface.class)
                .setParameter("factory", factory);
        Collection<PepAttributeInterface> attributes = q.getResultList();
        return attributes;
    }

    @Override
    public Collection<PepAttributeInterface> listManagedAttributes(String factory, StateType stateType) {
        EntityManager em = getEm();
        TypedQuery<PepAttributeInterface> q = em.createQuery(
                "select a from UconAttribute a where a.factory = :factory and a.expires is null and a.session is not null and a.session.stateType = :stateType",
                PepAttributeInterface.class)
                .setParameter("factory", factory)
                .setParameter("stateType", stateType);
        Collection<PepAttributeInterface> attributes = q.getResultList();
        return attributes;
    }

    @Override
    public Collection<PepAttributeInterface> listUnmanagedAttributes(String factory) {
        EntityManager em = getEm();
        TypedQuery<PepAttributeInterface> q = em.createQuery(
                "select a from UconAttribute a where a.factory = :factory and a.expires is not null",
                PepAttributeInterface.class)
                .setParameter("factory", factory);
        Collection<PepAttributeInterface> attributes = q.getResultList();
        return attributes;
    }

    private UconSession saveSession(EntityManager em, UconSession uconSession, UconRequest uconRequest) {
        // Store request's attributes to the database
        // put parents to front since we have a CASCADE towards children
        LinkedList<UconAttribute> rootsFirst = new LinkedList<>();
        for (PepAttributeInterface pepAttribute : uconRequest) {
            UconAttribute uconAttribute = (UconAttribute) pepAttribute;
            if (uconAttribute.getParent() == null) {
                rootsFirst.addFirst((UconAttribute) pepAttribute);
            } else {
                rootsFirst.addLast((UconAttribute) pepAttribute);
            }
        }
        // Then, process all attributes (persist, or merge).
        // Also, keep merged the original uconRequest for consistency.
        uconSession = em.merge(uconSession);
        uconSession.attributes.clear();
        for (UconAttribute uconAttribute : rootsFirst) {
            if (uconAttribute.getRowId() == null) {
                em.persist(uconAttribute);
            }
            int index = 0;
            while (uconAttribute != uconRequest.get(index)) {
                index++;
            }
            uconRequest.set(index, uconAttribute);
            uconAttribute = em.merge(uconAttribute);
            // XXX FLUSH IS VERY SLOW!!
            //em.flush();
            //assert(uconAttribute.getRowId() != null);
            // keep shared attributes out
            if (!uconAttribute.shared && !uconSession.attributes.contains(uconAttribute)) {
                uconAttribute.session = uconSession;
                uconSession.attributes.add(uconAttribute);
            }
        }
        uconSession = em.merge(uconSession);
        return uconSession;
    }

    @Override
    public UconSession startSession(UconSession uconSession, UconRequest uconRequest) throws Exception {
        // Store request's attributes to the database
        EntityManager em = getEm();
        try {
            em.persist(uconSession);
            uconSession = saveSession(em, uconSession, uconRequest);
        } catch (Exception e) {
            log.error("while creating dal session: {}", e);
            throw e;
        }
        return uconSession;
    }

    @Override
    public UconSession saveSession(UconSession uconSession, UconRequest uconRequest) {
        // Store request's attributes to the database
        log.debug("saving " + uconSession);
        EntityManager em = getEm();
        try {
            uconSession = saveSession(em, uconSession, uconRequest);
        } catch (Exception e) {
            log.error("while saving dal session: {}", e.getMessage());
            throw e;
        }
        return uconSession;
    }

    @Override
    public UconSession getSession(String uuid, URL uconUrl) {
        // TODO use custom generated value
        EntityManager em = getEm();
        UconSession uconSession = em.find(UconSession.class, uuid);
        if (uconSession == null) {
            throw new RuntimeException("Session with uuid=" + uuid + " is unknown");
        }
        uconSession.setUconUrl(uconUrl);
        return uconSession;
    }

    @Deprecated
    @Override
    public UconSession getSessionByCustomId(String customId) {
        EntityManager em = getEm();
        TypedQuery<UconSession> q = em.createQuery(
                "select s from UconSession s where s.customId = :id",
                UconSession.class)
                .setParameter("id", customId);
        UconSession uconSession = q.getSingleResult();
        return uconSession;
    }

    @Override
    public UconAttribute getSharedAttribute(String category, String id) {
        EntityManager em = getEm();
        UconAttribute uconAttribute;
        try {
            TypedQuery<UconAttribute> q = em.createQuery(
                    "select a from UconAttribute a where a.category = :category and a.id = :id and a.shared = true",
                    UconAttribute.class)
                    .setParameter("category", category)
                    .setParameter("id", id);
            uconAttribute = q.getSingleResult();
        } catch (NoResultException e) {
            uconAttribute = null;
        }
        return uconAttribute;
    }

    @Override
    public Collection<UconAttribute> listSharedAttributes() {
        EntityManager em = getEm();
        Collection<UconAttribute> uconAttributes;
        TypedQuery<UconAttribute> q = em.createQuery(
                "select a from UconAttribute a where a.shared = true",
                UconAttribute.class);
        uconAttributes = q.getResultList();
        return uconAttributes;
    }

    @Override
    public void removeAttributesByFactory(String factory) {
        log.debug("removing all attributes for " + factory);
        EntityManager em = getEm();
        try {
            TypedQuery<UconAttribute> q = em.createQuery(
                    "select a from UconAttribute a where a.factory = :factory",
                    UconAttribute.class)
                    .setParameter("factory", factory);
            for (UconAttribute a : q.getResultList()) {
                if (a.parent != null) {
                    a.parent.children.remove(a);
                    a.parent = null;
                }
                if(a.session != null) {
                    a.session.attributes.remove(a);
                    a.session = null;
                }
                em.remove(a);
            }
        } catch (Exception e) {
            log.error("while removing attributes: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public UconSession endSession(UconSession uconSession) {
        log.info("ending " + uconSession);
        EntityManager em = getEm();
        try {
            uconSession = em.merge(uconSession);
            // first remove possible parent links to avoid constraint
            // problems on session removal.
            Iterator<UconAttribute> i = uconSession.attributes.iterator();
            while (i.hasNext()) {
                UconAttribute a = i.next();
                a = em.merge(a);
                a.parent = null;
                //a.children.clear();
                a.session = null;
                a = em.merge(a);
                //em.remove(a);
                //i.remove();
            }
            //uconSession.attributes.clear();
            em.remove(uconSession);
            em.flush();
            em.clear();
        } catch (Exception e) {
            log.error("while ending session {} with {} attributes: {}", uconSession, uconSession.attributes.size(), e);
            throw e;
        }
        return uconSession;
    }

    @Override
    public Object save(Object o) {
        EntityManager em = getEm();
        try {
            o = em.merge(o);
        } catch (Exception e) {
            log.error("while saving {}: {}", e.getMessage());
            throw e;
        }
        // warning: returned object returned has transients reset.
        return o;
    }

    @Override
    public UconAttribute newPrivateAttribute(String id, String type, String value, String issuer, UconAttribute parent, String uuid) {
        for (UconAttribute child : parent.getChildren()) {
            if (child.getId().equals(id)) {
                child.setType(type);
                child.setValue(value);
                child.setIssuer(issuer);
                return child;
            }
        }
        PepAttribute a = new PepAttribute(id, type, value, issuer, parent.getCategory(), uuid);
        UconAttribute u = UconAttribute.newInstance(a);
        // DON'T SAVE ME!
        u.parent = parent;
        parent.children.add(u);
        u.session = parent.session;
        parent.session.attributes.add(u);        
        u = (UconAttribute) save(u);
        return u;
    }

    public UconAttribute newPrivateAttribute(String id, String type, String value, String issuer, String category, UconSession session, String uuid) {
        for (UconAttribute child : session.getAttributes()) {
            if (child.getId().equals(id)) {
                child.setType(type);
                child.setValue(value);
                child.setIssuer(issuer);
                return child;
            }
        }
        PepAttribute a = new PepAttribute(id, type, value, issuer, category, uuid);
        UconAttribute u = UconAttribute.newInstance(a);
        u.session = session;
        session.attributes.add(u);
        u = (UconAttribute) save(u);
        return u;
    }
    
    @Override
    public UconAttribute newSharedAttribute(String id, String type, String value, String issuer, String category, String uuid) {
        UconAttribute u = getSharedAttribute(category, id);
        if (u == null) {
            PepAttribute a = new PepAttribute(id, type, value, issuer, category, uuid);
            u = UconAttribute.newInstance(a);
            u.shared = true;
            getEm().persist(u);
            getEm().flush();
            u = getEm().merge(u);
        } else {
            u.setType(type);
            u.setValue(value);
            u.setIssuer(issuer);
        }
        assert (u.getFactory() != null); 
        assert(u.getRowId() != null);
        return u;
    }

    @Override
    public UconRequest rebuildUconRequest(UconSession uconSession) {
        log.debug("" + uconSession);
        UconRequest accessRequest = new UconRequest();
        // Add private attributes
        for (UconAttribute a : uconSession.attributes) {
            accessRequest.add(a);
        }
        // Add shared attributes
        for (UconAttribute a : listSharedAttributes()) {
            accessRequest.add(a);
        }
        return accessRequest;
    }

}
