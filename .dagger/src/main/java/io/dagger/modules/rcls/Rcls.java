package io.dagger.modules.rcls;

import io.dagger.client.Container;
import io.dagger.client.Directory;
import io.dagger.client.File;
import io.dagger.client.exception.DaggerQueryException;
import io.dagger.module.annotation.Function;
import io.dagger.module.annotation.Object;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static io.dagger.client.Dagger.dag;

/**
 * RCLS build module
 */
@Object
public class Rcls {

    /**
     * Build frontend application
     *
     * @param source Source code directory
     * @return Directory containing the built frontend dist
     */
    @Function
    public Directory buildFrontend(Directory source) {
        return dag().container()
                    .from("node:23-bookworm-slim")
                    .withDirectory(
                            "/app",
                            source
                    )
                    .withWorkdir("/app/rcls-frontend")
                    .withExec(List.of(
                            "npm",
                            "ci"
                    ))
                    .withExec(List.of(
                            "npm",
                            "run",
                            "build"
                    ))
                    .directory("/app/rcls-frontend/dist");
    }

    /**
     * Build backend JAR without frontend assets
     *
     * @param source Source code directory
     */
    @Function
    public Container buildBackend(Directory source) {

        return dag().container()
                    .from("eclipse-temurin:21-jdk-jammy")
                    .withDirectory(
                            "/app",
                            source
                    )
                    .withWorkdir("/app")
                    .withExec(List.of(
                            "chmod",
                            "+x",
                            "./gradlew"
                    ))
                    .withMountedCache(
                            "/root/.gradle",
                            dag().cacheVolume("gradle-cache")
                    )
                    .withExec(List.of(
                            "./gradlew",
                            "clean",
                            "build",
                            "--no-daemon"
                    ));
    }

    /**
     * Package application by injecting frontend into backend JAR
     *
     * @param source  Source code directory
     * @param version Version tag (e.g., "1.0.0" or "v1.0.0")
     * @return File containing the final packaged JAR
     */
    @Function
    public File packageApp(
            Directory source,
            String version
    ) {
        String normalizedVersion = version.startsWith("v")
                ? version.substring(1)
                : version;

        Directory frontendDist = buildFrontend(source);
        //Populate Gradle Cache
        buildBackend(source);

        return dag().container()
                    .from("eclipse-temurin:21-jdk-jammy")
                    .withDirectory(
                            "/app",
                            source
                    )
                    .withExec(List.of(
                            "rm",
                            "-rf",
                            "/app/rcls-backend/src/main/resources/static"
                    ))
                    .withExec(List.of(
                            "mkdir",
                            "-p",
                            "/app/rcls-backend/src/main/resources/static"
                    ))
                    .withDirectory(
                            "/app/rcls-backend/src/main/resources/static",
                            frontendDist
                    )
                    .withWorkdir("/app")
                    .withExec(List.of(
                            "chmod",
                            "+x",
                            "./gradlew"
                    ))
                    .withMountedCache(
                            "/root/.gradle",
                            dag().cacheVolume("gradle-cache")
                    )
                    .withExec(List.of(
                            "./gradlew",
                            "bootJar",
                            "--no-daemon",
                            "-Pversion=" + normalizedVersion
                    ))
                    .file("/app/rcls-backend/build/libs/RCLS-" + normalizedVersion + ".jar");
    }

    /**
     * Test backend
     *
     * @param source Source code directory
     * @return Container with test results
     */
    @Function
    public Container testBackend(Directory source) {
        return dag().container()
                    .from("eclipse-temurin:21-jdk-jammy")
                    .withDirectory(
                            "/app",
                            source
                    )
                    .withWorkdir("/app")
                    .withExec(List.of(
                            "chmod",
                            "+x",
                            "./gradlew"
                    ))
                    .withMountedCache(
                            "/root/.gradle",
                            dag().cacheVolume("gradle-cache")
                    )
                    .withExec(List.of(
                            "./gradlew",
                            "clean",
                            "test",
                            "--no-daemon"
                    ));
    }

    /**
     * Run all tests in parallel
     *
     * @param source Source code directory
     */
    @Function
    public void testAll(Directory source) throws ExecutionException, DaggerQueryException, InterruptedException {
        Container backendTests = testBackend(source);

        String backendResult = backendTests.stdout();

        return;
    }

    /**
     * Run full CI pipeline: test all, then package
     *
     * @param source  Source code directory
     * @param version Version tag (e.g., "1.0.0" or "v1.0.0")
     * @return File containing the final packaged JAR
     */
    @Function
    public File pipeline(
            Directory source,
            String version
    ) throws ExecutionException, DaggerQueryException, InterruptedException {
        testAll(source);
        return packageApp(
                source,
                version
        );
    }

    /**
     * Build for local development using dev-SNAPSHOT version
     *
     * @param source Source code directory
     * @return File containing the development JAR
     */
    @Function
    public File buildDev(Directory source) {
        return packageApp(
                source,
                "dev-SNAPSHOT"
        );
    }

    /**
     * Create a multi-platform build directory
     *
     * @param source  Source code directory
     * @param version Version tag (e.g., "1.0.0" or "v1.0.0")
     * @return Directory containing the packaged JAR
     */
    @Function
    public Directory buildMultiPlatform(
            Directory source,
            String version
    ) {
        String normalizedVersion = version.startsWith("v")
                ? version.substring(1)
                : version;
        File jar = packageApp(
                source,
                normalizedVersion
        );

        return dag().directory()
                    .withFile(
                            "RCLS-" + normalizedVersion + ".jar",
                            jar
                    );
    }

    /**
     * Verify the packaged JAR can be executed
     *
     * @param source  Source code directory
     * @param version Version tag (e.g., "1.0.0" or "v1.0.0")
     * @return String containing verification output
     */
    @Function
    public String verify(
            Directory source,
            String version
    ) throws ExecutionException, DaggerQueryException, InterruptedException {
        String normalizedVersion = version.startsWith("v")
                ? version.substring(1)
                : version;
        File jar = packageApp(
                source,
                normalizedVersion
        );

        String output = dag().container()
                             .from("eclipse-temurin:21-jre-jammy")
                             .withFile(
                                     "/app/RCLS.jar",
                                     jar
                             )
                             .withExec(List.of(
                                     "java",
                                     "-jar",
                                     "/app/RCLS.jar",
                                     "--help"
                             ))
                             .stdout();

        return "JAR verification successful:\n" + output;
    }

    /**
     * Get build information
     *
     * @param version Version tag (e.g., "1.0.0" or "v1.0.0")
     * @return String containing build information
     */
    @Function
    public String info(String version) {
        String normalizedVersion = version.startsWith("v")
                ? version.substring(1)
                : version;
        return String.format(
                "RCLS Build Info:\n" +
                "Version: %s\n" +
                "Artifact: rcls-backend-%s.jar\n" +
                "Build Steps:\n" +
                "  1. buildFrontend() - Compiles React/Node frontend\n" +
                "  2. buildBackend()  - Compiles Spring Boot backend\n" +
                "  3. packageApp()    - Injects frontend into backend JAR via Gradle\n" +
                "Build command: dagger call package-app --source=. --version=%s\n" +
                "Full pipeline:  dagger call pipeline --source=. --version=%s\n",
                normalizedVersion,
                normalizedVersion,
                version,
                version
        );
    }
}