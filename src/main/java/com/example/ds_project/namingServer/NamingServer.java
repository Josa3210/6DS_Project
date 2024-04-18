package com.example.ds_project.namingServer;

import com.example.ds_project.namingServer.database.I_NamingserverDB;
import com.example.ds_project.namingServer.database.NamingserverDB;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.Inet4Address;
import java.util.Collections;
import java.util.Set;

import static java.lang.Math.abs;
import static java.util.Collections.max;

@SpringBootApplication
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

    public Inet4Address getIp(int id) {
        return this.database.get(id);
    }

    @Override
    public void sendNumNodes() {
        
    }

    @Override
    public int[] giveLinkIds(int nodeID)
    {
        // Step 1: Compute hash value for the nodeID
        int hash = computeHash(Integer.toString(nodeID));

        // Step 2: Retrieve the set of keys from the database
        Set<Integer> keys = this.database.getKeys();

        // Step 3: Find previous and next IDs
        int prevID = -1;
        int nextID = -1;

        // Find the closest smaller and larger keys than the hash
        int closestSmaller = -1;
        int closestLarger = Integer.MAX_VALUE;
        for (Integer key : keys)
        {
            if (key < hash && key > closestSmaller) closestSmaller = key;
            if (key > hash && key < closestLarger) closestLarger = key;
        }

        // Check if closestSmaller is still -1, meaning no smaller key found
        // If no smaller key found, wrap around to the maximum key
        if (closestSmaller != -1) prevID = closestSmaller;
        else prevID = max(keys);


        // Check if closestLarger is still Integer.MAX_VALUE, meaning no larger key found
        // If no larger key found, wrap around to the minimum key
        if (closestLarger != Integer.MAX_VALUE) nextID = closestLarger;
        else nextID = Collections.min(keys);

        // Step 4: Return the previous and next IDs as an array
        return new int[]{prevID, nextID};
    }

    public static void main(String[] args) {
        SpringApplication.run(NamingServer.class, args);
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
