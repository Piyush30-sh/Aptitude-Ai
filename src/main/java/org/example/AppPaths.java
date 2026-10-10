package org.example;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

final class AppPaths {
    private AppPaths() {
    }

    static File projectFile(String relativePath) {
        return projectRoot().resolve(relativePath).toFile();
    }

    static String resourcePath(String resourceName) {
        return projectFile("src/main/resources").toPath().resolve(resourceName).toAbsolutePath().toString();
    }

    private static Path projectRoot() {
        try {
            URI location = AppPaths.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path codeLocation = Paths.get(location);
            Path root = findProjectRoot(codeLocation);
            if (root != null) {
                return root;
            }
        } catch (Exception ignored) {
        }

        Path workingDirectory = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        Path root = findProjectRoot(workingDirectory);
        return root != null ? root : workingDirectory;
    }

    private static Path findProjectRoot(Path location) {
        Path current = Files.isDirectory(location) ? location : location.getParent();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("pom.xml"))) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }
}