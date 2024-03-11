package com.example.ds_project;

import com.example.ds_project.database.NamingserverDB;

import java.net.Inet4Address;
import java.util.HashMap;

public class NamingServer implements I_NamingServer {
    NamingserverDB database;
    
    public NamingServer(){

    }

    @Override
    public Inet4Address getLocationIP(String filename) {
        return null;
    }

    @Override
    public HashMap<Integer, Inet4Address> loadMap(String filePath) {
        return null;
    }

    @Override
    public void addNodeIP(Inet4Address ipaddress) {

    }

    @Override
    public void removeNodeIP(Inet4Address ipaddress) {

    }
}
