/**
 * Small, self-written equivalents of Minecraft 1.20.5+/1.21 utility APIs that Sable was written against and that
 * don't exist on 1.20.1. They live in Sable's own package because Forge 1.20.1 loads Minecraft as a module, so mods
 * cannot add classes to {@code net.minecraft.*} packages. The class and member names follow the 1.21 originals so
 * call sites read the same.
 */
package dev.ryanhcode.sable.backport;
