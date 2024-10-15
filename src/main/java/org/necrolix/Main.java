package org.necrolix;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Main class for running the Project Zomboid server as a separate application.
 */
public class Main {
    private static final List<String> classPaths = new ArrayList<>(); // List of all folders with classes and jar files
    private static final List<String> nativePaths = new ArrayList<>(); // List of all native libraries

    /**
     * The main method that starts the application.
     *
     * @param args command line arguments to pass to the process.
     * @throws Exception if a runtime error occurs.
     */
    public static void main(String[] args) throws Exception {
        String jdkVersion = System.getProperty("java.version");
        String osName = System.getProperty("os.name");
        boolean isAmd64 = System.getProperty("os.arch").equalsIgnoreCase("amd64");

        File coreFile = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());

        // Checking the JDK version
        checkJDK(jdkVersion);

        // Checking the bit and adding native libraries
        addNatives(coreFile, osName, isAmd64);

        // Adding to classpath
        addClassPaths(coreFile);

        // Starting a new process with all settings
        List<String> command = new ArrayList<>();
        command.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        command.add("-Djava.awt.headless=true");
        command.add("-Dzomboid.steam=1");
        command.add("-Dzomboid.znetlog=1");
        command.add(isAmd64 ? "-XX:+UseZGC" : "-XX:+UseG1GC");
        command.add("-Djava.library.path=" + String.join(";", nativePaths));
        command.add("-cp");
        command.add(String.join(";", classPaths));
        command.add(Launcher.class.getName());
        Collections.addAll(command, args);

        System.out.printf("[#] Starting the server | OS: %s | Arch: %s | JDK: %s | Core: %s%n", osName, isAmd64 ? "x64" : "x32", jdkVersion, coreFile.getName().replace(".jar", ""));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.inheritIO();
        Process process = processBuilder.start();

        int exitCode = process.waitFor();
        System.exit(exitCode);
    }

    /**
     * Adds class paths to the classPaths list.
     *
     * @param coreFile the application's core JAR file.
     */
    private static void addClassPaths(File coreFile) {
        File javaFolder = new File(coreFile.getParentFile(), "java");
        if (!javaFolder.exists()) {
            System.out.println("[!] Directory 'java' not found! Check the location of the core file, it should be in the root folder of the server.");
            System.exit(-1);
        }
        classPaths.add(coreFile.getAbsolutePath());
        classPaths.add(javaFolder.getAbsolutePath());

        File[] files = javaFolder.listFiles();
        if (files != null) {
            classPaths.addAll(Arrays.stream(files)
                    .filter(file -> file.isFile() && file.getName().endsWith(".jar"))
                    .map(File::getAbsolutePath)
                    .toList());
        } else {
            System.out.println("[!] Unable to list files in 'java' directory.");
            System.exit(-1);
        }
    }

    /**
     * Adds native library paths to the nativePaths list.
     *
     * @param coreFile the application's core JAR file.
     * @param osName name of the operating system.
     * @param isAmd64 flag indicating whether the architecture is 64-bit.
     */
    private static void addNatives(File coreFile, String osName, boolean isAmd64) {
        nativePaths.add(coreFile.getParent());
        nativePaths.add(Paths.get(coreFile.getParent(), "natives").toString());

        if (osName.toLowerCase().contains("win")) {
            nativePaths.add(Paths.get(coreFile.getParent(), isAmd64 ? "natives/win64/" : "natives/win32/").toString());
        } else if (osName.toLowerCase().contains("linux")) {
            nativePaths.add(Paths.get(coreFile.getParent(), isAmd64 ? "linux64/" : "linux32/").toString());
        } else {
            System.out.printf("[!] Sorry, your system %s (%s) is not currently supported.%n", osName, isAmd64 ? "x64" : "x32");
            System.exit(-1);
        }
    }

    /**
     * Checks if the JDK version meets the minimum requirements.
     *
     * @param jdkVersion the JDK version obtained from system properties.
     */
    private static void checkJDK(String jdkVersion) {
        if (Integer.parseInt(jdkVersion.toLowerCase().split("\\.")[0]) < 17) {
            System.out.printf("[!] Your JDK version (%s) is out of date. Please update to at least JDK 17.%n", jdkVersion);
            System.out.printf("[!] Download JDK from: https://www.oracle.com/java/technologies/downloads/%n");
            System.exit(-1);
        }
    }
}