package com.example.ds_project;

import com.example.ds_project.database.I_NamingserverDB;
import com.example.ds_project.database.NamingserverDB;

import java.net.Inet4Address;
import java.util.HashMap;
import java.util.Set;

import static java.util.Collections.max;

public class NamingServer implements I_NamingServer {
    I_NamingserverDB database;

    public NamingServer() {
        String filepath = "Data/DB/namingServer";
        database = new NamingserverDB(filepath); // <-- insert filepath
        this.database.load();
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
        Set<Integer> keys = this.database.getKey();

        // Setup variables
        double smallestDist = Double.POSITIVE_INFINITY;
        int node = 0;

        // Calculate distance
        for (int key : keys) {
            double dist = key - hash;
            if (dist < smallestDist) {
                smallestDist = dist;
                node = key;
            }
        }

        if (node < hash) {
            node = max(keys);
        }

        return this.database.get(hash);
    }

    @Override
    public void addNodeIP(String nodeName, Inet4Address ipaddress) {
        int hash = computeHash(nodeName);
        this.database.put(hash, ipaddress);
        this.database.save();

        // Reallocate resources
    }

    @Override
    public void removeNodeIP(String nodeName, Inet4Address ipaddress) {
        int hash = computeHash(nodeName);
        this.database.remove(hash);
        this.database.save();
        
        // Reallocate resources
    }
}
