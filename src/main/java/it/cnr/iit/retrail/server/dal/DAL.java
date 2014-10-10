/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.dal;

import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.commons.Status;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author oneadmin
 */
public class DAL implements DALInterface {

    //Create entity manager, this step will connect to database, please check 
    //JDBC driver on classpath, jdbc URL, jdbc driver name on persistence.xml
    private static final String PERSISTENCE_UNIT_NAME = "retrail";
    private static final EntityManagerFactory factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
    protected static final Logger log = LoggerFactory.getLogger(DAL.class);
    final private ThreadLocal entityManager;

    private EntityManager getEntityManager() {
        EntityManager em = (EntityManager) entityManager.get();
        // clearing the entity manager is fundamental to avoid stale objects that
        // may have been updated by other threads!
        em.clear();
        return em;
    }

    private static DAL instance;

    public static DAL getInstance() {
        if (instance == null) {
            instance = new DAL();
        }
        return instance;
    }

    public void debugDump() {
        log.info("current open sessions:");
        Collection<UconSession> q = listSessions();
        for (UconSession s : q) {
            log.info("\t" + s);
            for (UconAttribute a : s.getAttributes()) {
                log.info("\t\t" + a);
            }
        }
        //System.out.println("DAL: all attributes:");
        //for (UconAttribute a: listAttributesByFactoryUUID())
        //        System.out.println("\t" + a);
    }

    public void debugDumpAttributes(Collection<UconAttribute> al) {
        for (UconAttribute a : al) {
            log.info("\t{} (parent: {})", a, ((UconAttribute) a.getParent()).getRowId());
            for (UconSession s : a.getSessions()) {
                log.info("\t\t{}", s);
            }
        }
    }

    private DAL() {
        log.info("creating Data Access Layer");
        this.entityManager = new ThreadLocal() {
            @Override
            protected synchronized Object initialValue() {
                return factory.createEntityManager();
            }
        };
        debugDump();
    }

    @Override
    public Collection<UconSession> listSessions() {
        EntityManager em = getEntityManager();
        TypedQuery<UconSession> q = em.createQuery(
                "select s from UconSession s",
                UconSession.class);
        Collection<UconSession> sessions = q.getResultList();
        return sessions;
    }

    @Override
    public Collection<UconSession> listSessions(Status status) {
        EntityManager em = getEntityManager();
        TypedQuery<UconSession> q = em.createQuery(
                "select s from UconSession s where s.status = :status",
                UconSession.class)
                .setParameter("status", status);
        Collection<UconSession> sessions = q.getResultList();
        return sessions;
    }

    @Override
    public Collection<UconSession> listSessions(Date lastSeenBefore) {
        EntityManager em = getEntityManager();
        TypedQuery<UconSession> q = em.createQuery(
                "select s from UconSession s where s.lastSeen < :lastSeen",
                UconSession.class)
                .setParameter("lastSeen", lastSeenBefore);
        Collection<UconSession> sessions = q.getResultList();
        return sessions;
    }

    @Override
    public Collection<UconSession> listSessions(URL pepUrl) {
        String url = pepUrl.toString();
        EntityManager em = getEntityManager();
        TypedQuery<UconSession> q = em.createQuery(
                "select s from UconSession s where s.pepUrl = :pepUrl",
                UconSession.class)
                .setParameter("pepUrl", url);
        Collection<UconSession> sessions = q.getResultList();
        return sessions;
    }

    @Override
    public Collection<UconSession> listOutdatedSessions() {
        EntityManager em = getEntityManager();
        TypedQuery<UconSession> q = em.createQuery(
                "select distinct s from UconSession s, UconAttribute a where s.status = :status and s member of a.sessions and a.expires < :now",
                UconSession.class)
                .setParameter("status", Status.ONGOING)
                .setParameter("now", new Date());
        Collection<UconSession> involvedSessions = q.getResultList();
        return involvedSessions;
    }

    @Override
    public Collection<UconAttribute> listAttributes(URL pepUrl) {
        EntityManager em = getEntityManager();
        TypedQuery<UconAttribute> q = em.createQuery(
                "select distinct a from UconAttribute a, UconSession s where s.pepUrl = :pepUrl and s member of a.sessions",
                UconAttribute.class)
                .setParameter("pepUrl", pepUrl.toString());
        Collection<UconAttribute> attributes = q.getResultList();
        return attributes;
    }

    @Override
    public Collection<UconAttribute> listAttributes() {
        EntityManager em = getEntityManager();
        TypedQuery<UconAttribute> q = em.createQuery(
                "select a from UconAttribute a",
                UconAttribute.class);
        Collection<UconAttribute> attributes = q.getResultList();
        return attributes;
    }

    @Override
    public Collection<PepAttributeInterface> listManagedAttributes(String factory) {
        log.debug("begin");
        EntityManager em = getEntityManager();
        TypedQuery<PepAttributeInterface> q = em.createQuery(
                "select a from UconAttribute a where a.factory = :factory and a.expires is null",
                PepAttributeInterface.class)
                .setParameter("factory", factory);
        Collection<PepAttributeInterface> attributes = q.getResultList();
        //debugDump();
        log.debug("end");
        return attributes;
    }

    @Override
    public Collection<PepAttributeInterface> listUnmanagedAttributes(String factory) {
        log.debug("begin");
        EntityManager em = getEntityManager();
        TypedQuery<PepAttributeInterface> q = em.createQuery(
                "select a from UconAttribute a where a.factory = :factory and a.expires is not null",
                PepAttributeInterface.class)
                .setParameter("factory", factory);
        Collection<PepAttributeInterface> attributes = q.getResultList();
        //debugDump();
        log.debug("end");
        return attributes;
    }

