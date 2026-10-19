# Development

Make sure your sub-projects are up-to-date: `git submodule update --init --recursive`

Run Gradle 8.7 on Java 17 or 21 and provide a Java 8 toolchain with `-Porg.gradle.java.installations.paths=<JDK 8 home>`.

```sh
heavy status
heavy --label replaymodserverbound-build -- ./gradlew --no-daemon :1.8.9:bundleJar
```

The jar is `versions/1.8.9/build/libs/replaymod-1.8.9-<version>.jar`.

The 1.8.9 Gradle project compiles against Forge `11.15.1.1722`.
