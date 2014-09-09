/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package it.cnr.iit.retrail.server.db;

import it.cnr.iit.retrail.commons.PepAccessRequest;
import it.cnr.iit.retrail.commons.PepRequestAttribute;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;

/**
 *
 * @author oneadmin
 */
public class DAL {
    private static final String PERSISTENCE_UNIT_NAME = "retrail";
    private final EntityManagerFactory factory;

    private static DAL instance;
    
    public static DAL getInstance() {
        if(instance == null)
            instance = new DAL();
        return instance;
    }
    
    private void debug_dump() {
        EntityManager em = factory.createEntityManager();
        System.out.println("DAL: current open sessions:");
        TypedQuery<UconSession> q = em.createQuery("select session from UconSession session", UconSession.class);
        for(UconSession s: q.getResultList()) {
            System.out.println("DAL: "+s);
            for(Attribute a: s.getAttributes())
                System.out.println("\t"+a);
        }
        em.close();        
    }
    
    private DAL() {
        //Create entity manager, this step will connect to database, please check 
        //JDBC driver on classpath, jdbc URL, jdbc driver name on persistence.xml
        factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
        debug_dump();
    }
    
    public void startSession(PepAccessRequest accessRequest) {
        // Store request's attributes to the database
        EntityManager em = factory.createEntityManager();
        //start transaction with method begin()
        em.getTransaction().begin();
        UconSession uconSession = new UconSession();
        em.persist(uconSession);
        for(PepRequestAttribute pepAttribute: accessRequest) {
            Attribute attribute;
            TypedQuery<Attribute> q = em.createQuery(
                    "select a from Attribute a where a.id = :id and a.category = :category", 
                    Attribute.class)
                    .setParameter("category", pepAttribute.category)
                    .setParameter("id", pepAttribute.id);
            try {
                   attribute = q.getSingleResult();
            } catch(NoResultException e) {
                attribute = Attribute.newInstance(pepAttribute);
                em.persist(attribute);
            }
            uconSession.addAttribute(attribute);
        }
        em.getTransaction().commit();
        em.close();
        debug_dump();
    }

}
