package com.example.ds_project.Client;

import com.example.ds_project.namingServer.NamingServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.Inet4Address;

@SpringBootApplication
public class Client implements I_Client
{

    public static void main(String[] args) {
        SpringApplication.run(NamingServer.class, args);
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

    @Override
    public void habari() {

    }

    @Override
    public void karibu() {

    }

    public int[] requestLinkIds()
    {
        // String url = namingServerBaseUrl + "/ns/giveLinkID";
        // give JSON with node ID
        // return restTemplate.getForObject(url, int[].class);
        return null;
    }

    @Override
    public void shutDown()
    {
        int[] linkIds = requestLinkIds();
        int prevID = linkIds[0];
        int nextID = linkIds[1];

        // Send the previous ID to the next node
        sendLinkID(nextNodeIP, prevID);

        // Send the next ID to the previous node
        sendLinkID(prevNodeIP, nextID);

        // Remove from the naming server
        removeFromNS();
    }

    @Override
    public void sendLinkID(Inet4Address nodeIP, int startID, int otherID) {
        // eigenlijk Update callen van de andere node
    }

    @Override
    public void receiveLinkID() {

    }

    @Override
    public void removeFromNS(Inet4Address nsIP, Inet4Address nodeIP) {

    }

    @Override
    public void ping(Inet4Address hostIP) {

    }

    @Override
    public void removeFromNetwork(Inet4Address hostIP) {

    }
}
