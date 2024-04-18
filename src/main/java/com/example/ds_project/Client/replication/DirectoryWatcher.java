package com.example.ds_project.Client.replication;

import java.io.IOException;
import java.nio.file.*;

public class DirectoryWatcher
{
    String directory;

    /**
     * When creating a directory watcher, a directory will be monitored periodically to check for
     * created and deleted files
     * @param directory the directory to be watched
     */
    public DirectoryWatcher(String directory)
    {
        this.directory = directory;
        monitorDirectory(); // TODO : Make it monitor periodically? or not?
    }

    /**
     * Monitors the directory.
     * If a file is created, it will replicate the file to the corresponding node
     * If a file is deleted, it will delete the replicated files from the file owner
     */
    private void monitorDirectory()
    {
        Path dirToWatch = Paths.get(directory);
        try (WatchService watcher = FileSystems.getDefault().newWatchService())
        {
            // Check for events CREATE and DELETE
            dirToWatch.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);

            WatchKey key;
            while ((key = watcher.take()) != null)
            {
                for (WatchEvent<?> event : key.pollEvents())
                {
                    System.out.println("Event kind:" + event.kind() + ". File affected: " + event.context() + ".");

                    // If a file gets created
                    if(event.kind() == StandardWatchEventKinds.ENTRY_CREATE)
                    {
                        // Replicate the file
                        System.out.println("File created: " + event.context() + ".");
                        System.out.println("Implement replicate stuff...");
                    }
                    // If a file gets removed
                    else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE)
                    {
                        // Delete also from the replicated files of the file owner
                        System.out.println("File deleted: " + event.context() + ".");
                        System.out.println("Implement deleting stuff...");
                    }

                }
                key.reset();
            }
        }
        catch (IOException | InterruptedException e) {throw new RuntimeException(e);}
    }
}