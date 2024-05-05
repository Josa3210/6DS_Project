package com.example.node;

import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.cluster.MembershipListener;
import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import jakarta.annotation.PostConstruct;
import org.springframework.http.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.io.FileNotFoundException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class Client implements I_Client {

    private static final String multicast_address = "224.2.2.5";
    private static ClusterMemberShipListener event_listener;
    RestClient restClient;
    int currentID, nextID, prevID;
    Config config;
    Map<String, Inet4Address> ipMap;
    Inet4Address currentIP;
    Logger logger;
    Thread fileMonitorThread;
    private Inet4Address namingServerIP;
    private Integer namingServerPort;
    private String hostname;

    public Client(String hostname) {
        try {
            event_listener = new ClusterMemberShipListener();
            this.config = createConfig();
            this.logger = new Logger(); // We create a logger to keep track of the replication
            logger.load();
            fileMonitorThread = new Thread(new FileMonitor(this, "Data/node/Files"));
            fileMonitorThread.start();
            this.hostname = hostname;
            this.restClient = RestClient.create();
            this.currentIP = (Inet4Address) InetAddress.getLocalHost();
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

    }

    private Config createConfig() throws FileNotFoundException {

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
        return config;
    }

    public Logger getLogger() {
        return logger;
    }

    @Override
    public void printLinkIds() {
        System.out.println("Link ids");
        System.out.println("currentID: " + String.valueOf(this.currentID));
        System.out.println("NextID: " + String.valueOf(this.nextID));
        System.out.println("PreviousID: " + String.valueOf(this.prevID));
        System.out.println("IP + port NS: " + this.namingServerIP + ":" + this.namingServerPort);
    }

    @PostConstruct
    public void init() {
        habari();
        this.currentID = computeHash(hostname);
        this.nextID = Integer.MAX_VALUE;
        this.prevID = Integer.MIN_VALUE;
    }

    @Override
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

    /**
     * Send message to every other node when joining the network
     * "Habari" is "Hello" in Swahili
     *
     * <p>
     * Calculate own hash and put it as currentID
     * Send own name and IP-address to everyone using MC
     * </p>
     *
     * <p>
     * Receive message from naming server with #node in network:
     * #nodes = 1 -> nextID & prevID = currentID
     * #nodes > 1 -> will receive message from other nodes with 2 ID's
     * </p>
     */
    @Override
    public void habari() {
        // Joins multicast group
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(this.config); // Creates a new Hazelcast instance with the provided configuration.
    }


    public void setupClient(int nrNodes, Inet4Address namingServerIP, int namingServerPort) {
        this.namingServerIP = namingServerIP;
        this.namingServerPort = namingServerPort;

        addNameToNS();

        if (nrNodes == 1) {
            nextID = currentID;
            prevID = currentID;
        } else {
            int[] ids = requestLinkIds();
            this.prevID = ids[0];
            this.nextID = ids[1];
        }

        System.out.println("Next node ID: " + nextID);
        System.out.println("Prev node ID: " + prevID);

        sendLinkID(nextID);
        sendLinkID(prevID);

        //TODO : werkt bijna prefect, enkel de eerste node nexId wordt nooit geupdate omdat de twee volgende nodes
        //TODO : hun ID lager zijn dan de eerste.
    }

    private void addNameToNS() {
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            String postUrl = "http://" + namingServerIP.getHostAddress() + ":8080/ns/addNode";

            System.out.println("posturl: " + postUrl);
            System.out.println("input: " + ip + "&" + hostname);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create the request body
            Map<String, Object> requestBody = new HashMap<>() {{
                put("ip", ip);
                put("name", hostname);
            }};

            System.out.println("Requesting...");

            // Send the POST request
            restTemplate.postForEntity(postUrl, requestBody, Void.class);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    /*Discovery + Bootstrap*/

    /**
     * Reply to "Habari" of other node
     * "Karibu" is "Welcome" in Swahili
     *
     * <p>
     * Calculate hash entering node (enteringID)
     * Check if entering node is new previous/next node (look slides)
     * Yes: answer (UC) to entering node with (startID, otherID) -> dependent of hash
     * No: do nothing
     * </p>
     * <p>
     */
    @Override
    public void karibu(int hash) {
        if ((this.currentID < hash) && (hash < this.nextID)) {
            this.nextID = hash;
        } else if ((this.prevID < hash) && (hash < this.currentID)) {
            this.prevID = hash;
        }
    }

    @Override
    public int[] requestLinkIds() {
        return requestLinkIds(currentID);
    }

    @Override
    public int[] requestLinkIds(int requestID) {
        String getUrl = "http://" + namingServerIP.getHostAddress() + ":8080/ns/giveLinkID/" + requestID;
        RestTemplate restTemplate = new RestTemplate();

        System.out.println("RequestLinks---------------");
        System.out.println("url requestLinks: " + getUrl);

        ResponseEntity<int[]> response = restTemplate.getForEntity(getUrl, int[].class);
        System.out.println("response: " + response.getBody()[0] + "&" + response.getBody()[1]);
        return response.getBody();
    }

    // Use "receiveLinkID" and "sendLinkID" from Shutdown

    /*Shutdown*/

    @Override
    public Inet4Address requestIP(int nodeID) {
        String getUrl = "http://" + namingServerIP.getHostAddress() + ":8080/ns/getIp/" + nodeID;
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response = restTemplate.getForEntity(getUrl, String.class);
        try {
            System.out.println("Body: " + response.getBody());
            Inet4Address ip = (Inet4Address) InetAddress.getByName(response.getBody());
            System.out.println("response: " + response.getBody());
            return ip;
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void shutDown() {
        int[] linkIds = requestLinkIds();
        int prevID = linkIds[0];
        int nextID = linkIds[1];

        // Remove from the naming server
        removeFromNS();

        sendLinkID(nextID);
        sendLinkID(prevID);

        ClientApplication.exitApplication();
    }

    @Override
    public void sendLinkID(int nodeID) {
        Inet4Address nodeIP = requestIP(nodeID);
        String postUrl = "http://" + nodeIP.getHostAddress() + ":8080/shutdown/updateID";
        System.out.println("Sending Link IDS to other Nodes----------------");
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> requestBody = new HashMap<>();

        try {


            ResponseEntity<Void> requestEntity = restTemplate.postForEntity(postUrl, requestBody, Void.class);

            if (!requestEntity.getStatusCode().is2xxSuccessful()) {
                removeFromNetwork(nodeID);
            }
        } catch (Exception e){
            System.out.println("Failed comms: removing " + nodeID);
            removeFromNetwork(nodeID);
        }
    }

    @Override
    public void receiveLinkID(int prevID, int nextID) {
        System.out.println("Updating prev and next ID : current:" + currentID + "&next:" + nextID + "&prev:" + prevID);
        this.nextID = prevID == currentID ? nextID : this.nextID;
        this.prevID = nextID == currentID ? prevID : this.prevID;
    }

    /**
     * Remove node with nodeIP from namingServer
     * <p>
     * Can also give own IP to be removed
     * </p>
     */
    @Override
    public void removeFromNS() {
        removeFromNS(currentID);
    }

    @Override
    public void removeFromNS(int removeID) {
        String postUrl = "http://" + namingServerIP.getHostAddress() + ":8080/ns/removeNode";

        RestTemplate restTemplate = new RestTemplate();

        // Create the request body
        Map<String, Object> requestBody = new HashMap<>() {{
            put("nodeID", removeID);
        }};

        System.out.println("Requesting...");

        // Send the POST request
        restTemplate.postForEntity(postUrl, requestBody, Void.class);
    }

    /**
     * Check for connection with other host
     *
     * @param nodeID
     */
    @Override
    public void ping(int nodeID) {
        Inet4Address nodeIP = requestIP(nodeID);

        String uri = "http://" + nodeIP.getHostAddress() + ":8080/test" + "?testString=test";
        System.out.println("Pinging " + uri);
        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<String> responseEntity = restTemplate.getForEntity(uri, String.class);
            System.out.println(responseEntity.getBody());

            if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                removeFromNetwork(nodeID);
            }
        } catch (Exception e) {
            System.out.println("Failed comms: removing " + nodeID);
            removeFromNetwork(nodeID);
        }

    }

    /*Failure*/

    /**
     * Reaction to a failure during communication with another node.
     *
     * @param failedID
     */
    @Override
    public void removeFromNetwork(int failedID) {
        int[] ids = requestLinkIds(failedID);

        // remove failed node from network
        removeFromNS(failedID);

        // Send prevID to nextIP and nextID to prevID
        sendLinkID(ids[0]);
        sendLinkID(ids[1]);

    }

    @Override
    public void getName() {
        System.out.println(this.hostname);
    }

    public void setNextID(int nextID) {
        this.nextID = nextID;
    }

    public void setPrevID(int prevID) {
        this.prevID = prevID;
    }

    @Override
    public void reportFilenameToNamingServer(String filename) {

        // Prepare the URL for reporting the hash value to the naming server
        String postUrl = "http://" + namingServerIP.getHostAddress() + ":8080/ns/reportFileName";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("filename", filename);
        requestBody.put("ip", currentIP.toString());

        // Make an HTTP POST request to report the hash value
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Void> responseEntity = restTemplate.postForEntity(postUrl, requestBody, Void.class);

        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            System.out.println("Hash value reported to naming server for file: " + filename);
        } else {
            System.err.println("Failed to report hash value to naming server for file: " + filename);
        }
    }


    public class ClusterMemberShipListener implements MembershipListener {
        public void memberAdded(MembershipEvent membershipEvent) {
            String s = membershipEvent.getMember().getSocketAddress().toString();
            s = s.substring(s.indexOf("/") + 1, s.indexOf(":"));
            int hash = computeHash(s);
            karibu(hash);
        }

        public void memberRemoved(MembershipEvent membershipEvent) {
            String s = membershipEvent.getMember().getSocketAddress().toString();
            s = s.substring(s.indexOf("/") + 1, s.indexOf(":"));

        }
    }
}
