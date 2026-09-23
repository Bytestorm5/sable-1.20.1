package dev.ryanhcode.sable.backport.serialization;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.RecordBuilder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Codec helpers that DataFixerUpper 7 (Minecraft 1.20.5+) has and DataFixerUpper 6 (Minecraft 1.20.1) lacks.
 */
public final class BackportCodecs {

    private BackportCodecs() {
    }

    /**
     * A map whose value codec is chosen by each entry's key.
     */
    public static <K, V> Codec<Map<K, V>> dispatchedMap(final Codec<K> keyCodec, final Function<K, Codec<? extends V>> valueCodecFactory) {
        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<Map<K, V>, T>> decode(final DynamicOps<T> ops, final T input) {
                return ops.getMapValues(input).flatMap(entries -> {
                    final Map<K, V> result = new LinkedHashMap<>();
                    final StringBuilder errors = new StringBuilder();
                    entries.forEach(entry -> {
                        final DataResult<K> key = keyCodec.parse(ops, entry.getFirst());
                        final DataResult<V> value = key.flatMap(k -> valueCodecFactory.apply(k).parse(ops, entry.getSecond()).map(v -> (V) v));
                        key.result().ifPresent(k -> value.result().ifPresent(v -> result.put(k, v)));
                        key.error().ifPresent(e -> errors.append(e.message()).append("; "));
                        value.error().ifPresent(e -> errors.append(e.message()).append("; "));
                    });
                    final Pair<Map<K, V>, T> pair = Pair.of(Map.copyOf(result), input);
                    if (errors.length() > 0) {
                        return DataResult.error(errors::toString, pair);
                    }
                    return DataResult.success(pair);
                });
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> DataResult<T> encode(final Map<K, V> input, final DynamicOps<T> ops, final T prefix) {
                final RecordBuilder<T> builder = ops.mapBuilder();
                for (final Map.Entry<K, V> entry : input.entrySet()) {
                    final Codec<V> valueCodec = (Codec<V>) valueCodecFactory.apply(entry.getKey());
                    builder.add(keyCodec.encodeStart(ops, entry.getKey()), valueCodec.encodeStart(ops, entry.getValue()));
                }
                return builder.build(prefix);
            }

            @Override
            public String toString() {
                return "DispatchedMapCodec[" + keyCodec + "]";
            }
        };
    }
}
