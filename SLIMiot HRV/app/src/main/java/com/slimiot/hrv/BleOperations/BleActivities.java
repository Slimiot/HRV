package com.slimiot.hrv.BleOperations;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.utils.HexUtil;
import com.slimiot.hrv.Helper.General;

import java.sql.Timestamp;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import static com.slimiot.hrv.Config.Constants.characteristicBaroPressure;
import static com.slimiot.hrv.Config.Constants.characteristicBaroTemperature;
import static com.slimiot.hrv.Config.Constants.characteristicBatteryLevel;
import static com.slimiot.hrv.Config.Constants.characteristicECG;
import static com.slimiot.hrv.Config.Constants.characteristicHTS;
import static com.slimiot.hrv.Config.Constants.characteristicHeartRate;
import static com.slimiot.hrv.Config.Constants.characteristicIMU;
import static com.slimiot.hrv.Config.Constants.characteristicPPG;
import static com.slimiot.hrv.Config.Constants.characteristicPowerOn;
import static com.slimiot.hrv.Config.Constants.serviceBarometer;
import static com.slimiot.hrv.Config.Constants.serviceBatteryLevel;
import static com.slimiot.hrv.Config.Constants.serviceECG;
import static com.slimiot.hrv.Config.Constants.serviceHTS;
import static com.slimiot.hrv.Config.Constants.serviceHeartRate;
import static com.slimiot.hrv.Config.Constants.serviceIMU;
import static com.slimiot.hrv.Config.Constants.servicePPG;
import static com.slimiot.hrv.Config.Constants.servicePowerOn;

public class BleActivities {

    public static final String KEY_DATA = "key_data";
    /* Frequency */
    public static String frequency_ECG = "3";
    public static String frequency_PPG = "3";
    public static String frequency_PPG_Raw = "3";
    public static String frequency_HeartRate = "3";
    public static  String frequency_BatteryLevel = "3";
    public static String frequency_HTS = "3";
    public static String frequency_Barometer = "3";

    public static String frequency_Imu = "1";

    /* Service Codes */
    public static String serviceCode_ECG = "1";
    public static String serviceCode_PPG = "2";
    public static  String serviceCode_PPG_Raw = "3";
    public static String serviceCode_HeartRate = "4";
    public static String serviceCode_BatteryLevel = "1";
    public static String serviceCode_HTS = "1";
    public static String serviceCode_Barometer = "1";

    public static String serviceCode_Imu = "1";

    /* Current On Off Status */
    public static boolean service_Status_ECG = false;
    public static boolean service_Status_PPG = false;
    public static boolean service_Status_PPG_Raw = false;
    public static boolean service_Status_HeartRate = false;
    public static boolean service_Status_BatteryLevel = false;
    public static boolean service_Status_HTS = false;
    public static boolean service_Status_Barometer = false;

    public static boolean service_Status_IMU = false;

    private static String TAG = "BleActivities";
    private static BleDevice bleDevice; // used in this file only
    private  static Context mContext;
    public   static boolean hrPPGState = false;  // true means hr is on else ppg is on..........
    public   static  boolean measureHRV = false; // true means user start using Heart Rate Service && then we have to  maintain that there is no interrupt happen..
    public static  boolean measureHRV_Breathing = false; // true -> measuring hrv while performing breathing exercise else false; && ppg off while is it measuring...
    public static boolean performHrMaxVo2Lactate = false; // true -> if performing the test hrMax,Vo2 , Lactate using the Cooper test, 4x2 test,30 min test.
    public static boolean measureECG = false; // true -> measuring ECG signal  // at that time -> HeartRate & PPG Sensor is off.
    public static int fowardCalibrateAngle = 0;
    public static int sideAngleCalibrate = 0;
    public static int k =0;

    public static BleDevice mBleDevice = null; // used in this whole app only

//    public BleActivities(BleDevice bleDevice){
//        this.bleDevice = bleDevice;
//    }

