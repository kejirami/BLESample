package shirami.kejirami.blesample;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

abstract public class RFIDActivity extends AppCompatActivity {

    abstract protected void onReceiveValue(String value);
    private static final String BT_ADDRESS = "0C:8B:95:A8:48:82";
    private static final UUID UUID_NOTIFY = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_SERVICE_PRIVATE = UUID.fromString("88752198-5fa5-4ff8-8af2-b3f9465feef8");
    private static final UUID UUID_CHARACTERISTIC_PRIVATE = UUID.fromString("0514f974-b78f-4218-beb5-83cd0268a71d");
    private static final long SCAN_PERIOD = 10000;
    private final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt = null;
    private Handler mHandler;
    private boolean mScanning;
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (!isGranted) {
                    Toast.makeText(this, "権限がありません", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
    );

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (result.getDevice() != null) {
                mBluetoothDevice = result.getDevice();
                scanLeDevice(false);
                if (mBluetoothGatt != null) return;
                if (ActivityCompat.checkSelfPermission(RFIDActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    mBluetoothGatt = mBluetoothDevice.connectGatt(RFIDActivity.this, false, gattCallback);
                }
            }
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (ActivityCompat.checkSelfPermission(RFIDActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mBluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mBluetoothGatt = mBluetoothDevice.connectGatt(RFIDActivity.this, false, gattCallback);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            for( BluetoothGattService service : gatt.getServices() ) {
                if( service != null && UUID_SERVICE_PRIVATE.equals(service.getUuid()) ) {
                    setCharacteristicNotification(UUID_SERVICE_PRIVATE,UUID_CHARACTERISTIC_PRIVATE,true);
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            readCharacteristic(UUID_SERVICE_PRIVATE,UUID_CHARACTERISTIC_PRIVATE);
                        }
                    }, 500);
                }
            }
        }

        @Override
        public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {
            // キャラクタリスティックごとに個別の処理
            if( UUID_CHARACTERISTIC_PRIVATE.equals( characteristic.getUuid() ) ) {
                runOnUiThread( new Runnable() {
                    public void run() {
                        String s = bin2hex(value);
                        if("000000000000000000000000".equals(s)) return;
                        onReceiveValue(s);
                    }
                });
            }
        }

        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            // キャラクタリスティックごとに個別の処理
            if( UUID_CHARACTERISTIC_PRIVATE.equals( characteristic.getUuid() ) ) {
                runOnUiThread( new Runnable() {
                    public void run() {
                        String s = bin2hex(value);
                        if("000000000000000000000000".equals(s)) return;
                        onReceiveValue(s);
                    }
                });
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // BLEをサポートしてるかの確認
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLEをサポートしていません", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Bluetoothアダプターを取得
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetoothをサポートしていません", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mHandler = new Handler();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!requestPermission()) return;
        if (!requestBluetoothFeature()) return;
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (mBluetoothLeScanner == null) return;
        scanLeDevice(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        disconnect();
    }

    // Android端末のBluetooth機能の有効化要求
    @SuppressLint("MissingPermission")
    private boolean requestBluetoothFeature() {
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) return true;

        // デバイスのBluetooth機能が有効になっていないときは、有効化要求（ダイアログ表示）
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intent, REQUEST_ENABLE_BT);
        return false;
    }

    // 権限の要求
    private boolean requestPermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_SCAN);
            return false;
        } else if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT);
            return false;
        } else if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION);
            return false;
        }
        return true;
    }

    // 機能の有効化ダイアログの操作結果
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        switch (requestCode) {
            // Bluetooth有効化要求
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_CANCELED) {
                    Toast.makeText(this, "Bluetoothを有効にしてください", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void scanLeDevice(final boolean enable) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @SuppressLint("MissingPermission")
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothLeScanner.stopScan(leScanCallback);
                }
            }, SCAN_PERIOD);

            ScanFilter filter = new ScanFilter.Builder().setDeviceAddress(BT_ADDRESS).build();
            List<ScanFilter> filters = new ArrayList<ScanFilter>();
            filters.add(filter);

            ScanSettings settings = new ScanSettings.Builder().setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH).build();

            mScanning = true;
            try {
                mBluetoothLeScanner.startScan(filters, settings, leScanCallback);
            } catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            mScanning = false;
            mBluetoothLeScanner.stopScan(leScanCallback);
        }
    }

    // 切断
    private void disconnect() {
        if (mBluetoothGatt == null) return;

        if (ActivityCompat.checkSelfPermission(RFIDActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    // キャラクタリスティック通知の設定
    private void setCharacteristicNotification(UUID uuid_service, UUID uuid_characteristic, boolean enable) {
        if (mBluetoothGatt == null) return;
        if (ActivityCompat.checkSelfPermission(RFIDActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        BluetoothGattCharacteristic gattchar = mBluetoothGatt.getService(uuid_service).getCharacteristic(uuid_characteristic);
        mBluetoothGatt.setCharacteristicNotification(gattchar, enable);
        BluetoothGattDescriptor descriptor = gattchar.getDescriptor( UUID_NOTIFY );
        descriptor.setValue( BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE );
        mBluetoothGatt.writeDescriptor( descriptor );
    }

    // キャラクタリスティックの読み込み
    private void readCharacteristic( UUID uuid_service, UUID uuid_characteristic ) {
        if (mBluetoothGatt == null) return;
        if (ActivityCompat.checkSelfPermission(RFIDActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        BluetoothGattCharacteristic gattchar = mBluetoothGatt.getService( uuid_service ).getCharacteristic( uuid_characteristic );
        mBluetoothGatt.readCharacteristic( gattchar );
    }

    private static String bin2hex(byte[] data) {
        StringBuffer sb = new StringBuffer();
        for (byte b : data) {
            String s = Integer.toHexString(0xff & b);
            if (s.length() == 1) {
                sb.append("0");
            }
            sb.append(s);
        }
        return sb.toString();
    }}