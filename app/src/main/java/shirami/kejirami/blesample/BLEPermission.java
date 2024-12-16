package shirami.kejirami.blesample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

public class BLEPermission {
    private final int REQUEST_ENABLE_BT = 1;
    private ComponentActivity mActivity;
    private BluetoothAdapter mBluetoothAdapter;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<String> mGetContent;

    public BLEPermission(ComponentActivity activity, BluetoothAdapter bluetoothAdapter){
        mActivity = activity;
        mBluetoothAdapter = bluetoothAdapter;
        ActivityResultLauncher<String> requestPermissionLauncher = mActivity.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (!isGranted) {
                        Toast.makeText( mActivity, "権限がありません", Toast.LENGTH_LONG ).show();
                        mActivity.finish();
                    }
                }
        );
    }

    // 権限の要求
    private boolean requestPermission() {
        if(ActivityCompat.checkSelfPermission(mActivity, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_SCAN);
            return false;
        }else if (ActivityCompat.checkSelfPermission(mActivity, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT);
            return false;
        }else if (ActivityCompat.checkSelfPermission(mActivity, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
            return false;
        }
        return true;
    }

    // Android端末のBluetooth機能の有効化要求
    @SuppressLint("MissingPermission")
    private boolean requestBluetoothFeature() {
        if( mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() ) return true;

        // デバイスのBluetooth機能が有効になっていないときは、有効化要求（ダイアログ表示）
        Intent intent = new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE );
        mActivity.startActivityForResult( intent, REQUEST_ENABLE_BT );
        return false;
    }
}
