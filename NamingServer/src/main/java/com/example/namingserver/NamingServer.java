package com.example.namingserver;

import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.cluster.MembershipListener;
import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;


import com.example.namingserver.database.I_NamingserverDB;
import com.example.namingserver.database.NamingserverDB;
import com.hazelcast.spi.exception.RestClientException;
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

public class NamingServer implements I_NamingServer
{
    private static String multicast_address = "224.2.2.5";
    private static Config config;
    private static HazelcastInstance hazelcastInstance;
    private static IMap<String, String> mapIP;
    private static ClusterMemberShipListener event_listener;
    /**
     * Database containing the IP's of the different nodes {@see I_NamingserverDB}
     */
    private static I_NamingserverDB database;
    public String filePath;
    private String ip;

    public String folderPath = "Data/node/Files";


    public NamingServer() {}

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

    /**
     * Constructor of the Naming Server
     * Creating a new database
     */
    @PostConstruct
    public void init()
    {
        try
        {
            System.out.println(">> Initializing NamingServer");
            this.ip = Inet4Address.getByName(Inet4Address.getLocalHost().getHostAddress()).getHostAddress();
            this.database = new NamingserverDB();
            this.database.load();
            this.database.print();
            event_listener = new ClusterMemberShipListener((NamingserverDB) this.database);
            NamingServer.CreateConfig();
            mapIP = hazelcastInstance.getMap("mapIP");
        }
        catch (FileNotFoundException | UnknownHostException e) { throw new RuntimeException(e); }
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
    public Inet4Address getLocationIP(String filename)
    {
        // Get hash of the file name
        int hash = computeHash(filename);

        // Setup variables
        double smallestDist = Double.POSITIVE_INFINITY;
        int node = 0;

        // Calculate distance
        Set<Integer> keys = database.getKeys();
        for (Integer key : keys)
        {
            double dist = abs(key - hash);
            if (dist < smallestDist)
            {
                smallestDist = dist;
                node = key;
            }
        }

        if (node < hash) node = max(keys);

        return database.get(node);
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
    public void addNodeIP(String nodeName, Inet4Address ipaddress)
    {
        int hash = computeHash(nodeName);
        database.put(hash, ipaddress);
        database.save();
        // Reallocate resources
    }

    /**
     * Returns the IP of a node using the node ID
     * @param nodeID the ID of the requested node
     * @return Inet4Address of the node
     */
    @Override
    public Inet4Address getIP(int nodeID)
    {
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
    public void removeNodeIP(int nodeID)
    {
        database.remove(nodeID);

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
    public int computeHash(String s)
    {
        int p = 59;
        int m = 10000009;
        int hash_value = 0;
        int p_pow = 1;

        for (char c : s.toCharArray())
        {
            if (Character.isDigit(c)) hash_value = (hash_value + Integer.parseInt(String.valueOf(c)) * p_pow) % m;
            else hash_value = (hash_value + (c - 'a' + 1) * p_pow) % m;

            p_pow = (p_pow * p) % m;
        }
        return hash_value;
    }

    public Inet4Address getIp(int id) {
        return database.get(id);
    }


    /**
     * Returns the size of the cluster of nodes in the network
     * @return size of the cluster as an integer
     */
    @Override
    public int sendNumNodes()
    {
        // Get the number of nodes in the cluster (network)
        return hazelcastInstance.getCluster().getMembers().size() - 1;
    }

    /**
     * Searches for the 2 closest nodes in the network using its hash
     * @param hash the hash of the node
     * @return the closest smaller (int[0]) and closest larger node ID (int[1])
     */
    @Override
    public int[] giveLinkIds(int hash)
    {
        System.out.println(">> Giving link ids to client");
        database.print();
        Set<Integer> keys = database.getKeys();
        System.out.println("* Hash: " + hash);

        // Find the closest smaller and larger keys than the hash
        int prevID, nextID;
        int closestSmaller = -1, closestLarger = Integer.MAX_VALUE;

        // Check for the closest hash on the upside and downside
        for (Integer key : keys)
        {
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
     * @param clientIP the IP of the client
     */
    private void welcomeClient(Inet4Address clientIP)
    {
        System.out.println(">> Welcoming client");
        System.out.println("* ClientIP: " + clientIP.getHostAddress());
        System.out.println("* Naming Server IP: " + this.ip);
        System.out.println("* Is client IP == naming server IP? : " + clientIP.getHostAddress().equals(this.ip));

        if (clientIP.getHostAddress().equals(this.ip)) return;

        System.out.println(">> Welcoming client - Sending POST request");

        String postUrl = "http://" + clientIP.getHostAddress() + ":8080/welcome";

        System.out.println("* URI : " + postUrl);

        RestTemplate restTemplate = new RestTemplate();

        // Create the request body
        Map<String, Object> requestBody = new HashMap<>()
        {{
            put("nrNodes", sendNumNodes());
            put("ip", ip);
            put("port", 8080);
        }};

        restTemplate.postForEntity(postUrl, requestBody, Void.class);
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
                TimeUnit.SECONDS.sleep(20);
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
    /**
     * Receives filename. Replication is performed as follows:
     * 1. If the hash of the node is less than the hash of the file and the distance to it is the smallest, indicating that the node is a replicated node,
     * the node becomes the owner of this file and creates a log with information on the file (references for the file).
     * 2. The node then replicates the file.
     *
     * @param filename The name of the file that needs to be replicated and the ip address of where it is originated from
     */

    public void reportLogger(String filename, Inet4Address originalIP, int operation, int nextID) {

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create the request body
        Map<String, Object> requestBody = new HashMap<>();

        // Create the request entity with headers and body
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        // Check for every node in the list if it's a replicated node by checking which node hash in the dataset is
        // the closest to the hashed value of the filename

        Inet4Address replicatedIP = getLocationIP(filename);
        System.out.println("original IP: " + originalIP + "; replicated IP: " + replicatedIP);

        int fileHash = 0;
        System.out.println("Computing the hash of the filename");
        fileHash = computeHash(filename);

        if (operation ==1) {

            if(originalIP.getHostAddress().equals(replicatedIP.getHostAddress())){
                System.out.println("Replicated ip = original IP");
                System.out.println("next ID: " + nextID);
                replicatedIP = getIP(nextID);}


            System.out.println("\nNode: " + replicatedIP.getCanonicalHostName() + " with IP " + replicatedIP.getHostAddress() + " is the replicated node of file: " + filename);
            String postUrl = "http://" + replicatedIP.getHostAddress() + ":8080/isReplicatedNode";

            try {

                // Send the POST request
                requestBody.put("hashValue", fileHash);
                requestBody.put("original ip", originalIP);
                requestBody.put("filepath", filePath);
                System.out.println("filepath: " + filePath);

                System.out.println("ID of the replicated node: " + replicatedIP.getHostAddress());
                System.out.println(originalIP.getHostAddress());

                ResponseEntity<Void> responseEntity = restTemplate.postForEntity(postUrl, requestEntity, Void.class);
                HttpStatusCode statusCode = responseEntity.getStatusCode();

                if (statusCode == HttpStatus.OK) {
                    System.out.println("Node list correctly sent to " + replicatedIP.getHostAddress() + "from ip" + originalIP.getHostAddress());
                } else {
                    System.err.println("Sending node list failed with status code: " + statusCode);
                }
            } catch (RestClientException e) {
                System.err.println("Failed to send node list to " + replicatedIP + ": " + e.getMessage());
            }

        }

        else{ // operation == 2 --> file also gets deleted in the replicated node

            System.out.println("\nNode with IP " + replicatedIP.getHostAddress() + "will delete the replicated file: " + filename);

            System.out.println("Replicated IP: " + replicatedIP.getHostAddress() + " original IP: " + originalIP.getHostAddress());

            if (replicatedIP.getHostAddress().equals(originalIP.getHostAddress())){

                replicatedIP = getIP(nextID); // if the replicated ip == original ip, then the replicated file is store on the node with nextID!
                System.out.println("Replicated file is stored on the node with nextID, deleting it there ..");
            }

            String postUrl = "http://" + replicatedIP.getHostAddress() + ":8080/deleteReplicatedFile";


            try{
                // Send the POST request with the replicated IP --> it becomes the new 'original' IP
                requestBody.put("hashValue", fileHash);
                requestBody.put("filename", filename);
                requestBody.put("filepath", filePath);

                ResponseEntity<Void> responseEntity = restTemplate.postForEntity(postUrl, requestEntity, Void.class);
                HttpStatusCode statusCode = responseEntity.getStatusCode();

                if (statusCode == HttpStatus.OK) {
                    System.out.println("Node list correctly sent to " + replicatedIP.getHostAddress() + " from ip " + originalIP.getHostAddress());
                } else {
                    System.err.println("Sending node list failed with status code: " + statusCode);
                }
            } catch (RestClientException e) {
                System.err.println("Failed to send node list to " + replicatedIP + ": " + e.getMessage());
            }

        }
    }
    public void shutdown_node(int PrevID, HashMap<Integer, Inet4Address> nodeMap, HashMap<Integer, String> fileMap, Inet4Address originalIP){
        Inet4Address newReplicatedIP = database.get(PrevID);

        System.out.println("sending the replicated files to node " + newReplicatedIP.getHostAddress() + " from IP "+ originalIP.getHostAddress());
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://" + newReplicatedIP.getHostAddress() +":8080/shutdown/sendFiles";
        Map<String, Object> requestbody = new HashMap<>();
        requestbody.put("originalIP", originalIP.getHostAddress());
        requestbody.put("nodeMap", nodeMap);
        requestbody.put("fileMap", fileMap);
        System.out.println("filemap: " + fileMap);
        restTemplate.postForEntity(url, requestbody, Void.class);
    }
}






