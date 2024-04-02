package com.example.ds_project.Client;

import com.example.ds_project.namingServer.NamingServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.Inet4Address;

@SpringBootApplication
public class Client implements I_Client{
    public static void main(String[] args) {
        SpringApplication.run(NamingServer.class, args);
    }

    @Override
    public int computeHash(String s) {
        return 0;
    }

    @Override
    public void habari() {

    }

    @Override
    public void karibu() {

    }

    @Override
    public void shutDown() {

    }

    @Override
    public void sendLinkID(Inet4Address nodeIP, int startID, int otherID) {

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
