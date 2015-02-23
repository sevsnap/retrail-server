/*
 * CNR - IIT
 * Coded by: 2014-2015 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.impl;

import it.cnr.iit.retrail.server.UConProtocol;
import java.util.List;
import org.w3c.dom.Node;

public class UConProtocolProxy implements UConProtocol {

    @Override
    public Node tryAccess(Node accessRequest, String pepUrl) throws Exception {
        return UConFactory.getProtocolInstance().tryAccess(accessRequest, pepUrl);
    }

    @Override
    public Node startAccess(String uuid) throws Exception {
        return UConFactory.getProtocolInstance().startAccess(uuid);
    }

    @Override
    public Node endAccess(String uuid) throws Exception {
        return UConFactory.getProtocolInstance().endAccess(uuid);
    }

    @Override
    public List<Node> endAccess(List<String> uuidList) throws Exception {
        return UConFactory.getProtocolInstance().endAccess(uuidList);
    }

    @Override
    public Node applyChanges(Node xacmlAttributes, String uuid) throws Exception {
        return UConFactory.getProtocolInstance().applyChanges(xacmlAttributes, uuid);
    }

    @Override
    public Node applyChanges(Node xacmlAttributes, String uuid, String customId) throws Exception {
        return UConFactory.getProtocolInstance().applyChanges(xacmlAttributes, uuid, customId);
    }

    @Override
    public final Node tryAccess(Node accessRequest, String pepUrl, String customId) throws Exception {
        return UConFactory.getProtocolInstance().tryAccess(accessRequest, pepUrl, customId);
    }

    @Override
    public final Node startAccess(String uuid, String customId) throws Exception {
        return UConFactory.getProtocolInstance().startAccess(uuid, customId);
    }
   
    @Override
    public final List<Node> endAccess(List<String> uuidList, List<String> customIdList) throws Exception {
        return UConFactory.getProtocolInstance().endAccess(uuidList, customIdList);
    }

    @Override
    public final Node endAccess(String uuid, String customId) throws Exception {
        return UConFactory.getProtocolInstance().endAccess(uuid, customId);
    }
   
    @Override
    public final Node heartbeat(String pepUrl, List<String> sessionsList) throws Exception {
        return UConFactory.getProtocolInstance().heartbeat(pepUrl, sessionsList);
    }

    @Override
    public final Node echo(Node node) throws Exception {
        return UConFactory.getProtocolInstance().echo(node);
    }

    @Override
    public final Node assignCustomId(String uuid, String oldCustomId, String newCustomId) throws Exception {
        return UConFactory.getProtocolInstance().assignCustomId(uuid, oldCustomId, newCustomId);
    }

    @Override
    public Node apply(String actionName, String uuid, String customId, Object... args) throws Exception {
        return UConFactory.getProtocolInstance().apply(actionName, uuid, customId, args);
    }

}
