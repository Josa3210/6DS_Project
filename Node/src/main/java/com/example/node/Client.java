package com.example.node;

import com.example.node.Agents.SyncAgent;
import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.cluster.MembershipListener;
import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spi.exception.RestClientException;
import jakarta.annotation.PostConstruct;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.FileNotFoundException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.*;
import java.io.*;
import java.net.*;


public class Client implements I_Client {

    private static final String multicast_address = "224.2.2.5";
    private static ClusterMemberShipListener event_listener;
    private final Config config;
    private final String currentIP;
    private final String hostname;
    private final Logger logger;
    private final Thread fileMonitorThread;
    public String folderPath = "Data/node/Files";
    public int numberNodes = 0;
    public boolean isReceivedFile = false;
    public ServerSocket serverSocket;
    public Socket clientSocket;
    public boolean isReplicatedFile;
    int currentID, nextID, prevID;
    private String namingServerIP;
    private Integer namingServerPort;
    private SyncAgent syncAgent;
    private List<NodeFileEntry> fileList;

    /**
     * Constructor of the client
     *
     * @param hostname the name of the client
     */
    public Client(String hostname) {
        try {
            event_listener = new ClusterMemberShipListener();
            this.config = createConfig();
            this.hostname = hostname;
            this.currentIP = Inet4Address.getByName(Inet4Address.getLocalHost().getHostAddress()).getHostAddress();
            this.fileList = new ArrayList<>();
            // We make a new logger file that keeps track of changes in the 'Files' map
            this.logger = new Logger(hostname); // We create a logger to keep track of the replication
            fileMonitorThread = new Thread(new FileMonitor(this));
            fileMonitorThread.start();

            //Sync Agent

            System.out.println("^^^^Debugging Run Sync Agent in Client");
            //syncAgent = new SyncAgent(this);
            //syncAgent.run();
        } catch (FileNotFoundException | UnknownHostException e) {
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

    public void ReceiveFile(String filepath, DataInputStream dataInputStream) {

        try {

            isReceivedFile = true;
            int bytes = 0;
            FileOutputStream fileOutputStream = new FileOutputStream(filepath);
            long size = dataInputStream.readLong(); // read file size
            byte[] buffer = new byte[4 * 1024];
            while (size > 0 && (bytes = dataInputStream.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                // Here we write the file using write method
                fileOutputStream.write(buffer, 0, bytes);
                size -= bytes; // read upto file size
            }
            // Here we received file
            System.out.println("^^^^File is Received");
            fileOutputStream.close();
            this.serverSocket.close();
        } catch (IOException e1) {
            System.err.println(e1.getMessage());
        }

    }


    public void SendFile(String filepath) {
        try {
            DataOutputStream dataOutputStream = new DataOutputStream(this.clientSocket.getOutputStream());
            int bytes = 0;
            // Open the File where he located in your pc
            File file = new File(filepath);
            FileInputStream fileInputStream = new FileInputStream(file);
            // Here we send the File to Server
            dataOutputStream.writeLong(file.length());
            // Here we  break file into chunks
            byte[] buffer = new byte[4 * 1024];
            while ((bytes = fileInputStream.read(buffer)) != -1) {
                // Send the file to Server Socket
                dataOutputStream.write(buffer, 0, bytes);
                dataOutputStream.flush();
            }
            // close the file here
            System.out.println("^^^^File was sent");
            fileInputStream.close();
            clientSocket.close();
        } catch (IOException e1) {
            System.err.println(e1.getMessage());
        }
    }

    public Thread getFileMonitorThread() {
        return fileMonitorThread;
    }

    /**
     * Writes information about the client to the console:
     * - Current, next and previous ID
     * - IP and Port of the naming server
     */
    @Override
    public void printLinkIds() {
        System.out.println(">> Client ID's");
        System.out.println("* Current ID: " + this.currentID);
        System.out.println("* Next ID: " + this.nextID);
        System.out.println("* Previous ID: " + this.prevID);
        System.out.println("* IP + port NS: " + this.namingServerIP + " + " + this.namingServerPort);
    }

    /**
     * Post Constructor of the client
     */
    @PostConstruct
    public void init() throws InterruptedException {
        habari();
        this.currentID = computeHash(hostname);
        this.nextID = Integer.MAX_VALUE;
        this.prevID = Integer.MIN_VALUE;

        //Thread.sleep(2000);  // Wait for the network to stabilize
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
    @Override
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
        System.out.println("Joining hazelcast instance");
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(this.config); // Creates a new Hazelcast instance with the provided configuration.
    }

    /**
     * This method will set the previous and next node ID's
     * and ask for the naming server to add the client
     *
     * @param nrNodes          the size of the cluster in the network
     * @param namingServerIP   the IP of the naming server
     * @param namingServerPort the port of the naming server
     */
    public void setupClient(int nrNodes, String namingServerIP, int namingServerPort) {
        this.namingServerIP = namingServerIP;
        this.namingServerPort = namingServerPort;

        addNameToNS();

        int[] ids = requestLinkIds();
        nextID = nrNodes == 1 ? currentID : ids[1];
        prevID = nrNodes == 1 ? currentID : ids[0];

        sendLinkID(nextID);
        sendLinkID(prevID);
    }

    /**
     * This method sends a Post REST request to the naming server
     * asking to add itself to the network
     */
    private void addNameToNS() {
        String postUrl = "http://" + namingServerIP + ":" + namingServerPort + "/ns/addNode";

        System.out.println(">> Sending REST Post (addNameToNS)");
        System.out.println("* Post url: " + postUrl);
        System.out.println("* IP + Name: " + currentIP + " + " + hostname);

        RestTemplate restTemplate = new RestTemplate();

        // Create the request body
        Map<String, Object> requestBody = new HashMap<>() {{
            put("ip", currentIP);
            put("name", hostname);
        }};

        System.out.println("Requesting...");

        // Send the POST request
        restTemplate.postForEntity(postUrl, requestBody, Void.class);
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
        if ((this.currentID < hash) && (hash < this.nextID)) this.nextID = hash;
        else if ((this.prevID < hash) && (hash < this.currentID)) this.prevID = hash;
    }

    /**
     * Returns the previous and next ID of the client itself
     *
     * @return previous ID (int[0]) and next ID (int[1])
     */
    @Override
    public int[] requestLinkIds() {
        return requestLinkIds(currentID);
    }

    /**
     * Returns the previous and next ID of the requested client
     *
     * @param requestID the ID of the requested client
     * @return previous ID (int[0]) and next ID (int[1])
     */
    @Override
    public int[] requestLinkIds(int requestID) {
        String getUrl = "http://" + namingServerIP + ":" + namingServerPort + "/ns/giveLinkID/" + requestID;
        RestTemplate restTemplate = new RestTemplate();

        System.out.println(">> Sending REST Get (requestLinkIds)");
        System.out.println("* Get URL: " + getUrl);

        ResponseEntity<int[]> response = restTemplate.getForEntity(getUrl, int[].class);
        return response.getBody();
    }

    /**
     * Returns the IP of a node using the ID
     *
     * @param nodeID the ID of the requested node
     * @return the IP of the requested node in a string
     */
    @Override
    public String requestIP(int nodeID) {
        String getUrl = "http://" + namingServerIP + ":" + namingServerPort + "/ns/getIp/" + nodeID;

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response = restTemplate.getForEntity(getUrl, String.class);

        return response.getBody();
    }

    /**
     * This method will shut the client down by removing itself from the naming server
     * and sending the previous and next ID to the corresponding nodes
     */
    @Override
    public void shutDown() {
        int[] linkIds = requestLinkIds();
        int prevID = linkIds[0];
        int nextID = linkIds[1];

        // Get IP of previous node
        System.out.println("^^^^Getting IP from previous node");
        RestTemplate restTemplate = new RestTemplate();
        String getUrl = "http://" + namingServerIP + ":8080/ns/getIp/" + getPrevID();
        ResponseEntity<String> response2 = restTemplate.getForEntity(getUrl, String.class);
        String newIP = response2.getBody();
        System.out.println("* IP: " + newIP);

        // Send file to previous node
        System.out.println("^^^^Sending files to previous node");
        String url = "http://" + newIP + ":8080/shutdown/sendFiles";
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("originalIP", getCurrentIP());
        requestBody.put("files", logger.getFileArray().toString());
        restTemplate.postForEntity(url, requestBody, Void.class);

        // Remove from the naming server
        System.out.println("^^^^Removing itself from NS");
        removeFromNS();

        // Sending new IDs to the prev and next node
        System.out.println("^^^^Passing on the ids");
        sendLinkID(nextID);
        sendLinkID(prevID);

        // Exiting application
        ClientApplication.exitApplication();
    }

    /**
     * Requests a node to update it's previous and next ID
     *
     * @param nodeID the requested nodes ID
     */
    @Override
    public void sendLinkID(int nodeID) {
        String nodeIP = requestIP(nodeID);
        String postUrl = "http://" + nodeIP + ":" + namingServerPort + "/shutdown/updateID";
        System.out.println(">> Sending REST Post (sendLinkID)");

        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> requestBody = new HashMap<>();

        try {
            ResponseEntity<Void> requestEntity = restTemplate.postForEntity(postUrl, requestBody, Void.class);
            if (!requestEntity.getStatusCode().is2xxSuccessful()) removeFromNetwork(nodeID);
        } catch (Exception e) {
            removeFromNetwork(nodeID);
        }
    }

    /**
     * This method will update the client's previous and next ID
     *
     * @param prevID the previous ID or current ID
     * @param nextID the next ID or current ID
     */
    @Override
    public void receiveLinkID(int prevID, int nextID) {
        System.out.println(">> Updating prev and next ID : current:" + currentID + ", next ID: " + nextID + ", prev ID: " + prevID);
        this.nextID = prevID == currentID ? nextID : this.nextID;
        this.prevID = nextID == currentID ? prevID : this.prevID;

        syncAgent.setActive(nextID != currentID);
    }

    /**
     * Remove node with nodeIP from namingServer
     * <p>
     * Can also give own IP to be removed
     * </p>
     */
    @Override
    public void removeFromNS() {
        // Remove itself from the naming server
        removeFromNS(currentID);
    }


    @Override
    public void removeFromNS(int removeID) {
        String postUrl = "http://" + namingServerIP + ":" + namingServerPort + "/ns/removeNode";

        System.out.println("^^^^Sending REST Post (removeFromNS)");
        System.out.println("* Post URL: " + postUrl);
        System.out.println("* Node ID: " + removeID);

        RestTemplate restTemplate = new RestTemplate();

        // Create the request body
        Map<String, Object> requestBody = new HashMap<>() {{
            put("nodeID", removeID);
        }};

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
        String nodeIP = requestIP(nodeID);

        String uri = "http://" + nodeIP + ":8080/test" + "?testString=test";
        System.out.println(">> Pinging: " + uri);
        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<String> responseEntity = restTemplate.getForEntity(uri, String.class);
            System.out.println("* Response: " + responseEntity.getBody());

            if (!responseEntity.getStatusCode().is2xxSuccessful()) removeFromNetwork(nodeID);
        } catch (Exception e) {
            removeFromNetwork(nodeID);
        }
    }


    /**
     * Reaction to a failure during communication with another node.
     *
     * @param failedID
     */
    @Override
    public void removeFromNetwork(int failedID) {
        System.out.println(">> Removing client from network");

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

    public String getHostname() {
        return hostname;
    }

    public Logger getLogger() {
        return logger;
    }

    public String getCurrentIP() {
        return currentIP;
    }

    public SyncAgent getSyncAgent() {
        return syncAgent;
    }

    public int getNextID() {
        return nextID;
    }

    public void setNextID(int nextID) {
        this.nextID = nextID;
    }

    public List<NodeFileEntry> getFileList() {
        return fileList;
    }

    public void setFileList(List<NodeFileEntry> newList) {
        this.fileList = newList;
    }

    @Override
    public void deleteReplicatedFile(String filename, String filePath) {
        // Prepare url
        String getUrl = "http://" + namingServerIP + ":8080/ns/getLocation/" + filename;

        // Get the ip of replicated node
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String[]> response = restTemplate.getForEntity(getUrl, String[].class);
        String[] locationString = response.getBody();
        String replicatedIP = locationString[1];

        if (Objects.equals(getCurrentIP(), replicatedIP)) {
            getUrl = "http://" + namingServerIP + ":8080/ns/getIp/" + getNextID();
            ResponseEntity<String> response2 = restTemplate.getForEntity(getUrl, String.class);
            replicatedIP = response2.getBody();
        }
        String postUrl = "http://" + replicatedIP + ":8080/deleteReplicatedFile";

        // Create the request body
        Map<String, Object> requestBody = new HashMap<>();
        HttpHeaders headers = new HttpHeaders();

        // Create the request entity with headers and body
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            // Send the POST request with the replicated IP --> it becomes the new 'original' IP
            requestBody.put("filename", filename);
            requestBody.put("filepath", filePath);

            ResponseEntity<Void> responseEntity = restTemplate.postForEntity(postUrl, requestEntity, Void.class);
            HttpStatusCode statusCode = responseEntity.getStatusCode();

            if (statusCode == HttpStatus.OK) {
                System.out.println("Succesfully removed file: " + filePath + "\\" + filename + " from " + replicatedIP);
            } else {
                System.err.println("Sending node list failed with status code: " + statusCode);
            }
        } catch (RestClientException e) {
            System.err.println("Failed to send node list to " + replicatedIP + ": " + e.getMessage());
        }
    }

    @Override
    public void createReplicatedFile(String filename, String filePath) {
        // Prepare the URL for reporting the hash value to the naming server
        String getUrl = "http://" + namingServerIP + ":8080/ns/getLocation/" + filename;

        // Get the ip of replicated node
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String[]> response = restTemplate.getForEntity(getUrl, String[].class);
        String[] locationString = response.getBody();
        String replicatedIP = locationString[1];

        if (Objects.equals(getCurrentIP(), replicatedIP)) {
            getUrl = "http://" + namingServerIP + ":8080/ns/getIp/" + getNextID();
            ResponseEntity<String> response2 = restTemplate.getForEntity(getUrl, String.class);
            replicatedIP = response2.getBody();
        }

        String postUrl = "http://" + replicatedIP + ":8080/isReplicatedNode";
        HttpHeaders headers = new HttpHeaders();
        // Create the request body
        Map<String, Object> requestBody = new HashMap<>();
        // Create the request entity with headers and body
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            // Send the POST request
            requestBody.put("original ip", getCurrentIP());
            requestBody.put("original id", getCurrentID());
            requestBody.put("filepath", filePath);

            System.out.println("^^^^Sending request to: " + postUrl);
            ResponseEntity<Void> responseEntity = restTemplate.postForEntity(postUrl, requestEntity, Void.class);
            HttpStatusCode statusCode = responseEntity.getStatusCode();

            System.out.println("^^^^Putting new owner in logger");
            System.out.println("ID: " + locationString[0] + ", IP: " + locationString[1]);
            logger.putOwner(computeHash(filename), Integer.parseInt(locationString[0]), locationString[1]);

            if (statusCode == HttpStatus.OK) {
                System.out.println("Successfully replicated file (" + filename + ") to " + replicatedIP);

            } else {
                System.err.println("Sending node list failed with status code: " + statusCode);
            }
        } catch (RestClientException e) {
            System.err.println("Failed to send node list to " + replicatedIP + ": " + e.getMessage());
        }
    }

    public void receiveReplicatedFile(Inet4Address originalIP, int originalId, String filepath) throws IOException {
        // Create socket for TCP
        this.serverSocket = new ServerSocket(5000);

        // Prepare Post
        String url = "http://" + originalIP.getHostAddress() + ":8080/OpenTCPConnection";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> requestBody = new HashMap<>();

        // Set IP to which the node needs to send
        requestBody.put("replicated ip", InetAddress.getLocalHost().getHostAddress());

        // Sending request
        System.out.println("^^^^Sending request to: " + url);
        restTemplate.postForEntity(url, requestBody, String.class);

        // Accept connection with other node
        this.clientSocket = this.serverSocket.accept();
        DataInputStream dataInputStream = new DataInputStream(this.clientSocket.getInputStream());

        // Prepare request
        url = "http://" + originalIP.getHostAddress() + ":8080/StartFileTransfer";
        requestBody.clear();
        requestBody.put("filepath", filepath);

        // Send request
        System.out.println("^^^^Sending request to: " + url);
        restTemplate.postForEntity(url, requestBody, Void.class);

        // Get ready to receive file
        this.ReceiveFile(filepath, dataInputStream);

        // Put in logger
        String filename = String.valueOf(Paths.get(filepath).getFileName());
        int hash = computeHash(filename);
        logger.put(hash, filename);
        logger.putOwner(hash, getCurrentID(), getCurrentIP());
        logger.putOriginal(hash, originalId, originalIP.getHostAddress());
    }

    @Override
    public int getCurrentID() {
        return currentID;
    }

    public int getPrevID() {
        return prevID;
    }

    public void setPrevID(int prevID) {
        this.prevID = prevID;
    }

    public class ClusterMemberShipListener implements MembershipListener {
        public void memberAdded(MembershipEvent membershipEvent) {
            numberNodes++;
            String s = membershipEvent.getMember().getSocketAddress().toString();
            s = s.substring(s.indexOf("/") + 1, s.indexOf(":"));
            int hash = computeHash(s);
            karibu(hash);

        }

        public void memberRemoved(MembershipEvent membershipEvent) {
            numberNodes--;
            String s = membershipEvent.getMember().getSocketAddress().toString();
            s = s.substring(s.indexOf("/") + 1, s.indexOf(":"));

        }
    }
}
