/**
 * Equivalents of Minecraft 1.20.5+/1.21 APIs that Sable uses and that neither 1.20.1 nor Veil's backports
 * ({@code foundry.veil.backport}, which provide StreamCodec, CustomPacketPayload, DeltaTracker and friends) cover.
 * They live in Sable's own package because Forge 1.20.1 loads Minecraft as a module, so mods cannot add classes to
 * {@code net.minecraft.*} packages.
 */
package dev.ryanhcode.sable.backport;
