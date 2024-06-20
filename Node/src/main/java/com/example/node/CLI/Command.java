package com.example.node.CLI;

public enum Command {
    NONE(0), PING(1), SHUTDOWN(0), GETLINKIDS(0), GETNAME(0), CREATE(1), DELETE(1), PRINTLOGGER(0), GETFILE(1);

    private String[] args;
    private int nrArgs;

    Command(int nrArgs) {
        this.nrArgs = nrArgs;
        this.args = new String[nrArgs];
    }

    public String[] getArgs() {
        return this.args;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }

    public int getNrArgs() {
        return nrArgs;
    }
}
