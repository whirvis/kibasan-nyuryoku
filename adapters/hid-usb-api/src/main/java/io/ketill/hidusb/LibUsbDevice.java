package io.ketill.hidusb;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.usb4java.Context;
import org.usb4java.DescriptorUtils;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceHandle;
import org.usb4java.DeviceList;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

import java.awt.*;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A wrapper for an underlying LibUSB device.
 * <p>
 * This class was originally created to make unit testing possible with
 * Mockito (as it does not support mocking native methods.) However, it
 * has proven to make the code which interfaces with USB devices cleaner
 * and safer.
 * <p>
 * This class can be extended and provided as the LibUSB device template
 * type to a {@link LibUsbDeviceSeeker}. Children of this class have access
 * to the underlying USB context, device, handle, etc. These can be used to
 * add missing LibUSB functionality.
 * <p>
 * <b>Note:</b> Most USB devices cannot be communicated with unless LibUSB
 * drivers have been installed for them on this system. This can be achieved
 * easily using Zadig. Make sure to inform clients of this! The function
 * {@link #openZadigHomepage()} is provided for this purpose. It directs
 * users to the home page of <a href="https://zadig.akeo.ie/">Zadig.</a>
 *
 * @see LibUsbDeviceSupplier
 * @see #requireSuccess(LibUsbOperation)
 */
public class LibUsbDevice implements Closeable {

    private static final Map<Context, Long> LAST_GET_DEVICE_TIMES =
            new HashMap<>();
    private static URI zadigHomepage;

    @FunctionalInterface
    protected interface LibUsbOperation {
        int execute();
    }

    /**
     * Requires that an operation return a value indicating a LibUSB
     * operation was successful. It is intended to reduce boilerplate
     * when making calls to {@link LibUsb}.
     *
     * @param operation the code to execute.
     * @return the result of the operation, may be ignored.
     * @throws NullPointerException if {@code operation} is {@code null}.
     * @throws LibUsbException      if an error code is returned.
     */
    @SuppressWarnings("UnusedReturnValue")
    protected static int requireSuccess(@NotNull LibUsbOperation operation) {
        Objects.requireNonNull(operation, "operation cannot be null");
        int result = operation.execute();
        if (result < LibUsb.SUCCESS) {
            throw new LibUsbException(result);
        }
        return result;
    }

    /**
     * Initializes a LibUSB context.
     *
     * @return the initialized context.
     * @throws LibUsbException if an error code is returned when
     *                         initializing the context.
     * @see #exitContext(Context)
     */
    public static @NotNull Context initContext() {
        Context context = new Context();
        requireSuccess(() -> LibUsb.init(context));
        return context;
    }

    /**
     * Shuts down a LibUSB context.
     *
     * @param context the context to de-initialize. A value of
     *                {@code null} is <i>not</i> permitted, the
     *                default context is forbidden.
     * @throws NullPointerException if {@code context} is {@code null}.
     */
    public static void exitContext(@NotNull Context context) {
        /*
         * Although LibUSB accepts a null for the default context,
         * this method is used to de-initialize a context created
         * by initContext() (which never returns a null value.) As
         * such, it is assumed to be an error by the user if they
         * provide null context.
         */
        Objects.requireNonNull(context, "default context is forbidden");
        LibUsb.exit(context);
    }

    /**
     * Returns all USB devices currently connected to the system.
     * <p>
     * All devices returned in this list must be freed, otherwise a memory
     * leak will occur. Use {@link #close()} when finished with a LibUSB
     * device, as it will ensure it is freed from memory.
     * <p>
     * <b>Note:</b> This method is non-blocking unless called multiple times
     * in less than one second for the specified context. When calling this
     * method too quickly, the thread will be put to sleep for the remaining
     * duration. This is to help ensure issues don't arise when communicating
     * with USB devices.
     *
     * @param context        the context to operate on. A value of
     *                       {@code null} is <i>not</i> permitted,
     *                       the default context is forbidden.
     * @param deviceSupplier the LibUSB device supplier.
     * @throws NullPointerException if {@code context} or
     *                              {@code deviceSupplier} are {@code null};
     *                              if a LibUSB device given by
     *                              {@code deviceSupplier} is {@code null}.
     * @throws LibUsbException      if an error code is returned when
     *                              retrieving the device list.
     * @see #closeDevices(Iterable)
     */
    /* @formatter:off */
    public static @NotNull <L extends LibUsbDevice> List<@NotNull L>
            getConnected(@NotNull Context context,
                         @NotNull LibUsbDeviceSupplier<L> deviceSupplier) {
        Objects.requireNonNull(context,
                "default context is forbidden");
        Objects.requireNonNull(deviceSupplier,
                "deviceSupplier cannot be null");

        long currentTime = System.currentTimeMillis();
        long lastGetDeviceTime = 0L;
        if(LAST_GET_DEVICE_TIMES.containsKey(context)) {
            lastGetDeviceTime = LAST_GET_DEVICE_TIMES.get(context);
        }

        long awaitMs = 1000L - (currentTime - lastGetDeviceTime);
        if(awaitMs > 0) {
            try {
                Thread.sleep(awaitMs);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        LAST_GET_DEVICE_TIMES.put(context, currentTime);

        DeviceList devices = new DeviceList();
        requireSuccess(() -> LibUsb.getDeviceList(context, devices));

        List<L> connected = new ArrayList<>();
        for (Device device : devices) {
            L wrapped = deviceSupplier.get(context, device);
            Objects.requireNonNull(wrapped,
                    "supplied device cannot be null");
            connected.add(wrapped);
        }

        /*
         * Now that the devices have been transferred to garbage
         * collected memory, free the list handle (but keep each
         * device handle.) The argument for the second parameter
         * must be false. When true, it decreases the reference
         * count by one. That would release them from memory too
         * early in this situation.
         */
        LibUsb.freeDeviceList(devices, false);

        return connected;
    }
    /* @formatter:on */

    /**
     * @param devices the devices to close.
     * @throws NullPointerException if {@code devices} is {@code null}.
     */
    /* @formatter:off */
    public static void
            closeDevices(@NotNull Iterable<? extends LibUsbDevice> devices) {
        Objects.requireNonNull(devices, "devices cannot be null");
        for (LibUsbDevice device : devices) {
            Objects.requireNonNull(device, "device cannot be null");
            device.close();
        }
    }
    /* @formatter:on */

    /**
     * @param usbClass     the USB class ID.
     * @param fallbackByte {@code true} if {@code usbClass} should be returned
     *                     as a hexadecimal string if the name is unknown,
     *                     {@code false} to return {@code null}.
     * @return the USB class name.
     */
    public static @Nullable String getClassName(byte usbClass,
                                                boolean fallbackByte) {
        String name = DescriptorUtils.getUSBClassName(usbClass);
        if (name.equalsIgnoreCase("unknown")) {
            if (fallbackByte) {
                return String.format("0x%02x", usbClass & 0xFF);
            } else {
                return null;
            }
        } else {
            return name;
        }
    }

    /**
     * @param usbClass the USB class ID.
     * @return the USB class name, {@code null} if it is unknown.
     */
    public static @Nullable String getClassName(byte usbClass) {
        return getClassName(usbClass, false);
    }

    /**
     * @return the URI for the Zadig homepage, {@code null} if it could not
     * be resolved.
     */
    public static URI getZadigHomepage() {
        if (zadigHomepage == null) {
            try {
                zadigHomepage = new URI("https://zadig.akeo.ie/");
            } catch (URISyntaxException e) {
                zadigHomepage = null;
            }
        }
        return zadigHomepage;
    }

    /**
     * Attempts to open the home page for the Zadig homepage with the
     * default web browser of the current system.
     *
     * @return {@code true} if the page was successfully opened,
     * {@code false} otherwise.
     */
    public static boolean openZadigHomepage() {
        if (!Desktop.isDesktopSupported()) {
            return false;
        }

        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.BROWSE)) {
            return false;
        }

        URI zadigHomepage = getZadigHomepage();
        if (zadigHomepage == null) {
            return false;
        }

        try {
            desktop.browse(zadigHomepage);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    protected final @NotNull Context usbContext;
    protected final @NotNull Device usbDevice;
    protected final @NotNull DeviceDescriptor usbDescriptor;

    private final long ptr;
    private final @NotNull ProductId productId;

    private @Nullable DeviceHandle usbHandle;
    private boolean closed;

    /**
     * Preferably, the construction of LibUSB devices should only be done
     * by {@link #getConnected(Context, LibUsbDeviceSupplier)}. If this
     * <i>must</i> be used, responsibility is placed upon the caller to
     * provide the correct arguments.
     * <p>
     * Children can access the underlying USB context and device via the
     * internal {@code usbContext} and {@code usbDevice} fields. A handle
     * for the USB handle can also be opened via {@link #openHandle()} and
     * later retrieved using {@link #getHandle()}.
     *
     * @param context   the context to operate on. A value of
     *                  {@code null} is <i>not</i> permitted,
     *                  the default context is forbidden.
     * @param usbDevice the USB device to perform I/O on.
     * @throws NullPointerException if {@code context} or {@code usbDevice}
     *                              are {@code null}.
     * @throws LibUsbException      if an error code is returned when
     *                              getting the device descriptor.
     * @see #close()
     */
    public LibUsbDevice(@NotNull Context context, @NotNull Device usbDevice) {
        this.usbContext = Objects.requireNonNull(context,
                "default context is forbidden");
        this.usbDevice = Objects.requireNonNull(usbDevice,
                "device cannot be null");
        this.usbDescriptor = new DeviceDescriptor();

        /* initialize contents of descriptor */
        requireSuccess(() -> LibUsb.getDeviceDescriptor(usbDevice,
                usbDescriptor));

        this.ptr = usbDevice.getPointer();

        /*
         * Vendor IDs and product IDs are unsigned shorts. However,
         * the underlying LibUSB API returns them as a signed Java
         * short. This converts them to an unsigned value and stores
         * them as an int so the expected value is returned.
         */
        int vendorId = usbDescriptor.idVendor() & 0xFFFF;
        int productId = usbDescriptor.idProduct() & 0xFFFF;
        this.productId = new ProductId(vendorId, productId);
    }

    public final @NotNull ProductId getProductId() {
        return this.productId;
    }

    /**
     * @return the USB device handle.
     * @throws IllegalStateException if a call to {@link #openHandle()} was
     *                               not made before calling this method.
     */
    protected final @NotNull DeviceHandle getHandle() {
        if (usbHandle == null) {
            throw new IllegalStateException("handle not open");
        }
        return this.usbHandle;
    }

    /**
     * Opens this device and obtains an internal device handle. The USB
     * handle can be obtained via {@link #getHandle()}. If the handle is
     * already open, this method has no effect.
     * <p>
     * <b>Note:</b> A LibUSB device can be closed without ever opening a
     * handle. The method {@link #requireOpen()} only ensures that this
     * device has not been closed via {@link #close()}.
     *
     * @throws LibUsbException       if an error code is returned when
     *                               opening the device handle.
     * @throws IllegalStateException if this LibUSB device has been
     *                               closed via {@link #close()}.
     */
    protected final void openHandle() {
        this.requireOpen();
        if (usbHandle != null) {
            return;
        }
        try {
            this.usbHandle = new DeviceHandle();
            requireSuccess(() -> LibUsb.open(usbDevice, usbHandle));
        } catch (LibUsbException e) {
            this.usbHandle = null;
            throw e;
        }
    }

    /**
     * Retrieves the string descriptor at the specified index. In order for
     * this method to work, the handle for this device must have been opened
     * via {@link #openHandle()}.
     *
     * @param index        the descriptor index.
     * @param fallbackByte {@code true} if {@code index} should be returned
     *                     as a hexadecimal string if the descriptor could not
     *                     be fetched, {@code false} to return {@code null}.
     * @return the string descriptor.
     */
    public final @Nullable String getString(byte index, boolean fallbackByte) {
        String descriptor = LibUsb.getStringDescriptor(usbHandle, index);
        if (descriptor == null) {
            if (fallbackByte) {
                return String.format("0x%02x", index & 0xFF);
            } else {
                return null;
            }
        } else {
            return descriptor;
        }
    }

    /**
     * Retrieves the string descriptor at the specified index. In order for
     * this method to work, the handle for this device must have been opened
     * via {@link #openHandle()}.
     * <p>
     * This method is a shorthand for {@link #getString(byte, boolean)}, with
     * the argument for {@code fallbackByte} being {@code false}.
     *
     * @param index the descriptor index.
     * @return the string descriptor, {@code null} if it could not be fetched
     * for any reason.
     */
    public final @Nullable String getString(byte index) {
        return this.getString(index, false);
    }

    /**
     * @throws IllegalStateException if this LibUSB device has been
     *                               closed via {@link #close()}.
     */
    protected final void requireOpen() {
        if (closed) {
            throw new IllegalStateException("device closed");
        }
    }

    public final boolean isClosed() {
        return this.closed;
    }

    /**
     * Destroys the internal LibUSB device. If this device was opened via
     * {@link #openHandle()}, the internal LibUSB handle is also closed.
     * If the device is already closed then invoking this method has no
     * effect.
     */
    @Override
    @MustBeInvokedByOverriders
    public void close() {
        if (closed) {
            return;
        }

        LibUsb.unrefDevice(usbDevice);

        if (usbHandle != null) {
            LibUsb.close(usbHandle);
            this.usbHandle = null;
        }

        this.closed = true;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (this.getClass() != obj.getClass()) {
            return false;
        }
        LibUsbDevice that = (LibUsbDevice) obj;
        return this.ptr == that.ptr;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(ptr);
    }

    @Override
    public final String toString() {
        short usbVersion = usbDescriptor.bcdUSB();
        byte usbClass = usbDescriptor.bDeviceClass();
        byte usbSubclass = usbDescriptor.bDeviceSubClass();
        byte protocol = usbDescriptor.bDeviceProtocol();
        byte maxPacketSize = usbDescriptor.bMaxPacketSize0();
        int vendorId = this.productId.vendorId;
        int productId = this.productId.productId;
        short releaseNumber = usbDescriptor.bcdDevice();
        byte manufacturer = usbDescriptor.iManufacturer();
        byte product = usbDescriptor.iProduct();
        byte serialNumber = usbDescriptor.iSerialNumber();
        byte configCount = usbDescriptor.bNumConfigurations();

        String usbVersionStr = DescriptorUtils.decodeBCD(usbVersion);
        String usbClassStr = getClassName(usbClass, true);
        String usbSubclassStr = Integer.toString(usbSubclass & 0xFF);
        String protocolStr = Integer.toString(protocol & 0xFF);
        String maxPacketSizeStr = Integer.toString(maxPacketSize & 0xFF);
        String vendorIdStr = String.format("0x%04x", vendorId);
        String productIdStr = String.format("0x%04x", productId);
        String releaseNumberStr = DescriptorUtils.decodeBCD(releaseNumber);
        String manufacturerStr = this.getString(manufacturer, true);
        String productStr = this.getString(product, true);
        String serialNumberStr = this.getString(serialNumber, true);
        String configCountStr = Integer.toString(configCount & 0xFF);

        /* @formatter:off */
        return this.getClass().getSimpleName()      + "{"  +
                "usbVersion="    + usbVersionStr    + ", " +
                "usbClass="      + usbClassStr      + ", " +
                "usbSubclass="   + usbSubclassStr   + ", " +
                "protocol="      + protocolStr      + ", " +
                "maxPacketSize=" + maxPacketSizeStr + ", " +
                "vendorId="      + vendorIdStr      + ", " +
                "productId="     + productIdStr     + ", " +
                "releaseNumber=" + releaseNumberStr + ", " +
                "manufacturer="  + manufacturerStr  + ", " +
                "product="       + productStr       + ", " +
                "serialNumber="  + serialNumberStr  + ", " +
                "configCount="   + configCountStr   + "}";
        /* @formatter:on */
    }

}
