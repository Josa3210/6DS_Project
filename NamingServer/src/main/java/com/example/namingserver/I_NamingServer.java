package com.example.namingserver;

import java.net.Inet4Address;

public interface I_NamingServer {
    /**
     * Get the IP address of the node containing a specific file
     *
     * @param filename: name of the file to search for
     * @return the ip address of the node that contains the resource {@see Inet4Address}
     */
    Inet4Address getLocationIP(String filename);

    /**
     * Add a node to the namingServer
     *
     * @param ipaddress: the ipaddress of the new node
     */
    void addNodeIP(String nodeName, Inet4Address ipaddress);

    Inet4Address getIP(int nodeID);

    /**
     * Remove a node from the namingServer
     */
    void removeNodeIP(String nodeName);

    /**
     * Returns a hash (in the form of an integer) of any given string
     *
     * @param s: string with undetermined size
     * @return an integer hash
     */
    int computeHash(String s);

    /**
     * Sends the number of nodes in the network
     *
     * @return
     */
    int sendNumNodes();

    /**
     * Return the nextID and previousID of the given ID
     */
    int[] giveLinkIds(int nodeID);
}
