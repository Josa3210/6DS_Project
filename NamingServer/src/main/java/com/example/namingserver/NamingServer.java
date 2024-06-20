package com.example.namingserver;

import com.example.namingserver.database.I_NamingserverDB;
import com.example.namingserver.database.NamingserverDB;
import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.cluster.MembershipListener;
import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.ListenerConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import jakarta.annotation.PostConstruct;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.FileNotFoundException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.abs;
import static java.util.Collections.max;

@Component

public class NamingServer implements I_NamingServer {
    private static final String multicast_address = "224.2.2.5";
    private static HazelcastInstance hazelcastInstance;
    private static ClusterMemberShipListener event_listener;
    /**
     * Database containing the IP's of the different nodes {@see I_NamingserverDB}
     */
    private static I_NamingserverDB database;
    private String ip;


    public NamingServer() {
    }

    /**
     * this method sets up a Hazelcast instance with clustering enabled, specific network configurations including
     * port settings and multicast for node discovery, enables the REST API and management center console,
     * and finally creates a Hazelcast instance with these settings.
     */

    private static void CreateConfig() throws FileNotFoundException {

        Config config = new Config();
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

    /**
     * Constructor of the Naming Server
     * Creating a new database
     */
    @PostConstruct
    public void init() {
        try {
            System.out.println(">> Initializing NamingServer");
            this.ip = Inet4Address.getByName(Inet4Address.getLocalHost().getHostAddress()).getHostAddress();
            database = new NamingserverDB();
            database.load();
            database.print();
            event_listener = new ClusterMemberShipListener((NamingserverDB) database);
            NamingServer.CreateConfig();
        } catch (UnknownHostException | FileNotFoundException e) {
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
    public int getFileOwner(String filename) {

        // Get hash of the file name
        int hash = computeHash(filename);

        // Setup variables
        double smallestDist = Double.POSITIVE_INFINITY;
        int node = 0;

        // Calculate distance
        Set<Integer> keys = database.getKeys();
        for (Integer key : keys) {
            double dist = abs(key - hash);
            if (dist < smallestDist) {
                smallestDist = dist;
                node = key;
            }
        }

        if (node < hash) node = max(keys);

        return node;
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
        database.put(hash, ipaddress);
        database.save();
        // Reallocate resources
    }

    /**
     * Returns the IP of a node using the node ID
     *
     * @param nodeID the ID of the requested node
     * @return Inet4Address of the node
     */
    @Override
    public Inet4Address getIP(int nodeID) {
        return database.get(nodeID);
    }

    /**
     * This method removes a node with the given node name and IP address from the database.
     * It computes a hash value for the node name, removes the corresponding entry from the
     * database, and saves the updated database.
     *
     * @param nodeID the id of the node to be removed
     */
    @Override
    public void removeNodeIP(int nodeID) {
        database.remove(nodeID);
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
            if (Character.isDigit(c)) hash_value = (hash_value + Integer.parseInt(String.valueOf(c)) * p_pow) % m;
            else hash_value = (hash_value + (c - 'a' + 1) * p_pow) % m;

            p_pow = (p_pow * p) % m;
        }
        return hash_value;
    }


    /**
     * Returns the size of the cluster of nodes in the network
     *
     * @return size of the cluster as an integer
     */
    @Override
    public int sendNumNodes() {
        // Get the number of nodes in the cluster (network)
        return hazelcastInstance.getCluster().getMembers().size() - 1;
    }

    /**
     * Searches for the 2 closest nodes in the network using its hash
     *
     * @param hash the hash of the node
     * @return the closest smaller (int[0]) and closest larger node ID (int[1])
     */
    @Override
    public int[] giveLinkIds(int hash) {
        System.out.println(">> Giving link ids to client");
        database.print();
        Set<Integer> keys = database.getKeys();
        System.out.println("* Hash: " + hash);

        // Find the closest smaller and larger keys than the hash
        int prevID, nextID;
        int closestSmaller = -1, closestLarger = Integer.MAX_VALUE;

        // Check for the closest hash on the upside and downside
        for (Integer key : keys) {
            if (closestSmaller < key && key < hash) closestSmaller = key;
            if (hash < key && key < closestLarger) closestLarger = key;
        }

        // Check if closestSmaller is still -1, meaning no smaller key found
        if (closestSmaller != -1) prevID = closestSmaller;
        else prevID = max(keys);

        // Check if closestLarger is still Integer.MAX_VALUE, meaning no larger key found
        if (closestLarger != Integer.MAX_VALUE) nextID = closestLarger;
        else nextID = Collections.min(keys);

        System.out.println("* Returning | prev ID: " + prevID + ", next ID: " + nextID);

        return new int[]{prevID, nextID};
    }

    /**
     * Rest request to welcome the client (http://[clientIP]:8080/welcome)
     * Sending the ip of the naming server, the size of the cluster and the port of the naming server
     *
     * @param clientIP the IP of the client
     */
    private void welcomeClient(Inet4Address clientIP) {
        System.out.println(">> Welcoming client");
        System.out.println("* ClientIP: " + clientIP.getHostAddress());
        System.out.println("* Naming Server IP: " + this.ip);
        System.out.println("* Is client IP == naming server IP? : " + clientIP.getHostAddress().equals(this.ip));

        if (clientIP.getHostAddress().equals(this.ip)) return;


        String postUrl = "http://" + clientIP.getHostAddress() + ":8080/welcome";
        RestTemplate restTemplate = new RestTemplate();

        System.out.println(">> Welcoming client - Sending request: " + postUrl);
        // Create the request body
        Map<String, Object> requestBody = new HashMap<>() {{
            put("nrNodes", sendNumNodes());
            put("ip", ip);
            put("port", 8080);
        }};

        restTemplate.postForEntity(postUrl, requestBody, Void.class);
    }

    public String getHostNameClient(int id)
    {
        String clientIP = getIP(id).getHostAddress();
        String getUrl = "http://" + clientIP + ":8080/host";
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(getUrl, String.class);
        return response.getBody();
    }

    public ArrayList<ArrayList<String>> getClientFiles(int id)
    {
        String clientIP = getIP(id).getHostAddress();
        String getUrl = "http://" + clientIP + ":8080/getFiles";
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<ClientFilesResponse> response = restTemplate.getForEntity(getUrl, ClientFilesResponse.class);
        return response.getBody().getClientFiles();
    }

    public void shutdownClient(int nodeId)
    {
        String clientIP = getIP(nodeId).getHostAddress();
        String postUrl = "http://" + clientIP + ":8080/shutdown/exit";
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> requestBody = new HashMap<>();
        restTemplate.postForEntity(postUrl, requestBody, Void.class);
    }

    public HashMap<Integer, Inet4Address> returnData()
    {
        return database.getNodeMap();
    }

    public class ClusterMemberShipListener implements MembershipListener {
        I_NamingserverDB database;

        public ClusterMemberShipListener(NamingserverDB namingserverDB) {
            this.database = namingserverDB;
        }

        public void memberAdded(MembershipEvent membershipEvent) {
            String s = membershipEvent.getMember().getSocketAddress().toString();
            s = s.substring(s.indexOf("/") + 1, s.indexOf(":"));
            try {
                Inet4Address ip_address = (Inet4Address) Inet4Address.getByName(s);
                TimeUnit.SECONDS.sleep(15);
                welcomeClient(ip_address);
            } catch (UnknownHostException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // Replicated node gets only changed when a member gets removed from the network
        public void memberRemoved(MembershipEvent membershipEvent) {
            String s = membershipEvent.getMember().getSocketAddress().toString();
            s = s.substring(s.indexOf("/") + 1, s.indexOf(":"));

        }
    }
}






