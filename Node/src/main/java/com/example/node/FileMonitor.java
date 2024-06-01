

package com.example.node;

import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;


/**
 * This class represents a task for monitoring file system changes in the specified Files directory.
 *
 * It implements the Runnable interface to be executed in a separate thread.

**/

public class FileMonitor implements Runnable {

    Client client;

    public FileMonitor(Client client) {
        this.client = client;

    }

    public void run() {

        Path path = Paths.get(client.folderPath);
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }
        System.out.println("folder created: " + client.folderPath);

        // Create a FileAlterationObserver for the specified folder path (specified in the client)
        FileAlterationObserver observer = new FileAlterationObserver(client.folderPath);

        // Add a listener to handle file system events
        observer.addListener(new FileAlterationListenerAdaptor() {

            @Override
            public void onFileCreate(File file) {

                if (!client.isReceivedFile) {
                    String filename = file.getName();

                    System.out.println("\nFile created: " + filename);
                    Logger logger = client.getLogger();
                    logger.load();
                    int hash = client.computeHash(filename);
                    logger.put(hash, client.currentIP);
                    logger.save();
                    client.reportFilenameToNamingServer(file.getName(), file.getPath(), 1); // Operation 1 --> file CREATE
                } else
                    client.isReceivedFile = false;
            }

            @Override
            public void onFileDelete(File file) {

                String filename = file.getName();
                String filepath = file.getPath();
                System.out.println("filepath: " + filepath);

                if (!filename.endsWith(".swp")) { // we don't look at temporary files

                    // We remove the file from the logger
                    Logger logger = client.getLogger();
                    logger.load();
                    int hash = client.computeHash(filename);
                    String originalIP = logger.get(hash).getHostAddress();
                    String  currentIP = client.currentIP.getHostAddress();

                    if (originalIP.equals(currentIP)) // we check if the original IP of the file = current IP

                        // if this is the case, the current IP is the IP where the file got downloaded, so we need to make
                        // sure that the naming server gets noted about this so it can remove the replicated nodes too.
                        client.reportFilenameToNamingServer(file.getName(), file.getPath(), 2); // Operation 2 --> file DELETE on replicated node

                    logger.remove(hash); // We remove the hash from the logger.
                }
            }
        });

        while (true) {  // Start monitoring the directory

            try {
                observer.checkAndNotify();
                Thread.sleep(1000); // Adjust sleep time as needed

            } catch (InterruptedException e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        }

    }
}