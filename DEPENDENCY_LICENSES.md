# Dependency and attribution ledger

The selected plugins retain FancyInnovations' MIT license and notices in `LICENSE`.
JPW-built jars are internal deployment artifacts and are not a relicensing of upstream.

Runtime code intentionally shaded by the selected build is limited to:

| Component | Pinned coordinate/source | License |
|---|---|---|
| FancyPlugins selected source and internal libraries | upstream `466eee00a4f815b3cfdf69ff7e0413e0b1e08aeb` | MIT |
| Incendo Cloud | `org.incendo:cloud-*` at the versions in Gradle lockfiles | MIT |
| MineSkin Java client | `org.mineskin:java-client*` at the locked `3.0.3-SNAPSHOT` resolution | MIT |
| Google Gson / Guava | versions in Gradle lockfiles | Apache-2.0 |
| jsoup | version in Gradle lockfiles | MIT |
| JetBrains annotations | version in Gradle lockfiles | Apache-2.0 |
| ChatColorHandler | `org.lushplugins:ChatColorHandler:6.0.4` | MIT |

Paper, Folia, PlaceholderAPI, PlotSquared, WorldEdit, Floodgate, and JUnit are build/test
or server-provided dependencies and are not published as standalone MegaMine libraries.
Every selected project has a committed `gradle.lockfile`; changing an upstream dependency
requires regenerating and reviewing those locks and rebuilding the artifact hashes.
