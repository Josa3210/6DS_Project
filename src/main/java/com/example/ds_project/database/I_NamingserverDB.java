package com.example.ds_project.database;

import java.net.Inet4Address;
import java.util.HashMap;
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

    Set<Integer> getKey();

    void remove(Integer hash);
}
