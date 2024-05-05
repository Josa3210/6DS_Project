/**
 *

package com.example.node;

import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;


/**
 * This class represents a task for monitoring file system changes in the specified Files directory.
 *
 * It implements the Runnable interface to be executed in a separate thread.


public class FileMonitor implements Runnable {

    private final String folderPath;
    private final Client client;

    public FileMonitor(Client client, String folderPath) {
        this.client = client;
        this.folderPath = folderPath;
    }

    @Override
    public void run() {

        // Create a FileAlterationObserver for the specified folder path (specified in the client)
        FileAlterationObserver observer = new FileAlterationObserver(folderPath);

        // Perform an initial scan of the directory, to check there are already files saved ..
        File[] existingFiles = new File(folderPath).listFiles();

        if (existingFiles != null) {

            for (File file : existingFiles) {
                // Calculate hash and report to naming server for existing files
                int hash = client.computeHash(file.getName());
                client.reportFilenameToNamingServer(file.getName());
            }
        }

        // Add a listener to handle file system events
        observer.addListener(new FileAlterationListenerAdaptor() {

            @Override
            public void onFileCreate(File file) {

                // Calculate hash and report to naming server when a new file is created
                String filename = file.getName();
                System.out.println("File created: " + filename);

                int hash = client.computeHash(filename);
                client.reportFilenameToNamingServer(file.getName());
            }

            @Override
            public void onFileDelete(File file) {
                // Handle file deletion event if needed
                System.out.println("File deleted: " + file.getName());
            }
        });

        // Start monitoring the directory
        while (true) {
            try {
                observer.checkAndNotify();
                Thread.sleep(1000); // Adjust sleep time as needed

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}

 **/