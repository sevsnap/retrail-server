/*
 * CNR - IIT
 * Coded by: 2014 Enrico "KMcC;) Carniani
 */
package it.cnr.iit.retrail.server.dal;

import it.cnr.iit.retrail.commons.PepAttributeInterface;
import it.cnr.iit.retrail.commons.impl.PepAttribute;
import it.cnr.iit.retrail.commons.impl.PepRequest;
import java.util.HashMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author oneadmin
 */
public class UconRequest extends PepRequest {
    protected static PepAttributeInterface newAttribute(Element e) {
        PepAttribute a = new PepAttribute(e);
        return Attribute.newInstance(a, null);
    }
    
    public UconRequest(Document doc) {
        super(doc);
    }
    
    public UconRequest() {
        super();
    }

}
