/* */
package it.cnr.iit.retrail.server;

import it.cnr.iit.retrail.commons.DomUtils;
import it.cnr.iit.retrail.commons.PepAccessRequest;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class API {

    public Node tryAccess(Node accessRequest) {
        Element request = (Element)((Document)accessRequest).getFirstChild();
        return UCon.getInstance().tryAccess(request);
    }

    public void startAccess(Node accessRequest) {
        UCon.getInstance().startAccess((Element)accessRequest);
    }

    public void endAccess(Node accessRequest) {
        UCon.getInstance().startAccess((Element)accessRequest);
    }

    public Node echo(Node node) throws TransformerConfigurationException, TransformerException {
        System.out.println("HO RICEVUTO NODE:");
        // write the content into xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(node);
        // Output to console for testing
        StreamResult result = new StreamResult(System.out);
        transformer.transform(source, result);
        return node;
    }

}
