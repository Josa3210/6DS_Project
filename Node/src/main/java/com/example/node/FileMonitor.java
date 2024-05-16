

package com.example.node;

import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;
import java.io.IOException;
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
            throw new RuntimeException(e);
        }
        System.out.println("folder created: " + client.folderPath);

        // Create a FileAlterationObserver for the specified folder path (specified in the client)
        FileAlterationObserver observer = new FileAlterationObserver(client.folderPath);

        // Add a listener to handle file system events
        observer.addListener(new FileAlterationListenerAdaptor() {

            @Override
            public void onFileCreate(File file) {

                if (!client.isReceivedFile){
                    String filename = file.getName();
                    System.out.println("\nFile created: " + filename);
                    client.reportFilenameToNamingServer(file.getName(),file.getPath(),1); // Operation 1 --> file CREATE
                    client.isReceivedFile = false;
                }
            }

            @Override
            public void onFileDelete(File file) {
                String filename = file.getName();
                System.out.println("\nFile deleted: " + file.getName());

                // We remove the file from the logger
                Logger logger = client.getLogger();
                logger.load();
                int hash = client.computeHash(filename);
                logger.remove(hash);
                client.reportFilenameToNamingServer(file.getName(),file.getPath(),2); // Operation 2 --> file DELETE
            }
        });

        while (true) {  // Start monitoring the directory

            try {
                observer.checkAndNotify();
                Thread.sleep(1000); // Adjust sleep time as needed

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}