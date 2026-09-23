package dev.ryanhcode.sable.backport.network.codec;

import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function5;
import com.mojang.datafixers.util.Function6;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A paired encoder and decoder for a buffer type.
 *
 * @param <B> the buffer type
 * @param <V> the value type
 */
public interface StreamCodec<B, V> extends StreamDecoder<B, V>, StreamEncoder<B, V> {

    static <B, V> StreamCodec<B, V> of(final StreamEncoder<B, V> encoder, final StreamDecoder<B, V> decoder) {
        return new StreamCodec<>() {
            @Override
            public V decode(final B buffer) {
                return decoder.decode(buffer);
            }

            @Override
            public void encode(final B buffer, final V value) {
                encoder.encode(buffer, value);
            }
        };
    }

    static <B, V> StreamCodec<B, V> ofMember(final StreamMemberEncoder<B, V> encoder, final StreamDecoder<B, V> decoder) {
        return of((buffer, value) -> encoder.encode(value, buffer), decoder);
    }

    static <B, V> StreamCodec<B, V> unit(final V instance) {
        return of((buffer, value) -> {
            if (!instance.equals(value)) {
                throw new IllegalStateException("Can't encode '" + value + "', expected '" + instance + "'");
            }
        }, buffer -> instance);
    }

    default <O> StreamCodec<B, O> map(final Function<? super V, ? extends O> to, final Function<? super O, ? extends V> from) {
        final StreamCodec<B, V> self = this;
        return of((buffer, value) -> self.encode(buffer, from.apply(value)), buffer -> to.apply(self.decode(buffer)));
    }

    default <O> StreamCodec<B, O> apply(final CodecOperation<B, V, O> operation) {
        return operation.apply(this);
    }

    @SuppressWarnings("unchecked")
    default <S extends B> StreamCodec<S, V> cast() {
        return (StreamCodec<S, V>) this;
    }

    static <B, C, T1> StreamCodec<B, C> composite(
            final StreamCodec<? super B, T1> codec1, final Function<C, T1> getter1,
            final Function<T1, C> factory) {
        return of((buffer, value) -> codec1.encode(buffer, getter1.apply(value)),
                buffer -> factory.apply(codec1.decode(buffer)));
    }

    static <B, C, T1, T2> StreamCodec<B, C> composite(
            final StreamCodec<? super B, T1> codec1, final Function<C, T1> getter1,
            final StreamCodec<? super B, T2> codec2, final Function<C, T2> getter2,
            final BiFunction<T1, T2, C> factory) {
        return of((buffer, value) -> {
            codec1.encode(buffer, getter1.apply(value));
            codec2.encode(buffer, getter2.apply(value));
        }, buffer -> {
            final T1 v1 = codec1.decode(buffer);
            final T2 v2 = codec2.decode(buffer);
            return factory.apply(v1, v2);
        });
    }

    static <B, C, T1, T2, T3> StreamCodec<B, C> composite(
            final StreamCodec<? super B, T1> codec1, final Function<C, T1> getter1,
            final StreamCodec<? super B, T2> codec2, final Function<C, T2> getter2,
            final StreamCodec<? super B, T3> codec3, final Function<C, T3> getter3,
            final Function3<T1, T2, T3, C> factory) {
        return of((buffer, value) -> {
            codec1.encode(buffer, getter1.apply(value));
            codec2.encode(buffer, getter2.apply(value));
            codec3.encode(buffer, getter3.apply(value));
        }, buffer -> {
            final T1 v1 = codec1.decode(buffer);
            final T2 v2 = codec2.decode(buffer);
            final T3 v3 = codec3.decode(buffer);
            return factory.apply(v1, v2, v3);
        });
    }

    static <B, C, T1, T2, T3, T4> StreamCodec<B, C> composite(
            final StreamCodec<? super B, T1> codec1, final Function<C, T1> getter1,
            final StreamCodec<? super B, T2> codec2, final Function<C, T2> getter2,
            final StreamCodec<? super B, T3> codec3, final Function<C, T3> getter3,
            final StreamCodec<? super B, T4> codec4, final Function<C, T4> getter4,
            final Function4<T1, T2, T3, T4, C> factory) {
        return of((buffer, value) -> {
            codec1.encode(buffer, getter1.apply(value));
            codec2.encode(buffer, getter2.apply(value));
            codec3.encode(buffer, getter3.apply(value));
            codec4.encode(buffer, getter4.apply(value));
        }, buffer -> {
            final T1 v1 = codec1.decode(buffer);
            final T2 v2 = codec2.decode(buffer);
            final T3 v3 = codec3.decode(buffer);
            final T4 v4 = codec4.decode(buffer);
            return factory.apply(v1, v2, v3, v4);
        });
    }

    static <B, C, T1, T2, T3, T4, T5> StreamCodec<B, C> composite(
            final StreamCodec<? super B, T1> codec1, final Function<C, T1> getter1,
            final StreamCodec<? super B, T2> codec2, final Function<C, T2> getter2,
            final StreamCodec<? super B, T3> codec3, final Function<C, T3> getter3,
            final StreamCodec<? super B, T4> codec4, final Function<C, T4> getter4,
            final StreamCodec<? super B, T5> codec5, final Function<C, T5> getter5,
            final Function5<T1, T2, T3, T4, T5, C> factory) {
        return of((buffer, value) -> {
            codec1.encode(buffer, getter1.apply(value));
            codec2.encode(buffer, getter2.apply(value));
            codec3.encode(buffer, getter3.apply(value));
            codec4.encode(buffer, getter4.apply(value));
            codec5.encode(buffer, getter5.apply(value));
        }, buffer -> {
            final T1 v1 = codec1.decode(buffer);
            final T2 v2 = codec2.decode(buffer);
            final T3 v3 = codec3.decode(buffer);
            final T4 v4 = codec4.decode(buffer);
            final T5 v5 = codec5.decode(buffer);
            return factory.apply(v1, v2, v3, v4, v5);
        });
    }

    static <B, C, T1, T2, T3, T4, T5, T6> StreamCodec<B, C> composite(
            final StreamCodec<? super B, T1> codec1, final Function<C, T1> getter1,
            final StreamCodec<? super B, T2> codec2, final Function<C, T2> getter2,
            final StreamCodec<? super B, T3> codec3, final Function<C, T3> getter3,
            final StreamCodec<? super B, T4> codec4, final Function<C, T4> getter4,
            final StreamCodec<? super B, T5> codec5, final Function<C, T5> getter5,
            final StreamCodec<? super B, T6> codec6, final Function<C, T6> getter6,
            final Function6<T1, T2, T3, T4, T5, T6, C> factory) {
        return of((buffer, value) -> {
            codec1.encode(buffer, getter1.apply(value));
            codec2.encode(buffer, getter2.apply(value));
            codec3.encode(buffer, getter3.apply(value));
            codec4.encode(buffer, getter4.apply(value));
            codec5.encode(buffer, getter5.apply(value));
            codec6.encode(buffer, getter6.apply(value));
        }, buffer -> {
            final T1 v1 = codec1.decode(buffer);
            final T2 v2 = codec2.decode(buffer);
            final T3 v3 = codec3.decode(buffer);
            final T4 v4 = codec4.decode(buffer);
            final T5 v5 = codec5.decode(buffer);
            final T6 v6 = codec6.decode(buffer);
            return factory.apply(v1, v2, v3, v4, v5, v6);
        });
    }

    @FunctionalInterface
    interface CodecOperation<B, S, T> {
        StreamCodec<B, T> apply(StreamCodec<B, S> codec);
    }
}
