package com.example.namingserver.database;

import java.net.Inet4Address;
import java.util.Set;

public interface I_NamingserverDB {
    //String filePath;

    // Load the map from file
    void load();

    // Save the map to file
    void save();

    // Get address from hash
    Inet4Address get(Integer hash);

    // Put address with key hash
    void put(Integer hash, Inet4Address ip);

    Set<Integer> getKeys();

    void remove(int hash);

    void print();
}
