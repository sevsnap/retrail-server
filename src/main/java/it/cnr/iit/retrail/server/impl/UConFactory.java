/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.impl;

import it.cnr.iit.retrail.server.UConProtocol;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.LoggerFactory;

/**
 *
 * @author oneadmin
 */
public class UConFactory {
    public static final String defaultUrlString = "http://localhost:8080";
    static final org.slf4j.Logger log = LoggerFactory.getLogger(UConFactory.class);
    protected static Map<String,UCon> uconMap = new HashMap<>();
    private static UConProtocol singleton = null;
    
    static public synchronized UCon getInstance(URL url) throws Exception {
        UCon ucon = uconMap.get(url.toString());
        if(ucon == null) {
            log.warn("creating new UCon for URL: {}", url);
            switch(url.getProtocol()) {
                case "http":
                    ucon = new UCon(url);
                    break;
                case "https":
                    ucon = new UConS(url);
                    break;
                default:
                    throw new RuntimeException("Unknown transport protocol: "+url.getProtocol());
            }
            uconMap.put(url.toString(), ucon);
            if(singleton == null)
                singleton = ucon;
        }
        return ucon;
    }
       
    static public synchronized void releaseInstance(UCon ucon) {
        if(singleton == ucon) {
             log.info("singleton instance set to null");
            singleton = null;
        }
        uconMap.remove(ucon.myUrl.toString());
    }
    
    static public synchronized UCon getInstance() throws Exception {
        return getInstance(new URL(defaultUrlString));
    }

    @Deprecated
    public static UConProtocol getProtocolInstance() throws Exception {
        if(singleton == null)
            log.warn("singleton instance is null!");
        return singleton;
    }
}
