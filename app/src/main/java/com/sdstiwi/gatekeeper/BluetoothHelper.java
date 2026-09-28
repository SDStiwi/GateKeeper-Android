package com.sdstiwi.gatekeeper;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Thin wrapper around a classic Bluetooth (RFCOMM/SPP) client socket used to talk to the
 * ESP32 door controller.
 *
 * The controller exposes the standard Serial Port Profile UUID, so no custom GATT service is
 * needed: the app opens one socket, writes short text commands (e.g. lock/unlock) to its output
 * stream and closes it on {@link #disconnect()}.
 *
 * Not thread-safe: {@link #connectToDevice(String)} blocks until the socket connects or fails,
 * so callers must not invoke it from the UI thread on a slow link.
 */
public class BluetoothHelper {
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice bluetoothDevice;
    private OutputStream outputStream;
    private Context context;

    private static final UUID ESP32_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Standard Serial UUID

    private boolean isConnected = false;  // Flag to track connection status

    /**
     * Creates a helper bound to the device's default Bluetooth adapter.
     *
     * @param context used only to show status toasts
     */
    public BluetoothHelper(Context context) {
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * Opens an RFCOMM socket to the paired device with the given MAC address.
     *
     * Returns immediately with {@code true} if a connection is already open, so it is safe to call
     * before every command. On failure the half-open socket is closed and the helper stays
     * disconnected.
     *
     * @param deviceAddress MAC address of the ESP32, e.g. {@code "24:6F:28:AA:BB:CC"}
     * @return {@code true} if connected, {@code false} if Bluetooth is unavailable or the connect failed
     */
    // Check Bluetooth connection status before connecting
    public boolean connectToDevice(String deviceAddress) {
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            Log.e("BluetoothConnection", "Bluetooth Adapter is null");
            return false;
        }

        if (isConnected) {
            Log.d("BluetoothConnection", "Already connected to device.");
            return true;  // No need to reconnect if already connected
        }

        Log.d("BluetoothConnection", "Getting remote device with address: " + deviceAddress);
        bluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);

        try {
            Log.d("BluetoothConnection", "Creating RFCOMM socket using UUID: " + ESP32_UUID);
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(ESP32_UUID);

            Log.d("BluetoothConnection", "Attempting to connect to socket...");
            bluetoothSocket.connect();

            Log.d("BluetoothConnection", "Connection successful! Getting output stream...");
            outputStream = bluetoothSocket.getOutputStream();

            isConnected = true;  // Set connection flag to true
            Toast.makeText(context, "Connected to ESP32", Toast.LENGTH_SHORT).show();
            Log.i("BluetoothConnection", "Successfully connected to ESP32 device.");
            return true;
        } catch (IOException connectException) {
            Toast.makeText(context, "Failed to connect", Toast.LENGTH_SHORT).show();
            Log.e("BluetoothConnection", "Connection failed: " + connectException.getMessage(), connectException);

            try {
                Log.d("BluetoothConnection", "Trying to close socket after failed connection...");
                if (bluetoothSocket != null) {
                    bluetoothSocket.close();
                }
            } catch (IOException closeException) {
                Log.e("BluetoothConnection", "Could not close socket after failed connection: " + closeException.getMessage(), closeException);
            }

            return false;
        }
    }

    /**
     * Writes a UTF-8 encoded command to the connected device and flushes the stream.
     *
     * A write failure marks the helper as disconnected so the next {@link #connectToDevice(String)}
     * call re-opens the socket instead of reusing a dead one.
     *
     * @param message raw command string understood by the ESP32 firmware
     * @return {@code true} if the bytes were written, {@code false} if not connected or the write failed
     */
    // Send data if connected
    public boolean sendData(String message) {
        if (!isConnected || outputStream == null) {
            Log.e("BluetoothConnection", "Not connected or output stream is unavailable.");
            return false;
        }

        try {
            outputStream.write(message.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            outputStream.flush();
            return true;
        } catch (IOException e) {
            Log.e("BluetoothConnection", "Failed to send data: " + e.getMessage(), e);
            isConnected = false;
            return false;
        }
    }

    /**
     * Closes the output stream and socket. No-op if there is no active connection.
     */
    // Disconnect from the device
    public void disconnect() {
        if (!isConnected) {
            Log.d("BluetoothConnection", "No active connection to disconnect.");
            return;  // Return early if not connected
        }

        try {
            if (outputStream != null) {
                outputStream.close();
            }
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
            isConnected = false;  // Reset connection flag
            Log.d("BluetoothConnection", "Disconnected from device.");
        } catch (IOException e) {
            Log.e("BluetoothConnection", "Error closing socket: " + e.getMessage(), e);
        }
    }
}
