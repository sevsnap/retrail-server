/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package it.cnr.iit.retrail.server;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.xmlrpc.XmlRpcException;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 *
 * @author oneadmin
 */
public interface API {

    Node echo(Node node) throws TransformerConfigurationException, TransformerException;

    Node endAccess(String sessionId);

    Node heartbeat(String pepUrl, List<String> sessionsList) throws MalformedURLException, ParserConfigurationException, XmlRpcException, SAXException, IOException;

    Node startAccess(Node accessRequest, String pepUrl) throws MalformedURLException;

    Node tryAccess(Node accessRequest);
    
}
