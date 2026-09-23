# Sable on Forge 1.20.1

This branch is a port of Sable 2.0.5 from Minecraft 1.21.1 / NeoForge 21.1 to **Minecraft 1.20.1 / Forge 47.4.x**.
The public API (`dev.ryanhcode.sable.api`, `sable_companion`) keeps the same packages and signatures, so
add-ons like Simulated only need their own 1.20.1 changes.

## Building

1. **Veil 1.20.1** isn't published anywhere yet. Build it from `Bytestorm5/Veil-1.20.1`, branch
   `veil-1-20-1-migration-aan416`, and install it to Maven Local:
   ```
   ./gradlew publishToMavenLocal
   ```
   Sable resolves `foundry.veil:veil-forge-1.20.1:4.3.2` and `veil-common-1.20.1` from there.
2. Build Sable:
   ```
   ./gradlew :forge:build
   ```
   The jar is written to `neoforge/build/libs/sable-forge-1.20.1-<version>.jar`. It is reobfuscated to SRG, has its
   refmap and SRG access transformer, lists its mixin configs in the manifest, and jar-in-jars:
   - sable_companion and sable_rapier (both reobfuscated)
   - Veil
   - MixinExtras
   - lz4-java, which 1.20.1 doesn't ship and the Rapier natives loader needs

The build uses a Java 21 toolchain that compiles with `--release 17`, because some stand-in dependencies ship Java 21
class files. The output runs on Java 17.

Gradle project layout:
- `:forge` lives in `neoforge/`, so the upstream tree stays mergeable.
- `:fabric` is parked in `settings.gradle`.
- `:common` compiles against Forge through `legacyForge { mcpVersion }`.

Dev runs:
- `./gradlew :forge:runClient` (`-PquickPlay=<world>` loads straight into a world)
- `:forge:runServer`
- `:forge:runGameTest`

## What changed

| Area | 1.21.1 | 1.20.1 |
|---|---|---|
| Loader | NeoForge, `neoforge.mods.toml` | Forge, `mods.toml`, `@Mod` no-arg constructor, `ForgeConfigSpec`, `LazyOptional` capabilities |
| Names | Mojang at runtime | SRG at runtime: SRG access transformer, mixin refmap (`sable.refmap.json`) |
| Networking | Veil payloads | Veil 1.20.1's `VeilPacketManager` / `VeilPayloadRegistry` with its `StreamCodec` / `CustomPacketPayload` backports |
| Codecs | DFU 7 | DFU 6: `BackportCodecs.dispatchedMap`, `getOrThrow(false, …)` |
| Tick rate | `TickRateManager` | `backport/TickRate` (fixed 20 TPS) |
| Chunks | `ChunkResult`, `SectionRenderDispatcher` | `Either` futures, `ChunkRenderDispatcher.RenderChunk`, `BufferBuilder.RenderedBuffer` |
| Data | `structure/`, `tags/block/`, `c:` tags | `structures/`, `tags/blocks/`, `forge:` tags (the `c:` entries are kept as optional), pack format 15 |
| Companion | NeoForge mod | `lowcodefml` library mod with its own `pack.mcmeta` |

The mechanical rewrites are scripted, so upstream changes can be ported the same way:
- `scripts/backport_rewrite.py`: 1.21 imports and idioms → Veil backports and Forge equivalents. Running it twice gives the same result.
- `scripts/at_to_srg.py`: converts access-transformer entries from Mojang names to SRG names and keeps the Mojang name as a comment.
- `scripts/fix_mixin_remap.py`: adds `remap = false` to injectors that target mod code and keeps Minecraft `@At` targets remapped.
- `scripts/check_tags.py`: checks that every tag entry resolves on 1.20.1.
- `./gradlew :forge:checkMixins` statically checks each mixin against the real 1.20.1 classes:
  - targets and `@At` targets
  - injector signatures
  - `@Shadow` / `@Accessor` / `@Invoker` members
  - Mixin 0.8.5 limits, such as no private methods in interface mixins

  It currently reports 559 injectors and no problems.

Some mixins targeted 1.21-only classes: `Leashable`, `PathfindingContext`, the `TestCommand` changes, the reach attributes and the `NetherPortalBlock` rework. Those mixins were dropped, and the mixins in the same packages that target the 1.20.1 classes cover their behavior.

### Compatibility

- **Create 6.0.8 (1.20.1)**: ported. The 1.20.1 packet handlers are lambdas, and `FanProcessingType` logic lives in `AirCurrent`. Fluids use Forge `FluidStack`, and capabilities use `LazyOptional`.
- **Ported, compile-checked only**: Flywheel 1.0.6, Sodium Extras, Oculus, Jade (and Jade Addons), Shoulder Surfing 5.1.1, Exposure, CC: Tweaked, Etched, and the Distant Horizons API.
- **Dropped** (no 1.20.1 build or API): Vista, PMWeather, Sophisticated Backpacks pickup events.
- Forge 1.20.1 has no `incompatible` dependency type, so the NeoForge build's guards aren't expressed:
  - against a newer sable-companion
  - against old Sodium
  - against ScalableLux

## Verification

- Both modules compile without errors, and `checkMixins` is clean.
- GameTest server: all required tests pass. The optional gravity test is off by about one tick, which matches its upstream `FIXME`.
- Dev dedicated server: assembling a sub-level, saving and reloading work.
- Dev client (software GL): sub-levels render, and a spinning sub-level falls and lands on another one.
- Production jar on a stock Forge 1.20.1-47.4.10 server (Sable only):
  - boots and loads the nested Veil and Rapier natives
  - assembles a sub-level and saves

## Known issues

- **Veil dev environment**: Veil's `PipelinePoseStackMixin` uses a `shadow$` method. ModDevGradle's dev-time remapper doesn't remap it, so the dev client crashes on start. Production isn't affected. The fix belongs in the Veil repo: call `((PoseStack) (Object) this).translate(...)` instead of the shadow.
- Without Create, the `create:flywheel` entry in `physics_block_properties/flywheel.json` logs an error on world load. Upstream does the same.
- These are ported but not runtime-tested yet:
  - Camera roll
  - Shoulder Surfing, Etched and the other optional compat mods
  - Oculus shader paths
- Upstream bug, kept as-is: `ServerLevelPlot.save` writes chunk attachments to the plot tag instead of the chunk tag.