    public static String getServiceCode() {
        String serviceCode = "";

        /* Battery Level */
        if (service_Status_BatteryLevel == true) {
            serviceCode = serviceCode + serviceCode_BatteryLevel + frequency_BatteryLevel;
        } else {
            serviceCode = serviceCode + "00";
        }

        /* Barometer */
        if (service_Status_Barometer == true) {
            serviceCode = serviceCode + serviceCode_Barometer + frequency_Barometer;
        } else {
            serviceCode = serviceCode + "00";
        }

        /* HTS */
        if (service_Status_HTS == true) {
            serviceCode = serviceCode + serviceCode_HTS + frequency_HTS;
        } else {
            serviceCode = serviceCode + "00";
        }

        /* IMU */
        if(service_Status_IMU == true){
            serviceCode = serviceCode + serviceCode_Imu + frequency_Imu;
        }else{
            serviceCode = serviceCode + "00";
        }

        /* Custom Config */
        if (service_Status_ECG == true) {
            serviceCode = serviceCode + serviceCode_ECG + frequency_ECG;
        } else if (service_Status_PPG == true) {
            serviceCode = serviceCode + serviceCode_PPG + frequency_PPG;
        } else if (service_Status_PPG_Raw == true) {
            serviceCode = serviceCode + serviceCode_PPG_Raw + frequency_PPG_Raw;
        } else if (service_Status_HeartRate == true) {
            serviceCode = serviceCode + serviceCode_HeartRate + frequency_HeartRate;
        } else {
            serviceCode = serviceCode + "00";
        }
        serviceCode = serviceCode + "0030" + "0030";

        Log.i(TAG, "hexCode " + serviceCode.toString());

        return serviceCode;
    }

    public static void onHeartRate() {
        service_Status_ECG = false;
        service_Status_PPG = false;
        service_Status_PPG_Raw = false;
        service_Status_HeartRate = true;
        String hexCode = getServiceCode();
        String successMessage = "Heart Rate Service is on.";
       // writeCharacteristic(hexCode, servicePowerOn, characteristicPowerOn, successMessage, "HeartRate");
        //readCharacteristic(serviceHeartRate, characteristicHeartRate);
        notifyCharacteristic(serviceHeartRate, characteristicHeartRate, "HeartRate");

    }

    public static void offHeartRate() {
        service_Status_HeartRate = false;
        String hexCode = getServiceCode();
        String successMessage = "Heart Rate Service is off.";
        writeCharacteristic(hexCode, servicePowerOn, characteristicPowerOn, successMessage, "");
//        txtDisplay_HeartRate.setText("");
        notifyCharacteristicStop(bleDevice, serviceHeartRate, characteristicHeartRate);
    }

    public static void onIMU() {
//        txtChangeAble = true;
        service_Status_IMU = true;
        String hexCode = getServiceCode();
        String successMessage = "IMU Service is on.";
//        accelerometerValues = new float[3];
//        gyroscopeValues = new float[3];
//        timestamp = 0;
        writeCharacteristic(hexCode, servicePowerOn, characteristicPowerOn, successMessage, "IMU");
    }

    public static void offIMU() {
//        txtChangeAble = false; // for displaying the value for 3 seconds..
        service_Status_IMU = false;
        String hexCode = getServiceCode();
        String successMessage = "IMU Service is off.";
        writeCharacteristic(hexCode, servicePowerOn, characteristicPowerOn, successMessage, "");
        notifyCharacteristicStop(bleDevice, serviceIMU, characteristicIMU);
    }
    public static void onHTS() {
        service_Status_HTS = true;
        String hexCode = getServiceCode();
        String successMessage = "HTS Service is on.";
        writeCharacteristic(hexCode, servicePowerOn, characteristicPowerOn, successMessage, "HTS");
    }

    public static void offHTS() {
        service_Status_HTS = false;
        String hexCode = getServiceCode();
        String successMessage = "HTS Service is off.";
        writeCharacteristic(hexCode, servicePowerOn, characteristicPowerOn, successMessage, "");
        notifyCharacteristicStop(bleDevice, serviceHTS, characteristicHTS);
    }
    public static void onBarometer() {
        service_Status_Barometer = true;
        String hexCode = getServiceCode();
        String successMessage = "Barometer Service is on.";
        writeCharacteristic(hexCode, servicePowerOn, characteristicPowerOn, successMessage, "Barometer");
    }

