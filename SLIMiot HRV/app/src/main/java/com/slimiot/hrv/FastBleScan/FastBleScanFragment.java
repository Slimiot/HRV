package com.slimiot.hrv.FastBleScan;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleMtuChangedCallback;
import com.clj.fastble.callback.BleRssiCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.BleScanRuleConfig;
import com.clj.fastble.utils.HexUtil;
import com.slimiot.hrv.BleOperations.BleActivities;
import com.slimiot.hrv.FastBle.adapter.DeviceAdapter;
import com.slimiot.hrv.FastBle.comm.ObserverManager;
import com.slimiot.hrv.FastBle.operation.OperationActivity;
import com.slimiot.hrv.Helper.General;
import com.slimiot.hrv.Home.MainActivity;
import com.slimiot.hrv.R;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import static android.app.Activity.RESULT_OK;
import static androidx.core.content.ContextCompat.checkSelfPermission;
import static com.slimiot.hrv.Config.Constants.characteristicTimeStamp;
import static com.slimiot.hrv.Config.Constants.serviceTimeStamp;

public class FastBleScanFragment extends Fragment {

    Context mContext;
    String TAG = "FastBleScanFragment";

    private boolean mAllPermissionsAsked;
    private Handler mHandler;

    public static final int REQUEST_CODE_OPEN_GPS = 1;
    public static final int REQUEST_CODE_PERMISSION_LOCATION = 2;
    public static final int REQUEST_CODE_OPEN_BLUETOOTH = 4;

    int REQ_ACCESS_PERMISSIONS = 1;

    Button btn_scan;
    EditText et_name, et_mac, et_uuid;
    Switch sw_auto;
    ImageView img_loading;

    Animation operatingAnim;
    DeviceAdapter mDeviceAdapter;
    ProgressDialog progressDialog;

    private FastBleScanViewModel mViewModel;

    public static FastBleScanFragment newInstance() {
        return new FastBleScanFragment();
    }

    @SuppressLint("MissingPermission")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        //return inflater.inflate(R.layout.fragment_fast_ble_scan, container, false);

        LayoutInflater layoutInflater = (LayoutInflater) getActivity().getApplicationContext().getSystemService(getContext().LAYOUT_INFLATER_SERVICE);
        View customView = layoutInflater.inflate(R.layout.fragment_fast_ble_scan, null);
        customView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        init(customView);

        setListener();

        BleManager.getInstance().init(getActivity().getApplication());
        BleManager.getInstance()
                .enableLog(true)
                .setReConnectCount(1, 5000)
                .setConnectOverTime(20000)
                .setOperateTimeout(5000);

        // Ask for needed permissions
        //requestPermissions();

        /*try {
            BluetoothManager bluetoothManager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager != null) {
                BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
                if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    mContext.startActivity(enableBtIntent);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        
        try {
            if (btn_scan.getText().equals(getString(R.string.start_scan))) {
                //checkPermissions();
                onClickScan();
            } else if (btn_scan.getText().equals(getString(R.string.stop_scan))) {
                BleManager.getInstance().cancelScan();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return customView;
    }

    @Override
    public void onResume() {
        super.onResume();
        showConnectedDevice();
    }

    @Override
    public void onStart() {
        super.onStart();
        requestPermissions();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        BleManager.getInstance().disconnectAllDevice();
        BleManager.getInstance().destroy();
    }

    public void init(View customView) {
        mContext = getContext();

        mHandler = new Handler();

        btn_scan = (Button) customView.findViewById(R.id.btn_scan);
        btn_scan.setText(getString(R.string.start_scan));

        img_loading = (ImageView) customView.findViewById(R.id.img_loading);
        operatingAnim = AnimationUtils.loadAnimation(mContext, R.anim.rotate);
        operatingAnim.setInterpolator(new LinearInterpolator());
        progressDialog = new ProgressDialog(mContext);

        mDeviceAdapter = new DeviceAdapter(mContext);
        ListView listView_device = (ListView) customView.findViewById(R.id.list_device);
        listView_device.setAdapter(mDeviceAdapter);
    }

    @SuppressLint("MissingPermission")
    public void showConnectedDevice() {
        List<BleDevice> deviceList = BleManager.getInstance().getAllConnectedDevice();
        mDeviceAdapter.clearConnectedDevice();
        for (BleDevice bleDevice : deviceList) {
            mDeviceAdapter.addDevice(bleDevice);
        }
        mDeviceAdapter.notifyDataSetChanged();
    }

    @SuppressLint("MissingPermission")
    public void setScanRule() {
        String[] uuids;
        uuids = null;
        /*String str_uuid = et_uuid.getText().toString();
        if (TextUtils.isEmpty(str_uuid)) {
            uuids = null;
        } else {
            uuids = str_uuid.split(",");
        }*/
        UUID[] serviceUuids = null;
        if (uuids != null && uuids.length > 0) {
            serviceUuids = new UUID[uuids.length];
            for (int i = 0; i < uuids.length; i++) {
                String name = uuids[i];
                String[] components = name.split("-");
                if (components.length != 5) {
                    serviceUuids[i] = null;
                } else {
                    serviceUuids[i] = UUID.fromString(uuids[i]);
                }
            }
        }

