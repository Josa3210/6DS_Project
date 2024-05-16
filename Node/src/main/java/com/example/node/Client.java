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
import java.io.*;
import java.net.*;



import java.util.concurrent.TimeUnit;


public class Client implements I_Client {

    private static final String multicast_address = "224.2.2.5";
    private static ClusterMemberShipListener event_listener;
    RestClient restClient;
    int currentID, nextID, prevID;
    Config config;
    Map<String, Inet4Address> ipMap;
    public Inet4Address currentIP;
    private Logger logger;
    private Thread fileMonitorThread;
    Inet4Address namingServerIP;
    private Integer namingServerPort;
    private String hostname;
    public String folderPath = "Data/node/Files";
    public int numberNodes = 0;
    public boolean startFileMonitor = false;
    public boolean isReceivedFile = false;

    boolean setupCompleted = false;
    private int portNumber = 80;

    public ServerSocket serverSocket;

    public Socket clientSocket;



    public Client(String hostname) {
        try {
            event_listener = new ClusterMemberShipListener();
            this.config = createConfig();
            this.logger = new Logger(hostname); // We create a logger to keep track of the replication
            logger.load();
            fileMonitorThread = new Thread(new FileMonitor(this));
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

    public void ReceiveFile(String filepath, DataInputStream dataInputStream){

        try {

            isReceivedFile = true;
            int bytes = 0;
            FileOutputStream fileOutputStream
                    = new FileOutputStream(filepath);
            long size
                    = dataInputStream.readLong(); // read file size
            byte[] buffer = new byte[4 * 1024];
            while (size > 0
                    && (bytes = dataInputStream.read(
                    buffer, 0,
                    (int) Math.min(buffer.length, size)))
                    != -1) {
                // Here we write the file using write method
                fileOutputStream.write(buffer, 0, bytes);
                size -= bytes; // read upto file size
            }
            // Here we received file
            System.out.println("File is Received");
            fileOutputStream.close();
            this.serverSocket.close();
        }catch(IOException e1){
            System.err.println(e1.getMessage());
        }
    }


    public void SendFile(String filepath) {
        try {
            DataOutputStream dataOutputStream = new DataOutputStream(this.clientSocket.getOutputStream());
            int bytes = 0;
            // Open the File where he located in your pc
            File file = new File(filepath);
            FileInputStream fileInputStream
                    = new FileInputStream(file);
            // Here we send the File to Server
            dataOutputStream.writeLong(file.length());
            // Here we  break file into chunks
            byte[] buffer = new byte[4 * 1024];
            while ((bytes = fileInputStream.read(buffer))
                    != -1) {
                // Send the file to Server Socket
                dataOutputStream.write(buffer, 0, bytes);
                dataOutputStream.flush();
            }
            // close the file here
            System.out.println("File was sent");
            fileInputStream.close();
            clientSocket.close();
        } catch (IOException e1) {
            System.err.println(e1.getMessage());
        }
    }


    public Logger getLogger() {
        return logger;
    }

    public Thread getFileMonitorThread() {
        return fileMonitorThread;
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

        // if  a node gets removed from the namingserver, the files replicated on this node should be
        // moved to  the previous node, that will become the owner of the files
        // We first remove the current entry with the original IP



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
    public void reportFilenameToNamingServer(String filename,String filePath, int operation) {

        System.out.println("namingserver IP: " + namingServerIP.getHostAddress());
        System.out.println("current IP: " + currentIP.getHostAddress());

        // Prepare the URL for reporting the hash value to the naming server
        String postUrl = "http://" + namingServerIP.getHostAddress() + ":8080/ns/reportFileName";


        Map<String, Object> requestBody = new HashMap<>();

        System.out.println("operation: " + operation);

        requestBody.put("filename", filename);
        requestBody.put("filepath", filePath);
        System.out.println(filePath);
        requestBody.put("ip", currentIP.getHostAddress());
        requestBody.put("operation", operation);

        // Make an HTTP POST request to report the hash value
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Void> responseEntity = restTemplate.postForEntity(postUrl, requestBody, Void.class);

        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            System.out.println("Hash value: " + filename + " correctly handled by server");
        } else {
            System.err.println("Failed to report hash value to naming server for file: " + filename);
        }
    }

    public class ClusterMemberShipListener implements MembershipListener {
        public void memberAdded(MembershipEvent membershipEvent) {
            if (startFileMonitor){
                try {
                    TimeUnit.SECONDS.sleep(20);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                startFileMonitor = false;
                Thread filemonitorthread = getFileMonitorThread();
                filemonitorthread.start();
            }
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
