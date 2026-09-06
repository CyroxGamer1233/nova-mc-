package com.novamc.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;

public class JavaManager {
    private static final Logger log = LoggerFactory.getLogger(JavaManager.class);

    public static String detectJava() {
        // Check JAVA_HOME
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && !javaHome.isBlank()) {
            String exe = javaHome + File.separator + "bin" + File.separator + "java";
            if (System.getProperty("os.name").toLowerCase().contains("win")) exe += ".exe";
            if (Files.exists(Path.of(exe))) {
                log.info("Found Java at JAVA_HOME: {}", exe);
                return exe;
            }
        }
        // Check current JVM
        String javaExecutable = ProcessHandle.current().info().command().orElse(null);
        if (javaExecutable != null && Files.exists(Path.of(javaExecutable))) {
            log.info("Using current JVM: {}", javaExecutable);
            return javaExecutable;
        }
        // Fallback
        log.info("Using java from PATH");
        return "java";
    }

    public static String getJavaVersion(String javaPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(javaPath, "-version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String output = new String(p.getInputStream().readAllBytes());
            p.waitFor();
            // Parse version from output like: openjdk version "21.0.2" ...
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("version \"(\\d+)").matcher(output);
            if (m.find()) return m.group(1);
            return "unknown";
        } catch (Exception e) {
            log.warn("Could not detect Java version: {}", e.getMessage());
            return "unknown";
        }
    }

    public static boolean isJava17OrHigher(String javaPath) {
        try {
            int v = Integer.parseInt(getJavaVersion(javaPath));
            return v >= 17;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
