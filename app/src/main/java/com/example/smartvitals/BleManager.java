package com.example.smartvitals;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;

import java.util.UUID;

public class BleManager {

    public interface Listener {
        void onStatusChanged(String message);
        void onDeviceFound(BluetoothDevice device);
        void onConnected(String deviceName);
        void onDisconnected();
        void onHeartRateReceived(int bpm);
    }

    private final Context context;
    private final Listener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private boolean isScanning = false;

    private static final UUID HEART_RATE_SERVICE_UUID =
            UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");

    private static final UUID HEART_RATE_MEASUREMENT_UUID =
            UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");

    private static final UUID CLIENT_CONFIG_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public BleManager(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;

        BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter != null) {
                bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            }
        }
    }

    public boolean isBluetoothReady() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    public void startScan() {
        if (!hasPermissions()) {
            listener.onStatusChanged("Bluetooth permission not granted");
            return;
        }

        if (bluetoothAdapter == null) {
            listener.onStatusChanged("Bluetooth not supported");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            listener.onStatusChanged("Please turn on Bluetooth");
            return;
        }

        if (bluetoothLeScanner == null) {
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        }

        if (bluetoothLeScanner == null) {
            listener.onStatusChanged("BLE scanner unavailable");
            return;
        }

        stopScan();

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        bluetoothLeScanner.startScan(null, settings, scanCallback);
        isScanning = true;
        listener.onStatusChanged("Scanning for devices...");
    }

    @SuppressLint("MissingPermission")
    public void stopScan() {
        if (bluetoothLeScanner != null && isScanning) {
            try {
                bluetoothLeScanner.stopScan(scanCallback);
            } catch (Exception ignored) {
            }
        }
        isScanning = false;
    }

    @SuppressLint("MissingPermission")
    public void connectToDevice(BluetoothDevice device) {
        if (device == null) {
            listener.onStatusChanged("No device selected");
            return;
        }

        if (!hasPermissions()) {
            listener.onStatusChanged("Bluetooth permission not granted");
            return;
        }

        stopScan();
        closeGattOnly();

        listener.onStatusChanged("Connecting to " + getDeviceName(device) + "...");
        bluetoothGatt = device.connectGatt(context, false, gattCallback);
    }

    @SuppressLint("MissingPermission")
    public void disconnect() {
        if (bluetoothGatt != null) {
            try {
                bluetoothGatt.disconnect();
            } catch (Exception ignored) {
            }
        }
    }

    public void close() {
        stopScan();
        closeGattOnly();
    }

    @SuppressLint("MissingPermission")
    private void closeGattOnly() {
        if (bluetoothGatt != null) {
            try {
                bluetoothGatt.close();
            } catch (Exception ignored) {
            }
            bluetoothGatt = null;
        }
    }

    @SuppressLint("MissingPermission")
    private String getDeviceName(BluetoothDevice device) {
        if (device == null) return "Unknown device";

        try {
            String name = device.getName();
            if (name != null && !name.trim().isEmpty()) {
                return name;
            }
        } catch (SecurityException ignored) {
        }

        return "Unnamed device";
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        @SuppressLint("MissingPermission")
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device != null) {
                listener.onDeviceFound(device);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            listener.onStatusChanged("Scan failed: " + errorCode);
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        @SuppressLint("MissingPermission")
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            mainHandler.post(() -> {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    listener.onStatusChanged("Connection failed: " + status);
                    try {
                        gatt.close();
                    } catch (Exception ignored) {
                    }
                    return;
                }

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    listener.onConnected(getDeviceName(gatt.getDevice()));
                    listener.onStatusChanged("Connected. Discovering services...");
                    try {
                        gatt.discoverServices();
                    } catch (SecurityException e) {
                        listener.onStatusChanged("Permission error while discovering services");
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    listener.onDisconnected();
                    listener.onStatusChanged("Disconnected");
                }
            });
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                mainHandler.post(() ->
                        listener.onStatusChanged("Service discovery failed"));
                return;
            }

            BluetoothGattService hrService = gatt.getService(HEART_RATE_SERVICE_UUID);
            if (hrService == null) {
                mainHandler.post(() ->
                        listener.onStatusChanged("Heart rate service not found"));
                return;
            }

            BluetoothGattCharacteristic hrCharacteristic =
                    hrService.getCharacteristic(HEART_RATE_MEASUREMENT_UUID);

            if (hrCharacteristic == null) {
                mainHandler.post(() ->
                        listener.onStatusChanged("Heart rate characteristic not found"));
                return;
            }

            enableHeartRateNotification(gatt, hrCharacteristic);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic,
                                            byte[] value) {
            if (HEART_RATE_MEASUREMENT_UUID.equals(characteristic.getUuid())) {
                int bpm = parseHeartRate(value);
                mainHandler.post(() -> listener.onHeartRateReceived(bpm));
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor,
                                      int status) {
            mainHandler.post(() -> {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    listener.onStatusChanged("Waiting for heart rate data...");
                } else {
                    listener.onStatusChanged("Failed to enable notifications");
                }
            });
        }
    };

    @SuppressLint("MissingPermission")
    private void enableHeartRateNotification(BluetoothGatt gatt,
                                             BluetoothGattCharacteristic characteristic) {
        boolean notifySet;
        try {
            notifySet = gatt.setCharacteristicNotification(characteristic, true);
        } catch (SecurityException e) {
            listener.onStatusChanged("Permission error while enabling notification");
            return;
        }

        if (!notifySet) {
            listener.onStatusChanged("Failed to enable heart rate notification");
            return;
        }

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CONFIG_UUID);
        if (descriptor == null) {
            listener.onStatusChanged("Notification descriptor not found");
            return;
        }

        try {
            int result = gatt.writeDescriptor(
                    descriptor,
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            );

            if (result != BluetoothGatt.GATT_SUCCESS) {
                listener.onStatusChanged("Failed to write notification descriptor");
            }
        } catch (SecurityException e) {
            listener.onStatusChanged("Permission error while writing descriptor");
        }
    }

    private int parseHeartRate(byte[] value) {
        if (value == null || value.length < 2) return 0;

        int flag = value[0] & 0x01;
        if (flag == 0) {
            return value[1] & 0xFF;
        } else {
            if (value.length < 3) return 0;
            return ((value[2] & 0xFF) << 8) | (value[1] & 0xFF);
        }
    }
}