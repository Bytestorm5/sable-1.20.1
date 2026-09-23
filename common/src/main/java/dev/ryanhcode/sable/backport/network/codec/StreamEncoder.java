package dev.ryanhcode.sable.backport.network.codec;

/**
 * Writes a value to a buffer.
 */
@FunctionalInterface
public interface StreamEncoder<O, T> {
    void encode(O buffer, T value);
}
