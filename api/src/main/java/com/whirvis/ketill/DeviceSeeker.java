package com.whirvis.ketill;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * The purpose of a device seeker is to scan for input devices currently
 * connected to the system. When such device is detected, the appropriate
 * {@code InputDevice} instance and adapter will be created. Devices must
 * be polled manually after creation using {@link InputDevice#poll()}. They
 * can be retrieved from {@link #discoveredDevices}.
 * <p/>
 * Implementations should call {@link #discoverDevice(InputDevice)} when a
 * device is discovered and {@link #forgetDevice(InputDevice)} when a device
 * is forgotten.
 * <p/>
 * <b>Note:</b> For a device seeker to work as expected, it must be told to
 * perform device scans periodically via {@link #seek()}. It is recommended
 * to perform a scan once every application update.
 *
 * @param <I> the input device type.
 * @see #onDiscoverDevice(Consumer)
 * @see #onForgetDevice(Consumer)
 * @see #onError(Consumer)
 * @see DeviceAdapter
 */
public abstract class DeviceSeeker<I extends InputDevice> {

    private final @NotNull List<I> devices;
    public final @NotNull List<I> discoveredDevices; /* read only view */

    private @Nullable Consumer<? super I> discoverDeviceCallback;
    private @Nullable Consumer<? super I> forgetDeviceCallback;
    private @Nullable Consumer<Throwable> errorCallback;

    public DeviceSeeker() {
        this.devices = new ArrayList<>();
        this.discoveredDevices = Collections.unmodifiableList(devices);
    }

    /**
     * Sets the callback for what will happen when this seeker has discovered
     * a device. If this callback was set <i>after</i> one or more devices
     * have been discovered, it will not be called for them. They will have
     * to be retrieved via {@link #discoveredDevices}.
     *
     * @param callback the code to execute when a device is discovered. A
     *                 value of {@code null} is permitted, and will result
     *                 in nothing being executed.
     */
    public void onDiscoverDevice(@Nullable Consumer<? super I> callback) {
        this.discoverDeviceCallback = callback;
    }

    /**
     * Sets the callback for what will happen when this seeker has forgotten
     * a device. If this callback was set <i>after</i> one or more devices
     * have been forgotten, it will not be called for them. Current devices
     * will have to be retrieved via {@link #discoveredDevices}.
     *
     * @param callback the code to execute when a device is forgotten. A
     *                 value of {@code null} is permitted, and will result
     *                 in nothing being executed.
     */
    public void onForgetDevice(@Nullable Consumer<? super I> callback) {
        this.forgetDeviceCallback = callback;
    }

    /**
     * Sets the callback for what will happen when an error occurs in
     * {@link #seek()}. By default, a wrapping {@code InputException}
     * will be constructed for the original error and thrown.
     *
     * @param callback the code to execute when a device is forgotten. A
     *                 value of {@code null} is permitted, and will result
     *                 in a wrapping {@code InputException} being thrown.
     */
    public void onError(@Nullable Consumer<Throwable> callback) {
        this.errorCallback = callback;
    }

    @MustBeInvokedByOverriders
    protected void discoverDevice(@NotNull I device) {
        Objects.requireNonNull(device, "device");
        if (devices.contains(device)) {
            return;
        }
        devices.add(device);
        if (discoverDeviceCallback != null) {
            discoverDeviceCallback.accept(device);
        }
    }

    @MustBeInvokedByOverriders
    protected void forgetDevice(@NotNull I device) {
        Objects.requireNonNull(device, "device");
        if (!devices.contains(device)) {
            return;
        }
        devices.remove(device);
        if (forgetDeviceCallback != null) {
            forgetDeviceCallback.accept(device);
        }
    }

    /**
     * Called by {@link #seek()}, this method can throw any exception without
     * needing to catch it. When an exception is thrown, {@link #seek()} will
     * wrap it into a {@link InputException} and throw it to the caller.
     *
     * @throws Exception if an error occurs.
     */
    protected abstract void seekImpl() throws Exception;

    /**
     * Performs a <i>single</i> scan for devices connected to this system.
     * For continuous scanning, this method must be called periodically once
     * every application update.
     *
     * @throws InputException if an error occurs and no callback has
     *                        been set via {@link #onError(Consumer)}.
     */
    public final synchronized void seek() {
        try {
            this.seekImpl();
        } catch (Throwable cause) {
            if (errorCallback != null) {
                errorCallback.accept(cause);
            } else {
                throw new InputException("error in DeviceSeeker", cause);
            }
        }
    }

}
