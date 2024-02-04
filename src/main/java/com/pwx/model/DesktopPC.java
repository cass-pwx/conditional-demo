package com.pwx.model;

/**
 * @author pengweixin
 */

public class DesktopPC extends Computer{

    private String host;

    public DesktopPC(String name, String host) {
        super(name);
        this.host = host;
    }
}
