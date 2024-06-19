package com.example.node;

public class NodeFileEntry
{
    private String filename;
    private boolean isLocked;

    public NodeFileEntry(String filename)
    {
        this.filename = filename;
        this.isLocked = false;
    }

    // No-argument constructor
    public NodeFileEntry() {
        // Default constructor
    }

    public String getFilename() {
        return filename;
    }
    public void setFilename(String filename) {
        this.filename = filename;
    }
    public boolean isLocked() {
        return isLocked;
    }
    public void setLocked(boolean locked) {
        isLocked = locked;
    }
}
