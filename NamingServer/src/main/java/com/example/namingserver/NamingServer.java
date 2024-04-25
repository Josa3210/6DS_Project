package com.example.namingserver;

import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.cluster.MembershipListener;
import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;


import com.example.namingserver.database.I_NamingserverDB;
import com.example.namingserver.database.NamingserverDB;
import jakarta.annotation.PostConstruct;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.FileNotFoundException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.abs;
import static java.util.Collections.max;

@Component

public class NamingServer implements I_NamingServer {
    private static String multicast_address = "224.2.2.5";
    private static Config config;
    private static HazelcastInstance hazelcastInstance;
    private static IMap<String, String> mapIP;
    private static ClusterMemberShipListener event_listener;
    /**
     * Database containing the IP's of the different nodes {@see I_NamingserverDB}
     */
    I_NamingserverDB database;
    private InetAddress ip;

    public NamingServer() {

    }

    /**
     * this method sets up a Hazelcast instance with clustering enabled, specific network configurations including
     * port settings and multicast for node discovery, enables the REST API and management center console,
     * and finally creates a Hazelcast instance with these settings.
     *
     * @throws FileNotFoundException
     */

    private static void CreateConfig() throws FileNotFoundException {

        config = new Config();
        config.getJetConfig().setEnabled(true);
        config.setClusterName("testCluster");
        NetworkConfig networkConfig = config.getNetworkConfig();
        networkConfig.setPort(5701);
        networkConfig.setPortAutoIncrement(true); // Allows automatic increment of port numbers if necessary.
        networkConfig.addOutboundPortDefinition("5900-5915");
        networkConfig.getRestApiConfig().setEnabled(true); // Enables the REST API for the network configuration.
        JoinConfig joinConfig = networkConfig.getJoin();
        joinConfig.getMulticastConfig().setEnabled(true); // Enables multicast for node discovery
        joinConfig.getMulticastConfig().setMulticastGroup(multicast_address); // Sets the multicast group address.
        joinConfig.getMulticastConfig().setMulticastPort(54321);
        config.getManagementCenterConfig().setConsoleEnabled(true); // Enables the management center console.
        config.addListenerConfig(new ListenerConfig(event_listener));
        hazelcastInstance = Hazelcast.newHazelcastInstance(config); // Creates a new Hazelcast instance with the provided configuration.
    }

    @PostConstruct
    public void init() {
        try {
            this.ip = InetAddress.getLocalHost();
            database = new NamingserverDB();
            this.database.load();
            System.out.println("Database in namingServer: ");
            this.database.print();
            event_listener = new ClusterMemberShipListener();
            NamingServer.CreateConfig();
            mapIP = hazelcastInstance.getMap("mapIP");
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
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

    @Override
    public Inet4Address getIP(int nodeID) {
        return this.database.get(nodeID);
    }

    /**
     * This method removes a node with the given node name and IP address from the database.
     * It computes a hash value for the node name, removes the corresponding entry from the
     * database, and saves the updated database.
     *
     * @param nodeName the name of the node to be removed
     */
    @Override
    public void removeNodeIP(String nodeName) {
        int hash = computeHash(nodeName);
        this.database.remove(hash);

        // Reallocate resources
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
    public int sendNumNodes() {
        // Get the number of nodes in the cluster (network)
        return hazelcastInstance.getCluster().getMembers().size() - 1;
    }

    @Override
    public int[] giveLinkIds(int nodeID) {
        System.out.println("Giving link ids to client---------");
        this.database.print();
        int hash = computeHash(Integer.toString(nodeID));
        Set<Integer> keys = this.database.getKeys();
        System.out.println("hash: " + hash);

        // Find the closest smaller and larger keys than the hash
        int prevID = -1, nextID = -1;
        int closestSmaller = -1, closestLarger = Integer.MAX_VALUE;

        for (Integer key : keys) {
            if (key < hash && key > closestSmaller) closestSmaller = key;
            if (key > hash && key < closestLarger) closestLarger = key;
        }

        System.out.println("closest: " + closestLarger + "." + closestSmaller);

        // Check if closestSmaller is still -1, meaning no smaller key found
        if (closestSmaller != -1) prevID = closestSmaller;
        else prevID = max(keys);

        // Check if closestLarger is still Integer.MAX_VALUE, meaning no larger key found
        if (closestLarger != Integer.MAX_VALUE) nextID = closestLarger;
        else nextID = Collections.min(keys);
        System.out.println("returning: " + nextID);

        return new int[]{prevID, nextID};
    }

    private void welcomeClient(Inet4Address clientIP) {
        System.out.println("clientIP: " + clientIP.getHostAddress());
        System.out.println("ip: " + this.ip.getHostAddress());
        System.out.println("CHECK: " + clientIP.getHostAddress().equals(this.ip.getHostAddress()));
        if (clientIP.getHostAddress().equals(this.ip.getHostAddress())) return;

        try {
            String ipString = InetAddress.getLocalHost().getHostAddress();
            System.out.println("IP : " + ipString);
            String postUrl = "http://" + clientIP.getHostAddress() + ":8080/welcome";
            System.out.println("URI : " + postUrl);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create the request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("nrNodes", sendNumNodes());
            requestBody.put("ip", ipString);
            requestBody.put("port", 8080);
            System.out.println("Body : " + requestBody);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Void> responseEntity = restTemplate.postForEntity(postUrl, requestEntity, Void.class);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public class ClusterMemberShipListener implements MembershipListener {
        public void memberAdded(MembershipEvent membershipEvent) {
            String s = membershipEvent.getMember().getSocketAddress().toString();
            s = s.substring(s.indexOf("/") + 1, s.indexOf(":"));
            int hash = computeHash(s);
            try {
                Inet4Address ip_address = (Inet4Address) Inet4Address.getByName(s);
                database.put(hash, ip_address);
                TimeUnit.SECONDS.sleep(20);
                welcomeClient(ip_address);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public void memberRemoved(MembershipEvent membershipEvent) {
            String s = membershipEvent.getMember().getSocketAddress().toString();
            s = s.substring(s.indexOf("/") + 1, s.indexOf(":"));
            int hash = computeHash(s);
            try {
                Inet4Address ip_address = (Inet4Address) Inet4Address.getByName(s);
                database.put(hash, ip_address);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
    }

}

