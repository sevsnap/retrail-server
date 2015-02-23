/*
 * CNR - IIT
 * Coded by: 2014-2015 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server;

import java.util.List;
import org.w3c.dom.Node;

/**
 *
 * @author oneadmin
 */
public interface UConProtocol {

    Node echo(Node node) throws Exception;

    Node heartbeat(String pepUrl, List<String> sessionsList) throws Exception;

    // new dynamic interface
    
    Node apply(String actionName, String uuid, String customId, Object... args) throws Exception;
    
    Node applyChanges(Node xacmlAttributes, String uuid) throws Exception;
    
    @Deprecated
    Node applyChanges(Node xacmlAttributes, String uuid, String customId) throws Exception;
    
    Node tryAccess(Node accessRequest, String pepUrl) throws Exception;

    @Deprecated
    Node tryAccess(Node accessRequest, String pepUrl, String customId) throws Exception;

    // old built-in interface
    
    @Deprecated
    Node startAccess(String uuid) throws Exception;

    @Deprecated
    Node endAccess(String uuid) throws Exception;

    @Deprecated
    List<Node> endAccess(List<String> uuidList) throws Exception;

    @Deprecated
    Node assignCustomId(String uuid, String oldCustomId, String newCustomId) throws Exception;

    @Deprecated
    Node startAccess(String uuid, String customId) throws Exception;

    @Deprecated
    Node endAccess(String uuid, String customId) throws Exception;

    @Deprecated
    List<Node> endAccess(List<String> uuidList, List<String> customIdsList) throws Exception;
    
}
