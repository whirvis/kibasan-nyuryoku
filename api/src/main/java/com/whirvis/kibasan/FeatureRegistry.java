package com.whirvis.kibasan;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface FeatureRegistry {

    /**
     * @param feature the feature whose registration to check.
     * @return {@code true} if {@code feature} is registered, {@code false}
     * otherwise.
     * @throws NullPointerException if {@code feature} is {@code null}.
     */
    boolean isRegistered(@NotNull DeviceFeature<?> feature);

    /**
     * @return all registered features.
     */
    @NotNull Collection<@NotNull RegisteredFeature<?, ?>> getFeatures();

    /**
     * @param feature the feature whose registration to fetch.
     * @param <S>     the state container type.
     * @return the feature registration, {@code null} if not registered.
     * @throws NullPointerException if {@code feature} is {@code null}.
     */
    /* @formatter:off */
    <S> @Nullable RegisteredFeature<?, S>
            getRegistered(@NotNull DeviceFeature<S> feature);
    /* @formatter:on */

    /**
     * @param feature the feature whose state to fetch.
     * @param <S>     the state container type.
     * @return the current state of {@code feature}.
     * @throws NullPointerException if {@code feature} is {@code null}.
     * @throws IllegalStateException if {@code feature} is not registered.
     */
    default <S> @NotNull S getState(@NotNull DeviceFeature<S> feature) {
        RegisteredFeature<?, S> registered = this.getRegistered(feature);
        if (registered == null) {
            throw new IllegalStateException("no such feature");
        }
        return registered.state;
    }

    /**
     * @param feature the feature to register.
     * @param <F>     the device feature type.
     * @param <S>     the state container type.
     * @return the feature registration.
     * @throws NullPointerException if {@code feature} is {@code null}.
     * @throws IllegalStateException if {@code feature} is already registered.
     */
    /* @formatter:off */
    <F extends DeviceFeature<S>, S> @NotNull RegisteredFeature<F, S>
            registerFeature(@NotNull F feature);
    /* @formatter:on */

    /**
     * @param feature the feature to unregister.
     * @throws NullPointerException if {@code feature} is {@code null}.
     * @throws IllegalStateException if {@code feature} is not registered.
     */
    void unregisterFeature(@NotNull DeviceFeature<?> feature);

}