    @Override
    public UconSession startSession(UconSession uconSession, UconRequest uconRequest) throws Exception {
        // Store request's attributes to the database
        EntityManager em = getEntityManager();
        //start transaction with method begin()
        em.getTransaction().begin();
        try {
            em.persist(uconSession);
            uconSession = em.merge(uconSession);
            if (uconSession.getCustomId() == null || uconSession.getCustomId().length() == 0) {
                uconSession.setCustomId(uconSession.getUuid());
                uconSession = em.merge(uconSession);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        }
        return saveSession(uconSession, uconRequest);
    }

    @Override
    public UconSession saveSession(UconSession uconSession, UconRequest uconRequest) {
        // Store request's attributes to the database
        log.debug("begin " + uconSession);
        EntityManager em = getEntityManager();
        //start transaction with method begin()
        em.getTransaction().begin();
        try {
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
            uconSession.getAttributes().clear();
            for (UconAttribute uconAttribute : rootsFirst) {
                if (uconAttribute.getRowId() == null) {
                    em.persist(uconAttribute);
                    //em.flush();
                    //log.error("CREATED ATTRIBUTE {}", uconAttribute);
                } 
                int index = 0;
                while(uconAttribute != uconRequest.get(index)) 
                    index++;
                uconAttribute = em.merge(uconAttribute);
                uconRequest.set(index, uconAttribute);

                uconAttribute.getSessions().remove(uconSession);
                uconSession.addAttribute(uconAttribute);
                //log.error("SET ATTRIBUTE {} TO {}", uconAttribute, uconSession);

            }
            //log.error("COMMITTING {}", uconSession);
            em.getTransaction().commit();
        } catch (Exception e) {
            log.error("*** Unexpected exception: {}", e.getMessage());
            em.getTransaction().rollback();
            throw e;
        }
        log.debug("end " + uconSession);
        return uconSession;
    }

    @Override
    public UconSession getSession(String uuid, URL uconUrl) {
        // TODO use custom generated value
        EntityManager em = getEntityManager();
        UconSession uconSession = em.find(UconSession.class, uuid);
        if (uconSession != null) {
            uconSession.setUconUrl(uconUrl);
        }
        return uconSession;
    }

    @Deprecated
    @Override
    public UconSession getSessionByCustomId(String customId) {
        EntityManager em = getEntityManager();
        TypedQuery<UconSession> q = em.createQuery(
                "select s from UconSession s where s.customId = :id",
                UconSession.class)
                .setParameter("id", customId);
        UconSession uconSession = q.getSingleResult();
        return uconSession;
    }

    @Override
    public UconAttribute getSharedAttribute(String category, String id) {
        EntityManager em = getEntityManager();
        UconAttribute uconAttribute;
        log.error("***** CATEGORY {} ID {}", category, id);
        try {
            TypedQuery<UconAttribute> q = em.createQuery(
                    "select a from UconAttribute a where a.category = :category and a.id = :id and a.parent is null",
                    UconAttribute.class)
                    .setParameter("category", category)
                    .setParameter("id", id);
            uconAttribute = q.getSingleResult();
        } catch (NoResultException e) {
            uconAttribute = null;
        }
        return uconAttribute;
    }

    private void removeAttributes(EntityManager em, UconSession uconSession) {
        log.info("removing all attributes for " + uconSession);
        while (uconSession.getAttributes().size() > 0) {
            UconAttribute a = uconSession.getAttributes().iterator().next();
            log.info("removing " + a);
            a.setParent(null);
            uconSession.removeAttribute(a);
            if (a.getSessions().isEmpty()) {
                em.remove(a);
            }
        }
        uconSession.getAttributes().clear();
    }

    @Override
    public void removeAttributesByFactory(String factory) {
        log.debug("removing all attributes for " + factory);
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        try {
            TypedQuery<UconAttribute> q = em.createQuery(
                    "select a from UconAttribute a where a.factory = :factory",
                    UconAttribute.class)
                    .setParameter("factory", factory);
            for (UconAttribute a : q.getResultList()) {
                Collection<UconSession> l = new ArrayList<>(a.getSessions());
                a.setParent(null);
                for (UconSession s : l) {
                    s.removeAttribute(a);
                }
                em.remove(a);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            log.error("*** unexpected exception: {}", e.getMessage());
            em.getTransaction().rollback();
            throw e;
        }
    }

    @Override
    public UconSession revokeSession(UconSession uconSession) {
        log.info("revoking " + uconSession);
        EntityManager em = getEntityManager();
        //start transaction with method begin()
        em.getTransaction().begin();
        try {
            uconSession = em.merge(uconSession);
            removeAttributes(em, uconSession);
            uconSession.setStatus(Status.REVOKED);
            uconSession = em.merge(uconSession);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        }
        //debugDump();
        return uconSession;
    }

    @Override
    public UconSession endSession(UconSession uconSession) {
        log.info("ending " + uconSession);
        EntityManager em = getEntityManager();
        //start transaction with method begin()
        em.getTransaction().begin();
        try {
            if (uconSession != null) {
                uconSession = em.merge(uconSession);
                removeAttributes(em, uconSession);
                em.remove(uconSession);
            } else {
                log.error("cannot find {}", uconSession);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            log.error("unexpected exception when ending {} ({} attributes): {}", uconSession, uconSession.getAttributes().size(), e);
            em.getTransaction().rollback();
            throw e;
        }
        //debugDump();
        log.info("ended " + uconSession);
        return uconSession;
    }

    @Override
    public Object save(Object o) {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        try {
            o = em.merge(o);
            em.getTransaction().commit();
        } catch (Exception e) {
            log.error(e.getMessage());
            em.getTransaction().rollback();
            throw e;
        }
        // warning: returned object returned has transients reset.
        return o;
    }
}
