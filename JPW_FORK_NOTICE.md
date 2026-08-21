# JPW Labs MegaMine presentation fork

This branch is a deliberately narrow Java 21 fork for MegaMine. It starts at upstream
`466eee00a4f815b3cfdf69ff7e0413e0b1e08aeb` and builds only FancyNpcs v2,
FancyHolograms v2, FancyDialogs, and the libraries shaded inside those artifacts.

JPW changes remove analytics, bStats, network version checks, the OP command action,
the Citizens converter, and default administrative command registration. Skin lookup is
closed to JPW-approved cached asset identifiers; the MineSkin/Mojang clients and their
mutable client dependency are not present in the artifact. FancyDialogs JSON cannot run
player commands, console commands, or proxy transfers. The only supported server version
is Paper 1.21.8 on Java 21.

The interaction policy includes the semantic correction from upstream commit
`512aa29dd42ef27b728b73fccd6a013f816995a6`, covered by an exactly-once regression test.
FancyDialogs `1.1.2-jpw.5` also removes its injected packet decoder on quit/disable and
resolves active players by UUID so a closed Netty channel cannot retain a Bukkit player.

Build command:

```shell
./gradlew clean :plugins:fancynpcs-v2:test :plugins:fancynpcs-v2:shadowJar \
  :plugins:fancyholograms-v2:shadowJar :plugins:fancydialogs:shadowJar \
  --no-daemon --no-build-cache
python3 scripts/normalize-artifacts.py \
  plugins/fancynpcs-v2/build/libs/FancyNpcs-2.9.2.341-jpw.4.jar \
  plugins/fancyholograms-v2/build/libs/FancyHolograms-2.9.1.180-jpw.4.jar \
  plugins/fancydialogs/build/libs/FancyDialogs-1.1.2-jpw.5.jar
python3 scripts/audit-artifacts.py
```

No artifacts or internal libraries from this repository are MegaMine-wide APIs.
