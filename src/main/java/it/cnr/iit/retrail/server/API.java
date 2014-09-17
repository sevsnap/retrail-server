/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package it.cnr.iit.retrail.server;

import java.net.MalformedURLException;
import java.util.List;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Node;

/**
 *
 * @author oneadmin
 */
public interface API {

    Node echo(Node node) throws TransformerConfigurationException, TransformerException;

    Node tryAccess(Node accessRequest, String pepUrl, String customId) throws MalformedURLException;

    Node assignCustomId(String uuid, String oldCustomId, String newCustomId);
    
    Node startAccess(String uuid, String customId) throws MalformedURLException;

    Node endAccess(String uuid, String customId) throws MalformedURLException;

    Node heartbeat(String pepUrl, List<String> sessionsList) throws Exception;

}
