/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package it.cnr.iit.retrail.server.test;

import it.cnr.iit.retrail.server.UCon;

/**
 *
 * @author oneadmin
 */
public class Test {

    public static void main(String[] args) throws Exception {
        UCon ucon = UCon.getInstance();
        ucon.addPIP(new TestPIPbalance(5));
        ucon.addPIP(new TestPIPreputation("bronze"));
    }
    
}