    public static void offBarometer() {
        service_Status_Barometer = false;
        String hexCode = getServiceCode();
        String successMessage = "Barometer Service is off.";
        writeCharacteristic(hexCode, servicePowerOn, characteristicPowerOn, successMessage, "");
        notifyCharacteristicStop(bleDevice, serviceBarometer, characteristicBaroPressure);
//        notifyCharacteristicStop(bleDevice, serviceBarometer, characteristicBaroTemperature);
    }
    public static void onPPG() {
        service_Status_ECG = false;
        service_Status_PPG = true;
        service_Status_PPG_Raw = false;
        service_Status_HeartRate = false;
        String hexCode = getServiceCode();
        String successMessage = "PPG Service is on.";
        writeCharacteristic(hexCode, servicePowerOn, characteristicPowerOn, successMessage, "PPG");
    }

    public static void offPPG() {
        service_Status_PPG = false;
        String hexCode = getServiceCode();
        String successMessage = "PPG Service is off.";
        writeCharacteristic(hexCode, servicePowerOn, characteristicPowerOn, successMessage, "");
        notifyCharacteristicStop(bleDevice, serviceHeartRate, characteristicHeartRate);
    }

    public static void onECG(){
        service_Status_ECG = true;
        service_Status_PPG = false;
        service_Status_PPG_Raw = false;
        service_Status_HeartRate = false;
        String hexCode = getServiceCode();
        String successMessage = "ECG Service is on.";
        //  ********* Ring
        writeCharacteristic(hexCode, servicePowerOn, characteristicPowerOn, successMessage, "ECG");
    }
    public static void offECG() {
        service_Status_ECG = false;
        String hexCode = getServiceCode();
        String successMessage = "ECG Service is off.";
        writeCharacteristic(hexCode, servicePowerOn, characteristicPowerOn, successMessage, "");
        notifyCharacteristicStop(bleDevice, serviceECG, characteristicECG);
    }

