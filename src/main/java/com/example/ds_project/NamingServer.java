package com.example.ds_project;

import com.example.ds_project.database.I_NamingserverDB;
import com.example.ds_project.database.NamingserverDB;

import java.net.Inet4Address;
import java.util.HashMap;

public class NamingServer implements I_NamingServer {
    I_NamingserverDB database;
    HashMap<Inet4Address, Integer> nodeDB;

    public NamingServer() {
        String filepath = "Data/DB/namingServer";
        database = new NamingserverDB(); // <-- insert filepath
        nodeDB = new HashMap<>();
    }

    static int computeHash(String s) {
        int p = 59;
        int m = 10000009;
        int hash_value = 0;
        int p_pow = 1;

        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) {
                hash_value = (hash_value + Integer.parseInt(String.valueOf(c)) * p_pow) % m;
            } else {
                hash_value = (hash_value + (c - 'a' + 1) * p_pow) % m;
            }
            p_pow = (p_pow * p) % m;
        }
        return hash_value;
    }

    @Override
    public Inet4Address getLocationIP(String filename) {
        int hash = computeHash(filename);
        return this.database.get(hash);
    }

    @Override
    public HashMap<Integer, Inet4Address> loadMap(String filePath) {
        return null;
    }

    @Override
    public void addNodeIP(String nodeName, Inet4Address ipaddress) {
        int hash = computeHash(nodeName);
        database.put(hash, ipaddress);

        // Reallocate resources
    }

    @Override
    public void removeNodeIP(Inet4Address ipaddress) {

    }
}
