package shirami.kejirami.blesample;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;

import shirami.kejirami.blesample.adapter.ItemTag;
import shirami.kejirami.blesample.adapter.TagAdapter;

public class MainActivity extends RFIDActivity {

    protected RecyclerView mTagView;
    protected TagAdapter mAdapter;
    protected List<ItemTag> mListTag;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTagView = (RecyclerView)findViewById(R.id.TagView);
        mListTag = new ArrayList<ItemTag>();

        mAdapter = new TagAdapter(mListTag);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mTagView.setHasFixedSize(true);
        mTagView.setLayoutManager(layoutManager);
        mTagView.setAdapter(mAdapter);
    }

    @Override
    protected void onReceiveValue(String value) {
        ItemTag item = new ItemTag(value);
        if(mListTag.contains(item)) return;
        mListTag.add(item);
        mAdapter.notifyDataSetChanged();
    }

}