/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package it.cnr.iit.retrail.server;

import it.cnr.iit.retrail.commons.PepAccessRequest;
import it.cnr.iit.retrail.commons.PepRequestAttribute;

/**
 *
 * @author kicco
 */
public class PIP {
    public boolean process(PepAccessRequest request) {
        System.out.println("PIP.process(): dummy PIP processor called, ignoring");
        //PepRequestAttribute test = new PepRequestAttribute("tuazia", "zietta", "we", "issuer", "cazness");
        //request.add(test);
        return true;
    }
}
