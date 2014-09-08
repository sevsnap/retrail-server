/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.iit.retrail.server.db;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 *
 * @author oneadmin
 */
@Entity
public class UconSession {

	//For SQLite use GenerationType.AUTO to generate id
    //for derby, H2, MySQL etc use GenerationType.IDENTITY
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String cookie;

    public String getCookie() {
        return cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    //this is optional, just for print out into console
    @Override
    public String toString() {
        return "UconSession [id=" + id + ", cookie=" + cookie + "]";
    }

}
