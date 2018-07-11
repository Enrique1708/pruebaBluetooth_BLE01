package corp.carrizales.pruebabluetooth_ble01;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.UUID;

import static android.content.Context.BLUETOOTH_SERVICE;

/**
 * Created by Luis on 10/07/2018.
 */

public class GattClient{

    private static final String TAG = GattClient.class.getSimpleName();

    public interface OnCounterReadListener {
        void onCounterRead(int value);

        void onConnected(boolean success);
    }
    public static final String MAC_ADDRESS = "B8:27:EB:D2:FE:02";
    public static final UUID UUID_SERVICE = UUID.fromString("ffffffff-ffff-ffff-ffff-fffffffffff0");
    public static final UUID UUID_CHARACTERISTIC_ESTADOMOTICICLETA = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    private Context mContext;
    private OnCounterReadListener mListener;
    private String mDeviceAddress;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT client. Attempting to start service discovery");
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT client");
                mListener.onConnected(false);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                boolean connected = false;

                BluetoothGattService service = gatt.getService(UUID_SERVICE);
                if (service != null) {
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID_CHARACTERISTIC_ESTADOMOTICICLETA);
                    if (characteristic != null) {
                        gatt.setCharacteristicNotification(characteristic, true);

                        /*BluetoothGattDescriptor descriptor = characteristic.getDescriptor(DESCRIPTOR_CONFIG);
                        if (descriptor != null) {
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            connected = gatt.writeDescriptor(descriptor);
                        }
                        */
                    }
                }
                mListener.onConnected(connected);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            readCounterCharacteristic(characteristic);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            readCounterCharacteristic(characteristic);
        }

        /*
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (DESCRIPTOR_CONFIG.equals(descriptor.getUuid())) {
                BluetoothGattCharacteristic characteristic = gatt.getService(UUID_SERVICE).getCharacteristic(UUID_CHARACTERISTIC_ESTADOMOTICICLETA);
                gatt.readCharacteristic(characteristic);
            }
        }
        */

        private void readCounterCharacteristic(BluetoothGattCharacteristic characteristic) {
            if (UUID_CHARACTERISTIC_ESTADOMOTICICLETA.equals(characteristic.getUuid())) {
                byte[] data = characteristic.getValue();
                int value = Ints.fromByteArray(data);
                mListener.onCounterRead(value);
            }
        }
    };

    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);

            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    startClient();
                    break;
                case BluetoothAdapter.STATE_OFF:
                    stopClient();
                    break;
                default:
                    // Do nothing
                    break;
            }
        }
    };

    public void onCreate(Context context, String deviceAddress, OnCounterReadListener listener) throws RuntimeException {
        mContext = context;
        mListener = listener;
        mDeviceAddress = deviceAddress;
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) context.getSystemService(BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
        }

        // Register for system Bluetooth events
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(mBluetoothReceiver, filter);
        if (!mBluetoothAdapter.isEnabled()) {
            Log.w(TAG, "Bluetooth is currently disabled... enabling");
            mBluetoothAdapter.enable();
        } else {
            Log.i(TAG, "Bluetooth enabled... starting client");
            startClient();
        }
    }

    public void onDestroy() {
        mListener = null;

        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
        if (bluetoothAdapter.isEnabled()) {
            stopClient();
        }

        mContext.unregisterReceiver(mBluetoothReceiver);
    }

    public void writeInteractor() {
        BluetoothGattCharacteristic interactor = mBluetoothGatt.getService(UUID_SERVICE).getCharacteristic(UUID_CHARACTERISTIC_ESTADOMOTICICLETA);
        interactor.setValue("!");
        mBluetoothGatt.writeCharacteristic(interactor);
    }

    private boolean checkBluetoothSupport(BluetoothAdapter bluetoothAdapter) {
        if (bluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth is not supported");
            return false;
        }

        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.w(TAG, "Bluetooth LE is not supported");
            return false;
        }

        return true;
    }

    private void startClient() {
        BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(MAC_ADDRESS);
        mBluetoothGatt = bluetoothDevice.connectGatt(mContext, false, mGattCallback);

        if (mBluetoothGatt == null) {
            Log.w(TAG, "Unable to create GATT client");
            return;
        }
    }

    private void stopClient() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }

        if (mBluetoothAdapter != null) {
            mBluetoothAdapter = null;
        }
    }
}