        String[] names;
        names = null;
        /*String str_name = et_name.getText().toString();
        if (TextUtils.isEmpty(str_name)) {
            names = null;
        } else {
            names = str_name.split(",");
        }*/

        //String mac = et_mac.getText().toString();

        //boolean isAutoConnect = sw_auto.isChecked();
        boolean isAutoConnect = true;

        BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
                .setServiceUuids(serviceUuids)
                .setDeviceName(true, names)
                //.setDeviceMac(mac)
                .setAutoConnect(isAutoConnect)
                .setScanTimeOut(10000)
                .build();
        BleManager.getInstance().initScanRule(scanRuleConfig);
    }

    @SuppressLint("MissingPermission")
    public void startScan() {
        try {
            BleManager.getInstance().scan(new BleScanCallback() {
                @SuppressLint("MissingPermission")
                @Override
                public void onScanStarted(boolean success) {
                    try {
                        mDeviceAdapter.clearScanDevice();
                        mDeviceAdapter.notifyDataSetChanged();
                        img_loading.startAnimation(operatingAnim);
                        img_loading.setVisibility(View.VISIBLE);
                        btn_scan.setText(getString(R.string.stop_scan));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @SuppressLint("MissingPermission")
                @Override
                public void onLeScan(BleDevice bleDevice) {
                    try {
                        super.onLeScan(bleDevice);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @SuppressLint("MissingPermission")
                @Override
                public void onScanning(BleDevice bleDevice) {
                    try {
                        mDeviceAdapter.addDevice(bleDevice);
                        mDeviceAdapter.notifyDataSetChanged();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @SuppressLint("MissingPermission")
                @Override
                public void onScanFinished(List<BleDevice> scanResultList) {
                    try {
                        img_loading.clearAnimation();
                        img_loading.setVisibility(View.INVISIBLE);
                        btn_scan.setText(getString(R.string.start_scan));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("MissingPermission")
    public void connect(final BleDevice bleDevice) {
        BleManager.getInstance().connect(bleDevice, new BleGattCallback() {

            @SuppressLint("MissingPermission")
            @Override
            public void onStartConnect() {
                try {
                    progressDialog.setTitle("Please wait ...");
                    progressDialog.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @SuppressLint("MissingPermission")
            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
                try {
                    img_loading.clearAnimation();
                    img_loading.setVisibility(View.INVISIBLE);
                    btn_scan.setText(getString(R.string.start_scan));
                    progressDialog.dismiss();
                    General.showToast(mContext, getString(R.string.connect_fail));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @SuppressLint("MissingPermission")
            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                try {
                    progressDialog.dismiss();
                    mDeviceAdapter.addDevice(bleDevice);
                    mDeviceAdapter.notifyDataSetChanged();
                    //General.showToast(mContext, "Connected with device");

                    if (BleManager.getInstance().isConnected(bleDevice)) {
                        //Intent intent = new Intent(mContext, OperationActivity.class);
//                        Intent intent = new Intent(mContext, CustomOperationActivity.class);

                        //*****************************
                        // Sending timestamp..........................

                        BluetoothGattService service = gatt.getService(UUID.fromString(serviceTimeStamp));
                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicTimeStamp));

                        // Step 3: Enable notifications/indications
                        gatt.setCharacteristicNotification(characteristic, true);

                        // Step 4: Write timestamp data
                        byte[] timestampData = getTimestampData(); // Get the timestamp data as byte array
                        characteristic.setValue(timestampData);
                        boolean val = gatt.writeCharacteristic(characteristic);

                        String content = HexUtil.formatHexString(timestampData, true);
                        Log.i("Data_Write_",String.valueOf(val)+" "+content);

                        //*****************************
                        // ******** changing gateway instead of CustomOperationActivty to MainActivity
                        Intent intent = new Intent(mContext, MainActivity.class);
                        intent.putExtra(OperationActivity.KEY_DATA, bleDevice);
                        startActivity(intent);



//                        Intent intent = new Intent(mContext, MainActivity.class);
//                        intent.putExtra(OperationActivity.KEY_DATA, bleDevice);
//                        startActivity(intent);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @SuppressLint("MissingPermission")
            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice bleDevice, BluetoothGatt gatt, int status) {
                try {
                    progressDialog.dismiss();
                    mDeviceAdapter.removeDevice(bleDevice);
                    mDeviceAdapter.notifyDataSetChanged();
                    if (isActiveDisConnected) {
                        General.showToast(mContext, getString(R.string.active_disconnected));
                    } else {
                        General.showToast(mContext, getString(R.string.disconnected));
                        ObserverManager.getInstance().notifyObserver(bleDevice);
                    }
                    // Broadcast message that device is disconnected......

                    // we have to broadcast message from here to our app that app is disconnected
                    BleActivities.mBleDevice = null;
                    String deviceName = gatt.getDevice().getName();
                    String deviceAddress = gatt.getDevice().getAddress();
                    Intent intent = new Intent("DEVICE_DISCONNECTED");
                    intent.putExtra("deviceName", deviceName);
                    intent.putExtra("deviceAddress", deviceAddress);
                    LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);



                    // ******************************************
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    public void readRssi(BleDevice bleDevice) {
        BleManager.getInstance().readRssi(bleDevice, new BleRssiCallback() {
            @Override
            public void onRssiFailure(BleException exception) {
                Log.i(TAG, "onRssiFailure" + exception.toString());
            }

            @Override
            public void onRssiSuccess(int rssi) {
                Log.i(TAG, "onRssiSuccess: " + rssi);
            }
        });
    }

    @SuppressLint("MissingPermission")
    public void setMtu(BleDevice bleDevice, int mtu) {
        BleManager.getInstance().setMtu(bleDevice, mtu, new BleMtuChangedCallback() {
            @Override
            public void onSetMTUFailure(BleException exception) {
                Log.i(TAG, "onsetMTUFailure" + exception.toString());
            }

            @Override
            public void onMtuChanged(int mtu) {
                Log.i(TAG, "onMtuChanged: " + mtu);
            }
        });
    }

    @SuppressLint("MissingPermission")
    @Override
    public final void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION_LOCATION:
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            onPermissionGranted(permissions[i]);
                        }
                    }
                }
                break;
        }
    }

    public void checkPermissions() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            General.showToast(mContext, getString(R.string.please_open_blue));
            return;
        }

        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
        List<String> permissionDeniedList = new ArrayList<>();
        for (String permission : permissions) {
            int permissionCheck = checkSelfPermission(mContext, permission);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted(permission);
            } else {
                permissionDeniedList.add(permission);
            }
        }
        if (!permissionDeniedList.isEmpty()) {
            String[] deniedPermissions = permissionDeniedList.toArray(new String[permissionDeniedList.size()]);
            ActivityCompat.requestPermissions(getActivity(), deniedPermissions, REQUEST_CODE_PERMISSION_LOCATION);
        }
    }

    public void onPermissionGranted(String permission) {
        switch (permission) {
            case Manifest.permission.ACCESS_FINE_LOCATION:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkGPSIsOpen()) {
                    new AlertDialog.Builder(mContext)
                            .setTitle(R.string.notifyTitle)
                            .setMessage(R.string.gpsNotifyMsg)
                            .setNegativeButton(R.string.cancel,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            getActivity().finish();
                                        }
                                    })
                            .setPositiveButton(R.string.setting,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                            startActivityForResult(intent, REQUEST_CODE_OPEN_GPS);
                                        }
                                    })

                            .setCancelable(false)
                            .show();
                } else {
                    setScanRule();
                    startScan();
                }
                break;
        }
    }

    public boolean checkGPSIsOpen() {
        LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null)
            return false;
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "requestCode = " + requestCode + ", resultCode = " + resultCode);
        /*if (requestCode == REQUEST_CODE_OPEN_GPS) {
            if (checkGPSIsOpen()) {
                setScanRule();
                startScan();
            }
        }*/
        if (requestCode == REQUEST_CODE_OPEN_BLUETOOTH && resultCode == RESULT_OK) {
            setScanRule();
            startScan();
        }
    }

    public void setListener() {
        try {
            btn_scan.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        if (btn_scan.getText().equals(getString(R.string.start_scan))) {
                            //checkPermissions();
                            onClickScan();
                        } else if (btn_scan.getText().equals(getString(R.string.stop_scan))) {
                            BleManager.getInstance().cancelScan();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            mDeviceAdapter.setOnDeviceClickListener(new DeviceAdapter.OnDeviceClickListener() {
                @Override
                public void onConnect(BleDevice bleDevice) {
                    if (!BleManager.getInstance().isConnected(bleDevice)) {
                        BleManager.getInstance().cancelScan();
                        connect(bleDevice);
                    }
                }

                @Override
                public void onDisConnect(final BleDevice bleDevice) {
                    if (BleManager.getInstance().isConnected(bleDevice)) {
                        BleManager.getInstance().disconnect(bleDevice);
                    }
                }

                @Override
                public void onDetail(BleDevice bleDevice) {
                    if (BleManager.getInstance().isConnected(bleDevice)) {
                        //Intent intent = new Intent(mContext, OperationActivity.class);
//                        Intent intent = new Intent(mContext, CustomOperationActivity.class);
                        Intent intent = new Intent(mContext, MainActivity.class);
                        intent.putExtra(OperationActivity.KEY_DATA, bleDevice);
                        startActivity(intent);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("MissingPermission")
    public void onClickScan() {
        try {
            if(!isAllPermissionsGranted(mContext)) {
                if (!mAllPermissionsAsked) {
                    mAllPermissionsAsked = true;
                    General.showToast(mContext, "All of the necessary permissions for this application to work properly have not been granted.");
                    requestPermissions();
                } else {
                    return;
                }
            }

            BluetoothManager bluetoothManager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager != null) {
                BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
                if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
                    //mBluetoothAdapter.enable();
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    //startActivity(enableBtIntent);
                    startActivityForResult(enableBtIntent, REQUEST_CODE_OPEN_BLUETOOTH);
                    return;
                    /*img_loading.clearAnimation();
                    img_loading.setVisibility(View.INVISIBLE);
                    btn_scan.setText(getString(R.string.start_scan));
                    General.showToast(mContext, "Please open Bluetooth");*/
                }
            }

            Runnable mTimer = () -> {
                Log.d(TAG, "Scanning timed out");
                if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_SCAN) !=
                        PackageManager.PERMISSION_GRANTED) {
                    // Do nothing, don't return
                    img_loading.clearAnimation();
                    img_loading.setVisibility(View.INVISIBLE);
                    btn_scan.setText(getString(R.string.start_scan));
                    General.showToast(mContext, "BLUETOOTH_SCAN not granted");
                } /*else {
                    BleManager.getInstance().cancelScan();
                }*/
            };

            mHandler.postDelayed(mTimer, 10000);

            setScanRule();
            startScan();
        } catch (Exception e) {
            e.printStackTrace();
        }
    } // end onClickScan()

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(FastBleScanViewModel.class);
        // TODO: Use the ViewModel
    }

    public static boolean isAllPermissionsGranted(Context ctx) {
        boolean granted = false;
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                // Android 12 and more
                granted = ctx.checkSelfPermission(
                        Manifest.permission.BLUETOOTH_CONNECT) ==
                        PackageManager.PERMISSION_GRANTED |
                        ctx.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) ==
                                PackageManager.PERMISSION_GRANTED;
            } else {
                // Android 11 and less
                granted = ctx.checkSelfPermission(
                        Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED |
                        ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                                PackageManager.PERMISSION_GRANTED;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return granted;
    }

    @SuppressLint("MissingPermission")
    public void requestPermissions() {
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                // Android 12 and more
                this.requestPermissions(new String[]{
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT},
                        REQ_ACCESS_PERMISSIONS);
            } else {
                // Android 11 and less
                this.requestPermissions(new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION},
                        REQ_ACCESS_PERMISSIONS);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private byte[] getTimestampData() {
        long timestamp = System.currentTimeMillis();

        long epochTime = System.currentTimeMillis() / 1000;

        // Convert the long timestamp to byte array, depending on the desired format
        // For example, to send as ASCII characters:

//        String timestampString = String.valueOf(epochTime);
//        return timestampString.getBytes(StandardCharsets.US_ASCII);
        byte[] timestampByteArray = ByteBuffer.allocate(Long.BYTES).putLong(epochTime).array();
        byte[] sendB = {timestampByteArray[7],timestampByteArray[6],timestampByteArray[5],timestampByteArray[4],timestampByteArray[3],timestampByteArray[2],timestampByteArray[1],timestampByteArray[0]};
        return sendB;
    }

} // end class