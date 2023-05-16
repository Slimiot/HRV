package com.slimiot.hrv.BleAction;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.utils.HexUtil;
import com.slimiot.hrv.BleOperations.BleActivities;
import com.slimiot.hrv.FastBle.comm.ObserverManager;
import com.slimiot.hrv.FastBle.operation.OperationActivity;
import com.slimiot.hrv.FastBleScan.FastBleScanFragment;
import com.slimiot.hrv.Helper.General;
import com.slimiot.hrv.Home.MainActivity;
import com.slimiot.hrv.R;

import java.nio.ByteBuffer;
import java.util.UUID;

import static com.slimiot.hrv.Config.Constants.characteristicTimeStamp;
import static com.slimiot.hrv.Config.Constants.serviceTimeStamp;

public class BluetoothConnection extends AppCompatActivity {

    private ImageView skipImage, logoImage;
    private TextView skipText;

    private Button connectNow;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connection);
        getSupportActionBar().hide();
        skipImage = findViewById(R.id.skip_bluetooth_img);
        skipText = findViewById(R.id.skip_bluetooth_text);
        connectNow = findViewById(R.id.connectBleDeviceNow);
        logoImage = findViewById(R.id.app_logo_slimiot);
        progressDialog = new ProgressDialog(BluetoothConnection.this);
        skipImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                skipBle();
            }
        });
        skipText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                skipBle();
            }
        });
        connectNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doSomething();
            }
        });
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
//                    img_loading.clearAnimation();
//                    img_loading.setVisibility(View.INVISIBLE);
//                    btn_scan.setText(getString(R.string.start_scan));
                    progressDialog.dismiss();
                    General.showToast(BluetoothConnection.this, getString(R.string.connect_fail));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @SuppressLint("MissingPermission")
            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                try {
                    progressDialog.dismiss();
//                    mDeviceAdapter.addDevice(bleDevice);
//                    mDeviceAdapter.notifyDataSetChanged();
                    //General.showToast(mContext, "Connected with device");

                    if (BleManager.getInstance().isConnected(bleDevice)) {
                        //Intent intent = new Intent(mContext, OperationActivity.class);

//                        Intent intent = new Intent(mContext, CustomOperationActivity.class);
//                        intent.putExtra(OperationActivity.KEY_DATA, bleDevice);
//                        startActivity(intent);

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


                        Intent intent = new Intent(BluetoothConnection.this, MainActivity.class);
                        intent.putExtra(OperationActivity.KEY_DATA, bleDevice);
                        startActivity(intent);



                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // check charateristics write success or fail....

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // Timestamp data written successfully
                    // Handle any additional logic here
                    Log.i("Data_Write_","Success");
                }else{
                    Log.i("Data_Write_","Fail");
                }
            }

            // ************************



            @SuppressLint("MissingPermission")
            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice bleDevice, BluetoothGatt gatt, int status) {
                try {
                    progressDialog.dismiss();
//                    mDeviceAdapter.removeDevice(bleDevice);
//                    mDeviceAdapter.notifyDataSetChanged();
                    if (isActiveDisConnected) {
                        General.showToast(BluetoothConnection.this, getString(R.string.active_disconnected));
                        Log.d("READ_Pro_Connection", "Disconnected by user");
                    } else {
                        General.showToast(BluetoothConnection.this, getString(R.string.disconnected));
                        Log.d("READ_Pro_Connection", "Disconnected by device");
                        ObserverManager.getInstance().notifyObserver(bleDevice);
                    }
                    // we have to broadcast message from here to our app that app is disconnected
                    BleActivities.mBleDevice = null;
                    String deviceName = gatt.getDevice().getName();
                    String deviceAddress = gatt.getDevice().getAddress();
                    Intent intent = new Intent("DEVICE_DISCONNECTED");
                    intent.putExtra("deviceName", deviceName);
                    intent.putExtra("deviceAddress", deviceAddress);
                    LocalBroadcastManager.getInstance(BluetoothConnection.this).sendBroadcast(intent);



                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
    private byte[] getTimestampData() {
        long timestamp = System.currentTimeMillis();

        long epochTime = System.currentTimeMillis() / 1000;

        byte[] timestampByteArray = ByteBuffer.allocate(Long.BYTES).putLong(epochTime).array();
        byte[] sendB = {timestampByteArray[7],timestampByteArray[6],timestampByteArray[5],timestampByteArray[4],timestampByteArray[3],timestampByteArray[2],timestampByteArray[1],timestampByteArray[0]};
        return sendB;
    }


    // here it ends for creating the conneection with the previously stored device.........

    public void doSomething() {
        logoImage.setVisibility(View.INVISIBLE);
        connectNow.setVisibility(View.INVISIBLE);
        findViewById(R.id.layout_fragment_home).setVisibility(View.VISIBLE);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.layout_fragment_home, new FastBleScanFragment())
                .commit();
    }

    private void skipBle() {
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
    }
    @Override
    public void onPause() {
        super.onPause();
        overridePendingTransition(0, 0);
    }
}