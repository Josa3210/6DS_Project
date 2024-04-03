package com.example.namingserver;

import com.example.namingserver.database.I_NamingserverDB;
import com.example.namingserver.database.NamingserverDB;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.util.Set;

import static java.lang.Math.abs;
import static java.util.Collections.max;

@Component
public class NamingServer implements I_NamingServer {
    /**
     * Database containing the IP's of the different nodes {@see I_NamingserverDB}
     */
    I_NamingserverDB database;

    public NamingServer() {
        database = new NamingserverDB();
        this.database.load();
    }

    /**
     * This method computes a hash value for a given string by iterating through
     * each character and applying a polynomial rolling hash function. The hash
     * value is calculated as (sum of character values * p_pow) % m, where p is a
     * prime number, m is a large prime number, and p_pow is the current power of p.
     *
     * @param s the input string for which the hash value needs to be computed
     * @return the computed hash value for the input string
     */
    public int computeHash(String s) {
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
    public void sendNumNodes() {

    }

    @Override
    public void giveLinkIds(int nodeID) {

    }

    /**
     * This method retrieves the IP address associated with the location of a given
     * filename by computing a hash value for the filename and comparing it with the
     * keys in the database. It then calculates the closest distance to the hash value
     * and returns the corresponding IP address.
     *
     * @param filename the name of the file for which location IP is requested
     * @return the IP address of the location associated with the filename
     */
    @Override
    public Inet4Address getLocationIP(String filename) {

        int hash = computeHash(filename);
        Set<Integer> keys = this.database.getKeys();

        // Setup variables
        double smallestDist = Double.POSITIVE_INFINITY;
        int node = 0;

        // Calculate distance
        for (Integer key : keys) {
            double dist = abs(key - hash);
            if (dist < smallestDist) {
                smallestDist = dist;
                node = key;
            }
        }

        if (node < hash) {
            node = max(keys);
        }

        return this.database.get(node);
    }

    /**
     * This method adds a new node with the given node name and IP address to the database.
     * It computes a hash value for the node name, stores the IP address in the database,
     * and saves the updated database.
     *
     * @param nodeName  the name of the node to be added
     * @param ipaddress the IP address of the node to be added
     */
    @Override
    public void addNodeIP(String nodeName, Inet4Address ipaddress) {
        int hash = computeHash(nodeName);
        this.database.put(hash, ipaddress);

        // Reallocate resources
    }

    /**
     * This method removes a node with the given node name and IP address from the database.
     * It computes a hash value for the node name, removes the corresponding entry from the
     * database, and saves the updated database.
     *
     * @param nodeName  the name of the node to be removed
     * @param ipaddress the IP address of the node to be removed
     */
    @Override
    public void removeNodeIP(String nodeName, Inet4Address ipaddress) {
        int hash = computeHash(nodeName);
        this.database.remove(hash);

        // Reallocate resources
    }
}
