package dev.ryanhcode.sable.backport.network.codec;

/**
 * Reads a value from a buffer.
 */
@FunctionalInterface
public interface StreamDecoder<I, T> {
    T decode(I buffer);
}
