# OnyxLoader

Custom launch bootstrap for Onyx Client (Java 8). Applies FPS/network hints, injects OptiFine HD U M5 + OnyxClient onto the classpath, then starts Minecraft via LaunchWrapper / Forge.

## Build

```bash
cd OnyxLoader
./gradlew build
./gradlew copyToLauncher   # copies jar → OnyxLauncher/resources/OnyxLoader.jar
```

Requires **Java 8**.

## Bundle OptiFine

Place `OptiFine_1.8.9_HD_U_M5.jar` at:

```
src/main/resources/optifine/OptiFine_1.8.9_HD_U_M5.jar
```

(Not redistributed in this repo — obtain legally from optifine.net.)

## Run

Usually launched by **OnyxLauncher**. Manual:

```bash
java -server -Xms2G -Xmx4G -XX:+UseG1GC -jar OnyxLoader-1.0.jar --gameDir ~/Library/Application\ Support/onyxclient
```

## Components

| Class | Role |
|-------|------|
| `OnyxLoader` | Entry point |
| `OptiFineInjector` | Extract/inject OptiFine + write `optionsof.txt` |
| `ClassInjector` | Add jars to system `URLClassLoader` |
| `FPSOptimizer` | Thread priority / render hints |
| `NetworkOptimizer` | TCP_NODELAY preference |
| `PatchManager` | Flags ASM-oriented render opts for client mixins |

---

*OnyxLoader v1.0*
