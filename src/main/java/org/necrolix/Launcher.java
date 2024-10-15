package org.necrolix;

import zombie.network.GameServer;

/**
 * The main class of the server launcher, in which the basic tools are initialized.
 */
public class Launcher {
    /**
     * The main method that starts the server.
     *
     * @param args command line arguments to pass to the process.
     */
    public static void main(String[] args) {
        GameServer.main(args);
    }
}