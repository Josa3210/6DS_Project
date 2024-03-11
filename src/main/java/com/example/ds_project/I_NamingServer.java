package com.example.ds_project;

import java.net.Inet4Address;
import java.util.HashMap;

public interface I_NamingServer {
    /**
     * Get the IP address of the node containing a specific file
     *
     * @param filename: name of the file to search for
     * @return the ip address of the node that contains the resource {@see Inet4Address}
     */
    Inet4Address getLocationIP(String filename);


    /**
     * Load the map with the resources to IP
     *
     * @param filePath: path where the map is stored
     * @return the hashMap {@see HashMap}
     */
    HashMap<Integer, Inet4Address> loadMap(String filePath);

    //

    /**
     * Add a node to the namingServer
     *
     * @param ipaddress: the ipaddress of the new node
     */
    void addNodeIP(String nodeName, Inet4Address ipaddress);


    /**
     * Remove a node from the namingServer
     *
     * @param ipaddress: the ipaddress of the node to remove
     */
    void removeNodeIP(Inet4Address ipaddress);

}
