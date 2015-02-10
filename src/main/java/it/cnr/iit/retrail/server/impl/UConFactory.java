/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.impl;

import it.cnr.iit.retrail.server.UConProtocol;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author oneadmin
 */
public class UConFactory {
    public static final String defaultUrlString = "http://localhost:8080";
    protected static Map<String,UCon> uconMap = new HashMap<>();
    protected static UConProtocol singleton;
    
    static public synchronized UCon getInstance(URL url) throws Exception {
        UCon ucon = uconMap.get(url.toString());
        if(ucon == null) {
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
       
    static public synchronized UCon getInstance() throws Exception {
        return getInstance(new URL(defaultUrlString));
    }

    @Deprecated
    public static synchronized UConProtocol getProtocolInstance() throws Exception {
        if(singleton == null)
            getInstance();
        return singleton;
    }
}
