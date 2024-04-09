package com.blue.bluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.blue.bluetooth.databinding.ActivityMainBinding;


@SuppressLint("MissingPermission")
public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> enableBluetooth; //打開藍牙意圖
    private ActivityResultLauncher<String> requestBluetoothConnect; //請求藍牙連接權限意圖
    private ActivityResultLauncher<String> requestBluetoothScan; //請求藍牙掃描權限意圖
    private ActivityResultLauncher<String> requestLocation; //請求定位權限

    private ActivityMainBinding binding;

    //獲取系統藍牙適配器
    private BluetoothAdapter mBluetoothAdapter;
    //掃描者
    private BluetoothLeScanner scanner;
    //是否正在掃描
    boolean isScanning = false;

    private void registerIntent() {
        //打開藍牙意圖
        enableBluetooth = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                if (isOpenBluetooth()) {
                    BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
                    mBluetoothAdapter = manager.getAdapter();
                    scanner = mBluetoothAdapter.getBluetoothLeScanner();
                    showMsg("藍牙已打開");
                } else {
                    showMsg("藍牙未打開");
                }
            }
        });
        //請求BLUETOOTH_CONNECT權限意圖
        requestBluetoothConnect = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
            if (result) {
                enableBluetooth.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            } else {
                showMsg("Android12中未獲取此權限，無法打開藍牙。");
            }
        });
        //請求BLUETOOTH_SCAN權限意圖
        requestBluetoothScan = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
            if (result) {
                //進行掃描
                startScan();
            } else {
                showMsg("Android12中未獲取此權限，則無法掃描藍牙。");
            }
        });
        //請求定位權限
        requestLocation = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
            if (result) {
                //掃描藍牙
                startScan();
            } else {
                showMsg("Android12以下，6及以上需要定位權限才能掃描設備");
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        registerIntent();
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initView();
    }

    //掃描結果回調
    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("qwer", "onScanResult: " + result.getDevice());
        }
    };

    private void initView() {
        if (isOpenBluetooth()) {
            BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
            mBluetoothAdapter = manager.getAdapter();
            scanner = mBluetoothAdapter.getBluetoothLeScanner();
        }
        //打開藍牙按鈕點擊事件
        binding.btnOpenBluetooth.setOnClickListener(v -> {
            //藍牙是否已打開
            if (isOpenBluetooth()) {
                showMsg("藍牙已打開");
                return;
            }
            //是Android12
            if (isAndroid12()) {
                //檢查是否有BLUETOOTH_CONNECT權限
                if (hasPermission(android.Manifest.permission.BLUETOOTH_CONNECT)) {
                    //打開藍牙
                    enableBluetooth.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
                } else {
                    //請求權限
                    requestBluetoothConnect.launch(android.Manifest.permission.BLUETOOTH_CONNECT);
                }
                return;
            }
            //不是Android12 直接打開藍牙
            enableBluetooth.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
        });
        //掃描藍牙按鈕點擊事件
        binding.btnScanBluetooth.setOnClickListener(v -> {
            if (isAndroid12()) {
                if (!hasPermission(android.Manifest.permission.BLUETOOTH_CONNECT)) {
                    requestBluetoothConnect.launch(android.Manifest.permission.BLUETOOTH_CONNECT);
                    return;
                }
                if (hasPermission(android.Manifest.permission.BLUETOOTH_SCAN)) {
                    //掃描或者停止掃描
                    if (isScanning) stopScan();
                    else startScan();
                } else {
                    //請求權限
                    requestBluetoothScan.launch(android.Manifest.permission.BLUETOOTH_SCAN);
                }
            } else {
                if (hasPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                    //掃描或者停止掃描
                    if (isScanning) stopScan();
                    else startScan();
                } else {
                    requestLocation.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                }
            }
        });
    }

    private void startScan() {
        if (!isScanning) {
            scanner.startScan(scanCallback);
            isScanning = true;
            binding.btnScanBluetooth.setText("停止掃描");
        }
    }

    private void stopScan() {
        if (isScanning) {
            scanner.stopScan(scanCallback);
            isScanning = false;
            binding.btnScanBluetooth.setText("掃描藍牙");
        }
    }

    private boolean isOpenBluetooth() {
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();
        if (adapter == null) {
            return false;
        }
        return adapter.isEnabled();
    }

    private boolean isAndroid12() {
        return true;
    }

    private boolean hasPermission(String permission) {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void showMsg(CharSequence msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
