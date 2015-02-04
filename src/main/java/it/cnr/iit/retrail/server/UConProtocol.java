/*
 * CNR - IIT
 * Coded by: 2014-2015 Enrico "KMcC;) Carniani
 */

package it.cnr.iit.retrail.server;

import java.util.List;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Node;

/**
 *
 * @author oneadmin
 */
public interface UConProtocol {

    Node echo(Node node) throws TransformerConfigurationException, TransformerException;

    Node tryAccess(Node accessRequest, String pepUrl, String customId) throws Exception;

    Node assignCustomId(String uuid, String oldCustomId, String newCustomId) throws Exception;
    
    Node startAccess(String uuid, String customId) throws Exception;

    Node endAccess(String uuid, String customId) throws Exception;
    
    List<Node> endAccess(List<String> uuidList, List<String> customIdsList) throws Exception;

    Node heartbeat(String pepUrl, List<String> sessionsList) throws Exception;

}