    public static void writeCharacteristic(String hex, String serviceUUID, String characteristicUUID, String successMessage, String serviceName) {
        BleDevice bleDevice = getBleDevice();
        BleManager.getInstance().write(
                bleDevice,
                serviceUUID,
                characteristicUUID,
                HexUtil.hexStringToBytes(hex),
                new BleWriteCallback() {

                    @Override
                    public void onWriteSuccess(final int current, final int total, final byte[] justWrite) {
                        Log.i(TAG, "write success, current: " + current
                                + " total: " + total
                                + " justWrite: " + HexUtil.formatHexString(justWrite, true));

                        if (!successMessage.equals("")) {
                            General.showToast(mContext, successMessage);
                        }

                        if (!serviceName.equals("")) {
                            switch (serviceName) {
                                case "HTS":
                                    notifyCharacteristic(serviceHTS, characteristicHTS, serviceName);
                                    break;
                                case "IMU":
                                    notifyCharacteristic(serviceIMU, characteristicIMU, serviceName);
                                    break;
                                case "HeartRate":
                                    notifyCharacteristic(serviceHeartRate, characteristicHeartRate, serviceName);
                                    break;
                                case "BatteryLevel":
                                    notifyCharacteristic(serviceBatteryLevel, characteristicBatteryLevel, serviceName);
                                    break;
                                case "ECG":
                                    notifyCharacteristic(serviceECG, characteristicECG, serviceName);
                                    break;
                                case "PPG":
                                    notifyCharacteristic(servicePPG, characteristicPPG, serviceName);
                                    break;
                                case "Barometer":
                                    try {
                                        notifyCharacteristic(serviceBarometer, characteristicBaroPressure, "BarometerPressure");
                                        Thread.sleep(1000); // 1000 milliseconds = 1 seconds
                                        notifyCharacteristic(serviceBarometer, characteristicBaroTemperature, "BarometerTemperature");
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    break;
                            }
                        }
                    }

                    @Override
                    public void onWriteFailure(final BleException exception) {
                        Log.i("READ_Prakhar_Write-Ring", exception.toString());
//                        Toast.makeText(MainActivity.this, exception.toString(), Toast.LENGTH_SHORT).show();
                        General.showToast(mContext, "" + exception.toString());
                    }
                });
    }

    public static void notifyCharacteristic(String sUUID, String cUUID, String serviceName) {
        try {
            Log.i(TAG, "notifyCharacteristic() is called");
            Log.i("READ_Prakhar_Notify", "Notify_characteristics_called");

            BleDevice bleDevice = getBleDevice();

            BleManager.getInstance().notify(
                    bleDevice,
                    sUUID,
                    cUUID,
                    new BleNotifyCallback() {

                        @Override
                        public void onNotifySuccess() {
                            // Heart Rate // HTS // Baro
                            Log.i("onNotifySuccess",serviceName);
//                            Toast.makeText(mContext, serviceName, Toast.LENGTH_SHORT).show();

                        }

                        @Override
                        public void onNotifyFailure(final BleException exception) {

                            Log.i("READ_Prakhar_NotifyE",exception.toString());

//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    Log.i("READ_Prakhar_NotifyF", "Notify_failure_called");
//                                    General.showToast(mContext, exception.toString());
//                                }
//                            });
                        }

                        @Override
                        public void onCharacteristicChanged(byte[] data) {

                            Handler handler = new Handler();

                            Runnable runn = new Runnable() {
                                @Override
                                public void run() {
//                                    Log.i("READ_Prakhar_Notify", "Notify_characteristics_changed_called");

                                    String content = HexUtil.formatHexString(data, true);

                                    Log.i("READ_Prakhar Hello " + serviceName, content);
//
                                    updateUI(serviceName, data);
                                }
                            };
                            handler.post(runn);
                        }
                    });
        } catch (Exception e) {
            Log.i("READ_Prakhar_Notify", e.toString());
            e.printStackTrace();
        }
    } // end notifyCharacteristic()

    public static void notifyCharacteristicStop(BleDevice bleDevice, String sUUID, String cUUID) {
        try {
            BleManager.getInstance().stopNotify(
                    bleDevice,
                    sUUID,
                    cUUID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateUI(String serviceName, byte[] data) {
        String content = HexUtil.formatHexString(data, true);
        Log.i("READ_UI_DATA_" + serviceName, content);
        switch (serviceName) {
            case "HeartRate":
//                updateUI_HeartRate(data);
                sendHeartRateData(data);
                break;
            case "PPG":
                sendPPGData(data);
                break;
            case "HTS":
                sendHTSData(data);
                break;
            case "IMU":
                sendIMUData(data);
                break;
            case "BarometerPressure":
                sendBarometerPressure(data);
                break;
            case "BarometerTemperature":
                sendBarometerTemperature(data);
                break;
            case "ECG":
                sendECGData(data);
                break;
        }
    }

    private static void sendECGData(byte[] data) {
        Intent intent = new Intent("sensor-dataECG");
        intent.putExtra("data", data);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    private static void sendBarometerTemperature(byte[] data) {
        Intent intent = new Intent("sensor-dataBaroTemperature");
        intent.putExtra("data", data);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    private static void sendBarometerPressure(byte[] data) {
        Intent intent = new Intent("sensor-dataBaroPressure");
        intent.putExtra("data", data);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    private static void sendIMUData(byte[] data) {
        if(data.length == 4){
            long result = byteArrayToLong(data)*1000;

            Timestamp timestamp = new Timestamp(result);
            // Extracting time components from Timestamp
            int hours = timestamp.getHours();
            int minutes = timestamp.getMinutes();
            int seconds = timestamp.getSeconds();

            int year = timestamp.getYear();
            int month = timestamp.getMonth();
            int day = timestamp.getDay();

            Log.i("READ_Prakhar_Time",hours+" : "+minutes+" : "+seconds+" "+day+" "+month+" "+year);
        }
        Intent intent = new Intent("sensor-dataIMU");
        intent.putExtra("data", data);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    public static long byteArrayToLong(byte[] byteArray) {
        long result = 0;
        int l = byteArray.length;
        for (int i = l-1; i >=0 ; i--) {
            result = (result << 8) | (byteArray[i] & 0xFF);
        }
        return result;
    }

    private static void sendHTSData(byte[] data) {
        Intent intent = new Intent("sensor-dataHTS");
        intent.putExtra("data", data);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    private static void sendPPGData(byte[] data) {
        Intent intent = new Intent("sensor-dataPPG");
        intent.putExtra("data", data);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    private static void sendHeartRateData(byte[] data) {
        Intent intent = new Intent("sensor-dataHR");
        intent.putExtra("data", data);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    public static BleDevice getBleDevice() {
        return bleDevice;
    }
    public static void setBleDevice(BleDevice mbleDevice){
        bleDevice = mbleDevice;
    }
    public static void setContext(Context context){
                 mContext = context;
    }

}
