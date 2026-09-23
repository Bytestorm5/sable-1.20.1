package dev.ryanhcode.sable.backport.network.codec;

/**
 * Writes a value to a buffer, with the value first (for instance methods such as {@code value.write(buf)}).
 */
@FunctionalInterface
public interface StreamMemberEncoder<O, T> {
    void encode(T value, O buffer);
}
