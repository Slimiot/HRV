package com.slimiot.hrv.FastBle.operation;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PanZoom;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYRegionFormatter;
import com.androidplot.xy.XYSeriesFormatter;
import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleReadCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.utils.HexUtil;

//import com.github.mikephil.charting.charts.LineChart;
//import com.github.mikephil.charting.components.AxisBase;
//import com.github.mikephil.charting.components.XAxis;
//import com.github.mikephil.charting.components.YAxis;
//import com.github.mikephil.charting.data.Entry;
//import com.github.mikephil.charting.data.LineData;
//import com.github.mikephil.charting.data.LineDataSet;
//import com.github.mikephil.charting.formatter.IAxisValueFormatter;
//import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.slimiot.hrv.Config.Constants;
import com.slimiot.hrv.FastBle.BCMBleService;
import com.slimiot.hrv.FastBle.IConstants;
import com.slimiot.hrv.FastBle.adapter.BCMDbAdapter;
import com.slimiot.hrv.FastBle.comm.Observer;
import com.slimiot.hrv.FastBle.comm.ObserverManager;
import com.slimiot.hrv.Helper.General;
import com.slimiot.hrv.R;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


public class CustomOperationActivity extends AppCompatActivity implements Observer {

    Context mContext;
    String TAG = "CustomOperationActivity";

    public static final String KEY_DATA = "key_data";

    int mLastHr = IConstants.INVALID_INT;
    String mLastRr = IConstants.INVALID_STRING;
    long mLastHrDate;
    long mSessionStartTime;

    LinearLayout layout_CustomOperationActivity;
    PopupWindow popupWindowGraphPPG, popupWindowGraphECG;
    XYPlot mPlot;
    boolean mPlotHr = true;
    boolean mPlotRr = true;
    int mPlotInterval = Constants.PLOT_MAXIMUM_AGE;

    BCMDbAdapter mDbAdapter;
    File mDataDir;
    long mPlotStartTime = Constants.INVALID_DATE;
    long mPlotSessionStart = Constants.INVALID_DATE;
    boolean mIsSession = false;
    long mLastRrUpdateTime = Constants.INVALID_DATE;
    long mLastRrTime = Constants.INVALID_DATE;

    final double RR_SCALE = .1;  // to 100 ms to use same axis

    XYSeriesFormatter<XYRegionFormatter> hrFormatter;
    XYSeriesFormatter<XYRegionFormatter> rrFormatter;
    SimpleXYSeries hrSeries;
    SimpleXYSeries rrSeries;


    private BleDevice bleDevice;
    private BluetoothGattService bluetoothGattService;
    private BluetoothGattCharacteristic characteristic;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;

    /* Frequency */
    public String frequency_ECG = "3";
    public String frequency_PPG = "3";
    public String frequency_PPG_Raw = "3";
    public String frequency_HeartRate = "3";
    public String frequency_BatteryLevel = "3";
    public String frequency_HTS = "3";
    public String frequency_Barometer = "3";

    public String frequency_Imu = "1";

    /* Service Codes */
    public String serviceCode_ECG = "1";
    public String serviceCode_PPG = "2";
    public String serviceCode_PPG_Raw = "3";
    public String serviceCode_HeartRate = "4";
    public String serviceCode_BatteryLevel = "1";
    public String serviceCode_HTS = "1";
    public String serviceCode_Barometer = "1";

    public String serviceCode_Imu = "1";

    /* Current On Off Status */
    public boolean service_Status_ECG = false;
    public boolean service_Status_PPG = false;
    public boolean service_Status_PPG_Raw = false;
    public boolean service_Status_HeartRate = false;
    public boolean service_Status_BatteryLevel = false;
    public boolean service_Status_HTS = false;
    public boolean service_Status_Barometer = false;

    public boolean service_Status_IMU = false;


    /* PPG */
    TextView txtBtnOnPPG, txtBtnOffPPG, txtBtnPpgGraph, txtDisplay_PPG_HR, txtDisplay_PPG_SPO2, txtDisplay_PPG_Quality, txtDisplay_PPG_RR;

    /* ECG */
    TextView txtBtnOnECG, txtBtnOffECG, txtBtnEcgGraph, txtDisplay_ECG;

    Boolean ecg_graph = false;

    LineChart chartECG;

    List<Entry> entries_ecg;
    LineDataSet dataSet_ecg;
    LineData lineData_ecg;

    float x_ecg;
    float ecg_y_max = 0;
    float ecg_y_min = 0;
    float ecg_y_diff;

    boolean ecg_graph_update;


    /* Heart Rate */
    TextView txtBtnOnHeartRate, txtBtnOffHeartRate, txtDisplay_HeartRate;

    /* Battery Level */
    TextView txtBtnOnBatteryLevel, txtBtnOffBatteryLevel, txtDisplay_BatteryLevel;

    /* HTS */
    TextView txtBtnOnHTS, txtBtnOffHTS, txtDisplay_HTS_Celsius, txtDisplay_HTS_Fahrenheit, txtDisplay_HTS_Env,txtDisplay_HTS_Core;

    private double temp1,temp2,temp3,tempCore,tempSkin,tempEnv,heartRate,valueTempCoreConditon;

    /* Barometer */
    TextView txtBtnOnBaro, txtBtnOffBaro, txtDisplay_Baro_Temperature, txtDisplay_Baro_Pressure,txtDisplay_Baro_HeightSea;


    /* IMU */

    TextView txtBtnOnIMU, txtBtnOffIMU,txtDisplay_Pitch,txtDisplay_Roll,txtDisplay_Yaw,txtDisplay_Accx,txtDisplay_Accy,txtDisplay_Accz;

    TextView txtDisplay_Gyrox,txtDisplay_Gyroy,txtDisplay_Gyroz;


    TextView txtDisplay_IMU_Value,txtDisplay_Temp_IMU;;

    TextView txtDisplay_tilt,txtDisplay_mlc,txtDisplay_steps;


    /* IMU Variables */
    private int wakeUp,stepDetector,mlc1,doubleTap,singleTap,tilt,sixD,freeFall;

    private int wakeUpX,wakeUpY,wakeUpZ,steps,mlcOut,doubleTapX,doubleTapY,doubleTapZ,doubleTapN,doubleTapP;

    private int singleTapX,singleTapY,singleTapZ,singleTapN,singleTapP;

    private int xh,xl,yh,yl,zh,zl;

    float[] accelerometerValues;
    float[] gyroscopeValues;

    long timestamp;

    private boolean txtIMURunn = true,txtChangeAble = false;;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

       entries_ecg = new ArrayList<>();

        //Remove notification bar
//        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //Remove action bar
        //getSupportActionBar().hide();
        setContentView(R.layout.activity_custom_operation);

        mContext = this;

        initActionBar("BLE Operations");

        initData();
        initView();
        setListener();

        writeCharacteristic("000000000000000000", Constants.servicePowerOn, Constants.characteristicPowerOn, "", "");

        ObserverManager.getInstance().addObserver(this);
    }


    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (!mIsSession) {
                if (mGattUpdateReceiver != null) {
                    unregisterReceiver(mGattUpdateReceiver);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        // intentFilter.addAction(BCMBleService.ACTION_GATT_CONNECTED);
        // intentFilter.addAction(BCMBleService.ACTION_GATT_DISCONNECTED);
        // intentFilter
        // .addAction(BCMBleService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BCMBleService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    public void myPlotPPG() {
        // Get whether to plot a session or current
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mIsSession = extras.getBoolean(IConstants.PLOT_SESSION_CODE, false);
            if (mIsSession) {
                mPlotSessionStart = extras.getLong(
                        IConstants.PLOT_SESSION_START_TIME_CODE, Constants.INVALID_DATE);
            }
        }
        if (mIsSession && (mPlotSessionStart == Constants.INVALID_DATE)) {
            General.showToast(mContext, "Plotting a session but got invalid "
                    + "values for the start time");
            return;
        }

        // Get the settings
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mPlotHr = prefs.getBoolean(IConstants.PREF_PLOT_HR, true);
        mPlotRr = prefs.getBoolean(IConstants.PREF_PLOT_RR, true);
        String stringVal = prefs.getString(IConstants.PREF_PLOT_INTERVAL, null);
        if (stringVal == null) {
            mPlotInterval = Constants.PLOT_MAXIMUM_AGE;
        } else {
            try {
                mPlotInterval = 60000 * Integer.parseInt(stringVal);
            } catch (Exception ex) {
                mPlotInterval = Constants.PLOT_MAXIMUM_AGE;
            }
        }

        if (mPlot != null) {
            createPlot();
        } else {
            General.showToast(mContext, "mPlot is null");
            returnResult(IConstants.RESULT_ERROR, "mPlot is null");
        }

        /*
        If mIsSession is true it uses mPlotSessionStart, set in onCreate.
        If mIsSession is false it uses mPlotStartTime, set here
        */
        if (!mIsSession) {
            Log.d(TAG, "onResume: Starting registerReceiver");
            registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
            mPlotStartTime = new Date().getTime() - mPlotInterval;
            // Make it keep the screen on
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            // Make it not keep the screen on
            getWindow().clearFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        // Create the datasets and fill them
        refresh();
    } // end myPlotPPG

    private final BroadcastReceiver mGattUpdateReceiver = new
            BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    final String action = intent.getAction();
                    if (BCMBleService.ACTION_DATA_AVAILABLE.equals(action)) {
//                        Log.d(TAG, "mGattUpdateReceiver:onReceive: " +
//                                action);
                        addValues(intent);
                    } else if (BCMBleService.ACTION_STATUS.equals(action)) {
//                        Log.d(TAG, "mGattUpdateReceiver:onReceive: " +
//                                action);
                        displayError(intent);
                    }
                }
            };

    public void addValues(Intent intent) {
//        Log.d(TAG, this.getClass().getSimpleName() + ": addValues: mPlotHr="
//                + mPlotHr + " mPlotRr=" + mPlotRr);
        String strValue;
        double value;
        long date = intent.getLongExtra(IConstants.EXTRA_DATE, Constants.INVALID_DATE);
        if (date == Constants.INVALID_DATE) {
            Log.d(TAG, this.getClass().getSimpleName() + ": addValues: " +
                    "INVALID_DATE");
            return;
        }
        //
        if (mPlotHr) {
            strValue = intent.getStringExtra(IConstants.EXTRA_HR);
            if (strValue != null && strValue.length() > 0) {
                try {
                    value = Double.parseDouble(strValue);
                } catch (NumberFormatException ex) {
                    value = Double.NaN;
                }
                if (value == IConstants.INVALID_INT) {
                    value = Double.NaN;
                }
                hrSeries.addLast(date, value);
            }
        }
        if (mPlotRr) {
            if (mLastRrUpdateTime == Constants.INVALID_DATE) {
                mLastRrUpdateTime = date;
                mLastRrTime = date;
            }
            strValue = intent.getStringExtra(IConstants.EXTRA_RR);
            if (strValue != null && strValue.length() > 0) {
                // boolean res = addRrValues(mRrSeries, date, strValue);
                // Don't check for errors here to avoid error storms
                addRrValues(date, strValue);
            }

            if (mLastRrUpdateTime == Constants.INVALID_DATE) {
                mLastRrUpdateTime = date;
                mLastRrTime = date;
            }
        }
        update();
    }

    public void update() {
        runOnUiThread(() -> mPlot.redraw());
    }

    private void displayError(Intent intent) {
        String msg = null;
        try {
            msg = intent.getStringExtra(IConstants.EXTRA_MSG);
            if (msg == null) {
                General.showToast(mContext, "Received null error message");
                return;
            }
            General.showToast(mContext, msg);
        } catch (Exception ex) {
            Log.d(TAG, this.getClass().getSimpleName() + ": displayError: " +
                    "Error displaying error", ex);
            General.showToast(mContext, msg);
            ex.printStackTrace();
        }
    }

    /**
     * Refreshes the mPlot by getting new data.
     */
    private void refresh() {
        // The logic for whether it is a session or not is in this method
        createSeries();
    }

    /**
     * Creates the data sets and series.
     */
    private void createSeries() {
        Log.d(TAG, "Creating series");
        if (!mPlotHr && !mPlotRr) {
            General.showToast(mContext, "Neither HR nor RR is selected to be plotted");
        }
        if (mPlotHr) {
            hrSeries = new SimpleXYSeries("HR");
            /*if (!mIsSession) {
                mHrSeries.setMaximumItemAge(mPlotInterval);
            }*/
        } else {
            hrSeries = null;
        }
        if (mPlotRr) {
            rrSeries = new SimpleXYSeries("RR");
            /*if (!mIsSession) {
                mRrSeries.setMaximumItemAge(mPlotInterval);
            }*/
        } else {
            rrSeries = null;
        }
        mLastRrTime = Constants.INVALID_DATE;
        mLastRrUpdateTime = Constants.INVALID_DATE;
        Cursor cursor = null;
        int nHrItems = 0, nRrItems = 0;
        int nErrors = 0;
        boolean res;
        try {
            if (mDbAdapter != null) {
                if (mIsSession) {
                    Log.i(TAG, "mIsSession is true");
                    cursor = mDbAdapter
                            .fetchAllHrRrDateDataForStartDate
                                    (mPlotSessionStart);
                } else {
                    Log.i(TAG, "mIsSession is false");
                    cursor = mDbAdapter
                            .fetchAllHrRrDateDataStartingAtDate
                                    (mPlotStartTime);
                }
                int indexDate = cursor.getColumnIndex(IConstants.COL_DATE);
                int indexHr = mPlotHr ? cursor.getColumnIndex(IConstants.COL_HR) : -1;
                int indexRr = mPlotRr ? cursor.getColumnIndex(IConstants.COL_RR) : -1;

                // Loop over items
                cursor.moveToFirst();
                long date;
                double hr;
                String rrString;
                while (!cursor.isAfterLast()) {
                    date = cursor.getLong(indexDate);
                    if (indexHr > -1) {
                        hr = cursor.getInt(indexHr);
                        if (hr == IConstants.INVALID_INT) {
                            hr = Double.NaN;
                        }
                        hrSeries.addLast(date, hr);
                        nHrItems++;
                    }
                    if (indexRr > -1) {
                        rrString = cursor.getString(indexRr);
                        if (nRrItems == 0) {
                            mLastRrUpdateTime = date;
                            mLastRrTime = date - IConstants.INITIAL_RR_START_TIME;
                        }
                        res = addRrValues(date, rrString);
                        nRrItems++;
                        if (!res) {
                            nErrors++;
                        }
                    }
                    cursor.moveToNext();
                }
            }
        } catch (Exception ex) {
            General.showToast(mContext, "Error creating datasets");
            ex.printStackTrace();
        } finally {
            try {
                if (cursor != null) cursor.close();
            } catch (Exception ex) {
                // Do nothing
            }
        }
        if (nErrors > 0) {
            General.showToast(mContext, nErrors + " error(s) creating RR series");
        }
        if (mPlotHr) {
            Log.d(TAG, "HR series created with " + nHrItems + " items");
            mPlot.addSeries(hrSeries, hrFormatter);
        }
        if (mPlotRr) {
            Log.d(TAG, "RR series created with " + nRrItems + " items with nErrors=" + nErrors);
            mPlot.addSeries(rrSeries, rrFormatter);
        }
    } // end createSeries

    /**
     * Parses the RR String and adds items to the series at the appropriate
     * times.
     *
     * @param updateTime The time of this update.
     * @param strValue   The RR String from the database.
     * @return If the operation was successful.
     */
    private boolean addRrValues(long updateTime, String strValue) {
        if (strValue.length() == 0) {
            // Do nothing
            return true;
        }
        if (strValue.equals(IConstants.INVALID_STRING)) {
            mLastRrUpdateTime = updateTime;
            mLastRrTime = updateTime - IConstants.INITIAL_RR_START_TIME;
            return true;
        }
        String[] tokens;
        tokens = strValue.trim().split("\\s+");
        int nRrValues = tokens.length;
        if (nRrValues == 0) {
            return false;
        }
        long[] times = new long[nRrValues];
        double[] values = new double[nRrValues];
        long lastRrTime = mLastRrTime;
        double val;
        for (int i = 0; i < nRrValues; i++) {
            try {
                val = Double.parseDouble(tokens[i]);
            } catch (NumberFormatException ex) {
                return false;
            }
            lastRrTime += val;
            times[i] = lastRrTime;
            values[i] = val / 1.024;
        }
        // Make first rr time be >= mLastRrUpdateTime
        long deltaTime;
        long firstTime = times[0];
        if (firstTime < mLastRrUpdateTime) {
            deltaTime = mLastRrUpdateTime - firstTime;
            for (int i = 0; i < nRrValues; i++) {
                times[i] += deltaTime;
            }
        }
        // Make all times be <= updateTime. Overrides previous if necessary.
        long lastTime = times[nRrValues - 1];
        if (times[nRrValues - 1] > updateTime) {
            deltaTime = lastTime - updateTime;
            for (int i = 0; i < nRrValues; i++) {
                times[i] -= deltaTime;
            }
        }
        double rr;
        for (int i = 0; i < nRrValues; i++) {
            rr = RR_SCALE * values[i];
            rrSeries.addLast(times[i], rr);
        }
        mLastRrUpdateTime = updateTime;
        mLastRrTime = times[nRrValues - 1];
        return true;
    } // end addRrValues

    private void createPlot() {
        Log.d(TAG, this.getClass().getSimpleName() + ": createPlot");
        hrFormatter = new LineAndPointFormatter(Color.RED, null, null, null);
        hrFormatter.setLegendIconEnabled(false);
        rrFormatter = new LineAndPointFormatter(Color.rgb(0, 153, 255), null, null, null);
        rrFormatter.setLegendIconEnabled(false);

        mPlot.setRangeBoundaries(0, BoundaryMode.FIXED, 100, BoundaryMode.AUTO);
        mPlot.setDomainBoundaries(0, 360000, BoundaryMode.AUTO);
        /* Range labels will increment by 10 */
        mPlot.setRangeStep(StepMode.INCREMENT_BY_VAL, 10);
        //mPlot.setDomainStep(StepMode.INCREMENT_BY_VAL, 60000); // 1 min
        mPlot.setDomainStep(StepMode.SUBDIVIDE, 5);
        /* Make left labels be an integer (no decimal places) */
        mPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(new DecimalFormat("#"));
        mPlot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(new Format() {
            private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.US);

            @Override
            public StringBuffer format(Object obj,
                                       @NonNull StringBuffer toAppendTo,
                                       @NonNull FieldPosition pos) {
                long time = Math.round(((Number) obj).doubleValue());
                return dateFormat.format(time, toAppendTo, pos);
            }

            @Override
            public Object parseObject(String source, @NonNull ParsePosition pos) {
                return null;
            }
        });

        /* This adds sub-grid lines in the default color 180,180,180 (#646464) */

        //mPlot.setLinesPerRangeLabel(2);
        /* No resource to set this, and this makes them disappear */
        //mPlot.getGraph().setRangeSubGridLinePaint(new Paint(Color.rgb(90, 90, 90)));

        // Pan and Zoom
        PanZoom.attach(mPlot, PanZoom.Pan.BOTH, PanZoom.Zoom.STRETCH_BOTH);
    } // end createPlot

    public void showGraphPPGPopup() {
        try {
            if (popupWindowGraphPPG != null) {
                popupWindowGraphPPG.dismiss();
            }
            LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(mContext.LAYOUT_INFLATER_SERVICE);
            final View customView = layoutInflater.inflate(R.layout.popup_graph_ppg, null);
            customView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            TextView txtClose = (TextView) customView.findViewById(R.id.txtClose);

            mPlot = (XYPlot) customView.findViewById(R.id.mPlot);

            myPlotPPG();

            //instantiate popup window
            popupWindowGraphPPG = new PopupWindow(customView, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            //display the popup window
            popupWindowGraphPPG.showAtLocation(layout_CustomOperationActivity, Gravity.CENTER, 0, 0);
            popupWindowGraphPPG.setOutsideTouchable(true);
            popupWindowGraphPPG.setFocusable(true);
            popupWindowGraphPPG.update();

            txtClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (popupWindowGraphPPG != null) {
                        popupWindowGraphPPG.dismiss();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public String getServiceCode() {
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

    @Override
    public void disConnected(BleDevice device) {
        if (device != null && bleDevice != null && device.getKey().equals(bleDevice.getKey())) {
            finish();
        }
    }

    @SuppressLint({"MissingPermission"})
    private void initData() {
        bleDevice = getIntent().getParcelableExtra(KEY_DATA);
        if (bleDevice == null) {
            finish();
        }
        mSessionStartTime = new Date().getTime();

        mDbAdapter = new BCMDbAdapter(this);
        mDbAdapter.open();
    }

    private void initView() {
        layout_CustomOperationActivity = (LinearLayout) findViewById(R.id.layout_CustomOperationActivity);
        /* PPG */
        txtBtnOnPPG = (TextView) findViewById(R.id.txtBtnOnPPG);
        txtBtnOffPPG = (TextView) findViewById(R.id.txtBtnOffPPG);
        txtBtnPpgGraph = (TextView) findViewById(R.id.txtBtnPpgGraph);
        txtDisplay_PPG_HR = (TextView) findViewById(R.id.txtDisplay_PPG_HR);
        txtDisplay_PPG_SPO2 = (TextView) findViewById(R.id.txtDisplay_PPG_SPO2);
        txtDisplay_PPG_Quality = (TextView) findViewById(R.id.txtDisplay_PPG_Quality);
        txtDisplay_PPG_RR = (TextView) findViewById(R.id.txtDisplay_PPG_RR);

        /* ECG */
        txtBtnOnECG = (TextView) findViewById(R.id.txtBtnOnECG_2);
        txtBtnOffECG = (TextView) findViewById(R.id.txtBtnOffECG);
        txtBtnEcgGraph = (TextView) findViewById(R.id.txtBtnEcgGraph);
        txtDisplay_ECG = (TextView) findViewById(R.id.txtDisplay_ECG);

        /* Heart Rate */
        txtBtnOnHeartRate = (TextView) findViewById(R.id.txtBtnOnHeartRate);
        txtBtnOffHeartRate = (TextView) findViewById(R.id.txtBtnOffHeartRate);
        txtDisplay_HeartRate = (TextView) findViewById(R.id.txtDisplay_HeartRate);

        /* Battery Level */
        txtBtnOnBatteryLevel = (TextView) findViewById(R.id.txtBtnOnBatteryLevel);
        txtBtnOffBatteryLevel = (TextView) findViewById(R.id.txtBtnOffBatteryLevel);
        txtDisplay_BatteryLevel = (TextView) findViewById(R.id.txtDisplay_BatteryLevel);

        /* HTS */
        txtBtnOnHTS = (TextView) findViewById(R.id.txtBtnOnHTS);
        txtBtnOffHTS = (TextView) findViewById(R.id.txtBtnOffHTS);
        txtDisplay_HTS_Celsius = (TextView) findViewById(R.id.txtDisplay_HTS_Celsius);
        txtDisplay_HTS_Env = (TextView) findViewById(R.id.txtDisplay_HTS_Env_Celsius);
        txtDisplay_HTS_Core = (TextView) findViewById(R.id.txtDisplay_HTS_Core_Celsius);
//        txtDisplay_HTS_Fahrenheit = (TextView) findViewById(R.id.txtDisplay_HTS_Fahrenheit);

        /* Barometer */
        txtBtnOnBaro = (TextView) findViewById(R.id.txtBtnOnBaro);
        txtBtnOffBaro = (TextView) findViewById(R.id.txtBtnOffBaro);
        txtDisplay_Baro_Pressure = (TextView) findViewById(R.id.txtDisplay_Baro_Pressure);
        txtDisplay_Baro_Temperature = (TextView) findViewById(R.id.txtDisplay_Baro_Temperature);
        txtDisplay_Baro_HeightSea = (TextView) findViewById(R.id.txtDisplay_Baro_HeightSea);

        /* IMU */
        txtBtnOnIMU = (TextView) findViewById(R.id.txtBtnOnIMU);
        txtBtnOffIMU = (TextView) findViewById(R.id.txtBtnOffIMU);
        txtDisplay_Pitch = (TextView) findViewById(R.id.txtDisplay_Pitch);
        txtDisplay_Roll = (TextView) findViewById(R.id.txtDisplay_Roll);
        txtDisplay_Yaw = (TextView) findViewById(R.id.txtDisplay_Yaw);
        txtDisplay_Accx = (TextView) findViewById(R.id.txtDisplay_Acce_x);
        txtDisplay_Accy = (TextView) findViewById(R.id.txtDisplay_Acce_y);
        txtDisplay_Accz = (TextView) findViewById(R.id.txtDisplay_Acce_z);
        txtDisplay_Gyrox = (TextView) findViewById(R.id.txtDisplay_Gyro_x);
        txtDisplay_Gyroy = (TextView) findViewById(R.id.txtDisplay_Gyro_y);
        txtDisplay_Gyroz = (TextView) findViewById(R.id.txtDisplay_Gyro_z);


        txtDisplay_IMU_Value = (TextView) findViewById(R.id.txtDisplay_IMU_Value);
        txtDisplay_tilt = (TextView) findViewById(R.id.txtDisplay_tilt_degree);
        txtDisplay_mlc = (TextView) findViewById(R.id.txtDisplay_MLC);
        txtDisplay_steps = (TextView) findViewById(R.id.txtDisplay_Steps);
        txtDisplay_Temp_IMU = (TextView) findViewById(R.id.txtDisplay_Temp_IMU);


    } // end initView()

    private void setListener() {
        txtBtnOnPPG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*offECG();
                offPPG_Raw();
                offHeartRate();*/
                onPPG();
            }
        });
        txtBtnOffPPG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                offPPG();
            }
        });
        txtBtnPpgGraph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPPGGraph();
            }
        });

        txtBtnOnECG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*offPPG();
                offPPG_Raw();
                offHeartRate();*/
                onECG();
            }
        });
        txtBtnOffECG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                offECG();
            }
        });
        txtBtnEcgGraph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startECGGraph();
            }
        });

        txtBtnOnHeartRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*offECG();
                offPPG();
                offPPG_Raw();*/
                onHeartRate();
            }
        });
        txtBtnOffHeartRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                offHeartRate();
            }
        });

        txtBtnOnBatteryLevel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBatteryLevel();
            }
        });
        txtBtnOffBatteryLevel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                offBatteryLevel();
            }
        });

        txtBtnOnHTS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onHTS();
            }
        });
        txtBtnOffHTS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                offHTS();
            }
        });

        txtBtnOnBaro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBarometer();
            }
        });
        txtBtnOffBaro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                offBarometer();
            }
        });
        txtBtnOnIMU.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onIMU();
            }
        });
        txtBtnOffIMU.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                offIMU();
            }
        });
    } // end setListener()

    public BleDevice getBleDevice() {
        return bleDevice;
    }

    public void notifyCharacteristic(String sUUID, String cUUID, String serviceName) {
        try {
//            Log.i("READ_Prakhar","Notify called");
            Log.i(TAG, "notifyCharacteristic() is called");
            Log.i("READ_Prakhar_Notify","Notify_characteristics_called");

//            BluetoothGattCharacteristic characteristic = cUUID.get
            // **** Checking property notify set or not

            //        BluetoothGattCharacteristic characteristic;
//
//// ... code to retrieve the characteristic
//
//        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
//            // The PROPERTY_NOTIFY property is set for this characteristic
//              Log.i("READ_Prakhar","Notify characteristic property is set!!");
//        } else {
//            Log.i("READ_Prakhar","Notify characteristic property is not set!!");
//            // The PROPERTY_NOTIFY property is not set for this characteristic
//        }

            // *******

            BleDevice bleDevice = getBleDevice();

            BleManager.getInstance().notify(
                    bleDevice,
                    sUUID,
                    cUUID,
                    new BleNotifyCallback() {

                        @Override
                        public void onNotifySuccess() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.i("READ_Prakhar_Notify","Notify_success_called");
                                    Log.i(TAG, "onNotifySuccess() is called");
                                }
                            });
                        }

                        @Override
                        public void onNotifyFailure(final BleException exception) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.i("READ_Prakhar_Notify","Notify_failure_called");
                                    General.showToast(mContext, exception.toString());
                                }
                            });
                        }

                        @Override
                        public void onCharacteristicChanged(byte[] data) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
//                                  Log.i("Read_Prakhar_Notify", "onCharacteristicChanged() is called");
                                    Log.i("READ_Prakhar_Notify","Notify_characteristics_changed_called");

                                    String content = HexUtil.formatHexString(data, true);
                                    //String converted = HexString.unHex(content.replaceAll("\\s+", ""));
                                    //content = content + " ~ " + converted;

//                                    byte[] b = new byte[4];
//                                    b[0] = (byte) data[0];
//                                    b[1] = (byte) data[1];
//                                    b[2] = (byte) data[2];
//                                    b[3] = (byte) data[3];
//                                    float f = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getFloat();

                                     Log.i("READ_Prakhar Hello", content);

//                                    int[] val = new int[20];
//                                    for(int i=0;i<20;i++){
//                                        val[i] = Integer.parseInt(String.valueOf(data[i]),16);
//                                    }

                                    if (serviceName.equals("ECG")&&ecg_graph){

                                            int size = data.length;
                                            int k = size / 4;
                                            int ecgValue;
                                            int temp = 0;
                                            float ecg_graph;
//                                            Byte[] bt  = new Byte[4];
                                            for (int i = 0; i < k; i++) {
//                                                bt  = new Byte[4];
//                                                bt[0] = data[0+temp];
//                                                bt[1] = data[1+temp];
//                                                bt[2] = data[2+temp];
//                                                bt[3] = data[3+temp];
//                                                int btr = ByteBuffer.wrap(bt).getInt();
//                                                byte[] bytes = new byte[4];
//                                                bytes[0] = data[0+temp];
//                                                bytes[1] = data[1+temp];
//                                                bytes[2] = data[2+temp];
//                                                bytes[3] = data[3+temp];
//                                                float f = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
//                                                f = f * 0.0000244140625f;
//                                                entries_ecg.add(new Entry(x_ecg, f));
//                                                x_ecg = x_ecg + 1f;
//

                                                ecgValue = (data[3 + temp] & 0xff) << 24 | (data[2 + temp] & 0xff) << 16 | (data[1 + temp] & 0xff) << 8 | (data[0 + temp] & 0xff);
                                                ecg_graph = (float) ecgValue;
                                                ecg_graph = ecg_graph / 1000f;
                                                temp = temp + 4;
                                                ecg_graph = ecg_graph * 0.0000244140625f;
                                                ecg_y_max = Math.max(ecg_y_max,ecg_graph);
                                                ecg_y_min = Math.min(ecg_y_min,ecg_graph);
                                                if(ecg_graph_update){
                                                    entries_ecg.set((int)x_ecg,new Entry(x_ecg, ecg_graph));
                                                }else{
                                                    entries_ecg.add(new Entry(x_ecg, ecg_graph));
                                                }
                                                Log.i("READ_PrakharECG_", String.valueOf(ecg_graph));
//                                                entries_ecg.add(new Entry(x_ecg, ecg_graph));
                                                x_ecg = x_ecg + 1f;
                                                if(x_ecg==1251){
                                                    x_ecg = 0;
                                                    ecg_graph_update = true;
                                                  //  entries_ecg.set(0,new Entry(x_ecg, ecg_graph));
                                                }
                                                Log.i("READ_PrakharECG__", String.valueOf(x_ecg));
                                            }


//                       ************ Graph Implementation *****************

//                                    byte[] b = new byte[4];
//                                    b[0] = (byte) data[0];
//                                    b[1] = (byte) data[1];
//                                    b[2] = (byte) data[2];
//                                    b[3] = (byte) data[3];
//
//
//
//                                    float f = ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN).getFloat();
//                                    int ecgValue = (data[3] & 0xff) << 24 | (data[2] & 0xff) << 16 | (data[1] & 0xff) << 8 | (data[0] & 0xff);
//                                    float ecg_graph = (float) ecgValue;
//                                    ecg_graph = ecg_graph /  1000f;
//
//                                    Log.i("READ_Prakhar_ECG_Check",String.valueOf(ecgValue));
//                                    Log.i("READ_Prakhar_ECG_Float",String.valueOf(ecg_graph));
//
//
//                                    entries_ecg.add(new Entry(x_ecg,ecg_graph));

//                                   ***************************
//

                                        ecg_y_diff = ecg_y_max - ecg_y_min;
                                        ecg_y_diff = ecg_y_diff / 0.6f;
                                        XAxis xAxisECG = chartECG.getXAxis();
                                        YAxis yAxis = chartECG.getAxisLeft();
                                        yAxis.setAxisMaximum(ecg_y_max+ecg_y_diff*0.2f); // set the maximum y-axis value
                                        yAxis.setAxisMinimum(ecg_y_min-ecg_y_diff*0.2f);
                                        YAxis yAxisRight = chartECG.getAxisRight();
                                        yAxisRight.setAxisMaximum(ecg_y_max+ecg_y_diff*0.2f); // set the maximum y-axis value
                                        yAxisRight.setAxisMinimum(ecg_y_min-ecg_y_diff*0.2f);
                                        xAxisECG.setAxisMinimum(0); // set the minimum X-axis value
                                        xAxisECG.setAxisMaximum(1250);


                                        final float xGridLinesStep = 250f; // set the constant X-axis grid lines step
                                        xAxisECG.setGranularity(xGridLinesStep); // set the X-axis grid lines step
                                        xAxisECG.setDrawGridLines(true);

                                        LineData data_ecg = chartECG.getData();

                                            if (data_ecg == null) {
                                                data_ecg = new LineData();
                                                chartECG.setData(data_ecg);
                                            }

                                            ILineDataSet set = data_ecg.getDataSetByIndex(0);
//                                            set.setDrawFilled(true);
                                            if (set == null) {
                                                set = createSet();
                                                data_ecg.addDataSet(set);
                                            }

//                                        float xValueToRemove = 10f; // the X-axis value for which you want to remove the old entries
//                                        int entryIndexToRemove = set.getEntryIndex(xValueToRemove);
//                                        if (entryIndexToRemove >= 0) {
//                                            set.removeEntry(set.getEntryForIndex(entryIndexToRemove));
//                                        }



                                            dataSet_ecg.notifyDataSetChanged();
                                            lineData_ecg.notifyDataChanged();
                                            chartECG.notifyDataSetChanged();
                                            chartECG.setVisibleXRangeMinimum(0);
                                            chartECG.setVisibleXRangeMaximum(1250);
                                            chartECG.moveViewTo(x_ecg - 7, 50f, YAxis.AxisDependency.LEFT);

//                                    ***********************
                                            // Graph implementation finished


                                    }
                                    updateUI(serviceName, data);
                                }
                            });
                        }
                    });
        } catch (Exception e) {
            Log.i("READ_Prakhar_Notify",e.toString());
            e.printStackTrace();
        }
    } // end notifyCharacteristic()

    public void notifyCharacteristicStop(BleDevice bleDevice, String sUUID, String cUUID) {
        try {
            BleManager.getInstance().stopNotify(
                    bleDevice,
                    sUUID,
                    cUUID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // end notifyCharacteristicStop();

    public void readCharacteristic(String sUUID, String cUUID) {

      BleDevice bleDevice = getBleDevice();

       x_ecg = x_ecg + 1f;

        BleManager.getInstance().read(
                bleDevice,
                sUUID,
                cUUID,
                new BleReadCallback() {

                    @Override
                    public void onReadSuccess(final byte[] data) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String content = HexUtil.formatHexString(data, true);
                                //String converted = HexString.unHex(content.replaceAll("\\s+", ""));
                                //content = content + " ~ " + converted;

                                Log.i("READ_Prakhar",content);
//                                int vv = Byte.toUnsignedInt(data[0]);
//                                Log.i("READ_Prakhar", String.valueOf(vv));

//                                byte[] b = new byte[4];
//                                b[0] = (byte) data[0];
//                                b[1] = (byte) data[1];
//                                b[2] = (byte) data[2];
//                                b[3] = (byte) data[3];
//                                float f = ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN).getFloat();
//                                int ecgValue = (data[3] & 0xff) << 24 | (data[2] & 0xff) << 16 | (data[1] & 0xff) << 8 | (data[0] & 0xff);
//                                Log.i("READ_Prakhar_Patch",String.valueOf(f));
//                                Log.i("READ_Prakhar_Int",String.valueOf(ecgValue));
////                                int result = 0;
////                                for (int i = 0; i < 4; i++) {
////                                    result = (result << 8) + (data[i] & 0xff);
////                                }
//                                float ecg_graph = (float) ecgValue;
//                                ecg_graph = ecg_graph /  1000f;
//
////                                Log.i("READ_Prakhar_ECG_VAL", String.valueOf(f));
////                                Log.i("READ_Prakhar_ECG_INT",String.valueOf(result));
//                                Log.i("READ_Prakhar_ECG_Check",String.valueOf(ecgValue));
//                                Log.i("READ_Prakhar_ECG_Float",String.valueOf(ecg_graph));
//
//

//                              // implementing graph
//
//                                entries_ecg.add(new Entry(x_ecg,ecg_graph));
//                                LineData data_ecg = chartECG.getData();
//
//                                if (data_ecg == null) {
//                                    data_ecg = new LineData();
//                                    chartECG.setData(data_ecg);
//                                }
//
//                                ILineDataSet set = data_ecg.getDataSetByIndex(0);
//                                // set.addEntry(...); // can be called as well
//
//                                if (set == null) {
//                                    set = createSet();
//                                    data_ecg.addDataSet(set);
//                                }
//
//
//
//                                dataSet_ecg.notifyDataSetChanged();
//                                lineData_ecg.notifyDataChanged();
//                                chartECG.notifyDataSetChanged();
//                                chartECG.setVisibleXRangeMinimum(0);
//                                chartECG.setVisibleXRangeMaximum(500);
////                                chartECG.moveViewTo(dataSet_ecg.getEntryCount()-7 , 50f, YAxis.AxisDependency.LEFT);
//                                chartECG.moveViewTo(x_ecg-7 , 50f, YAxis.AxisDependency.LEFT);


//                                ******************
//

                                if (cUUID.equals(Constants.characteristicBaroPressure)) {
                                    readCharacteristic(Constants.serviceBarometer, Constants.characteristicBaroTemperature);
                                    updateUI("BarometerPressure", data);
                                }
                                if (cUUID.equals(Constants.characteristicBaroTemperature)) {
                                    updateUI("BarometerTemperature", data);
                                }
                                if (cUUID.equals(Constants.characteristicHeartRate)) {
                                    updateUI("HeartRate", data);
                                }
                            }
                        });
                    }

                    @Override
                    public void onReadFailure(final BleException exception) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                             Log.i("READ_Prakhar", exception.toString());
//                                General.showToast(mContext, exception.toString());
                            }
                        });
                    }

                });
    } // end readCharacteristic()


    public void writeDescriptor(String hex, String descriptorUUID) {
        BleDevice bleDevice = getBleDevice();


        BleManager.getInstance().write(
                bleDevice,
                descriptorUUID,
                descriptorUUID,
                HexUtil.hexStringToBytes(hex),
                new BleWriteCallback() {

                    @Override
                    public void onWriteSuccess(final int current, final int total, final byte[] justWrite) {
                        Log.i(TAG, "write success, current: " + current
                                + " total: " + total
                                + " justWrite: " + HexUtil.formatHexString(justWrite, true));

                    }

                    @Override
                    public void onWriteFailure(final BleException exception) {
                        General.showToast(mContext, exception.toString());
                    }
                });

    } // end writeDescriptor()

    public void writeCharacteristic(String hex, String serviceUUID, String characteristicUUID, String successMessage, String serviceName) {
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
                                    notifyCharacteristic(Constants.serviceHTS, Constants.characteristicHTS, serviceName);
                                    break;
                                case "IMU":
                                    notifyCharacteristic(Constants.serviceIMU, Constants.characteristicIMU,serviceName);
                                    break;
                                case "HeartRate":
                                    notifyCharacteristic(Constants.serviceHeartRate, Constants.characteristicHeartRate, serviceName);
                                    break;
                                case "BatteryLevel":
                                    notifyCharacteristic(Constants.serviceBatteryLevel, Constants.characteristicBatteryLevel, serviceName);
                                    break;
                                case "ECG":
                                    notifyCharacteristic(Constants.serviceECG, Constants.characteristicECG, serviceName);
                                    break;
                                case "PPG":
                                    notifyCharacteristic(Constants.servicePPG, Constants.characteristicPPG, serviceName);
                                    break;
                                case "Barometer":
                                    try {
                                        notifyCharacteristic(Constants.serviceBarometer, Constants.characteristicBaroPressure, "BarometerPressure");
                                        Thread.sleep(1000); // 1000 milliseconds = 1 seconds
                                        notifyCharacteristic(Constants.serviceBarometer, Constants.characteristicBaroTemperature, "BarometerTemperature");
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
                        General.showToast(mContext, exception.toString());
                    }
                });
    }
    // end writeDeviceSensor()

    public void updateUI(String serviceName, byte[] data) {
        switch (serviceName) {
            case "HTS":
                updateUI_HTS(data);
                break;
            case "IMU":
                updateUI_IMU(data);
            case "HeartRate":
                updateUI_HeartRate(data);
                break;
            case "BatteryLevel":
                updateUI_BatteryLevel(data);
                break;
            case "ECG":
                updateUI_ECG(data);
                break;
            case "PPG":
                updateUI_PPG(data);
                break;
            case "BarometerPressure":
                updateUI_Barometer_Pressure(data);
                break;
            case "BarometerTemperature":
                updateUI_Barometer_Temperature(data);
                break;
        }
    }

    public void updateUI_HTS(byte[] data) {
        try {
            String content = HexUtil.formatHexString(data, true);
            String[] contentArray = content.split(" ");

            int a1 = Integer.parseInt(contentArray[2], 16);
            int a2 = Integer.parseInt(contentArray[1], 16);

            // Environment temp calculation..started....

            int a3 = Integer.parseInt(contentArray[8],16);
            int a4 = Integer.parseInt(contentArray[7],16);

            int dd2 = (a3 <<8 | a4) + 45;

            double tempCelsiusEnv = (double) dd2 / (double) 100;
//            Log.i("READ_Prakhar_Temp_",String.valueOf(tempCelsiusEnv));

            // Environment temp calculation..ended.....


            int dd = (a1 << 8 | a2) + 45;

            double tempCelsius = (double) dd / (double) 100;
            double tempFahrenheit = ((tempCelsius*9)/5)+32;

            Log.i("HTS Data", content);

            txtDisplay_HTS_Celsius.setText(String.format("%.1f", tempFahrenheit));
            tempSkin = tempCelsius;

            heartRate = 75; // Need to change when sensor give value;
//            tempEnv = 27;  // Need to change when sensor give value;
            tempEnv = tempCelsiusEnv;
            temp1 = 0.0044*tempSkin*tempSkin+0.1736*tempSkin+23.523;
            temp2 = 0.01*heartRate+0.0837*temp1+33.1735;
            temp3 = -0.009*tempEnv*tempEnv+0.7368*tempEnv+21.076;
            if(temp3>temp1){
                valueTempCoreConditon = temp2-(temp3-temp1)*0.1;
            }else{
                valueTempCoreConditon = temp2 + (temp1- temp3)*1.1;
            }
            if(valueTempCoreConditon>39.5){
                Toast.makeText(mContext, "Most Critical Situation", Toast.LENGTH_SHORT).show();
                tempCore = Double.MAX_VALUE;
            }else{
                if(temp3>temp1){
                    tempCore = temp2 - (temp3-temp1)*0.1;
                }else{
                    tempCore = temp2 + (temp1 - temp3)*1.1;
                }
            }
            double tempFahrenheitEnV = ((tempEnv*9)/5)+32;
            txtDisplay_HTS_Env.setText(String.format("%.1f",tempFahrenheitEnV));
            double tempFahrenheitCore = ((tempCore*9)/5)+32;
            txtDisplay_HTS_Core.setText(String.format("%.1f", tempFahrenheitCore));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateUI_IMU(byte[] data) {
        try{
            String content = HexUtil.formatHexString(data, true);
            String[] arr = content.split(" ");
            String valueIMU = "";
            String tiltIMU = "";
            String mlcIMU = "";
            String stepsIMU = "";
            int activityIMU = Integer.parseInt(String.valueOf(arr[0]), 16);
            int activityiMU = Integer.parseInt(String.valueOf(data[0]), 16);
            Log.i("READ_PrakharCheck_1",String.valueOf(activityIMU));
            Log.i("READ_PrakharCheck_2",String.valueOf(activityiMU));
//            if(String.valueOf(data[0].equals("A"))){
//
//            }
            final Handler handler = new Handler();
            String finalValueIMU = valueIMU;
            Runnable runn = new Runnable() {
                @Override
                public void run() {
                    txtChangeAble = true;
                    txtDisplay_IMU_Value.setText("");
                }
            };
            if(activityIMU==1){
                Log.i("READ_Prakhar_IMU","Wake up");
                valueIMU = "WakeUp ";
                wakeUpX = Integer.parseInt(String.valueOf(arr[1]), 16);
                wakeUpY = Integer.parseInt(String.valueOf(arr[2]), 16);
                wakeUpZ = Integer.parseInt(String.valueOf(arr[3]), 16);
                if(wakeUpX==1){
                    valueIMU = valueIMU +"X ";
                    Log.i("READ_Prakhar_IMU","Wake up X");
                }
                if(wakeUpY==1){
                    valueIMU = valueIMU +"Y ";
                    Log.i("READ_Prakhar_IMU","Wake up Y");
                }
                if(wakeUpZ==1){
                    valueIMU = valueIMU +"Z ";
                    Log.i("READ_Prakhar_IMU","Wake up Z");
                }
            }else if(activityIMU==4){
                valueIMU = "Double Tap ";
                Log.i("READ_Prakhar_IMU","Double Tap");
                doubleTapX = Integer.parseInt(String.valueOf(arr[1]), 16);
                doubleTapY = Integer.parseInt(String.valueOf(arr[2]), 16);
                doubleTapZ = Integer.parseInt(String.valueOf(arr[3]), 16);
                doubleTapN = Integer.parseInt(String.valueOf(arr[4]), 16);
                doubleTapP = Integer.parseInt(String.valueOf(arr[5]), 16);
                if(doubleTapX==1){
                    valueIMU = valueIMU + "X ";
                    Log.i("READ_Prakhar_IMU","Double tap x");
                }
                if(doubleTapY==1){
                    valueIMU = valueIMU + " Y ";
                    Log.i("READ_Prakhar_IMU","Double tap y");
                }
                if(doubleTapZ==1){
                    valueIMU = valueIMU + " Z ";
                    Log.i("READ_Prakhar_IMU","Double tap z");
                }
                if(doubleTapN==1){
                    valueIMU = valueIMU + " -ve ";
                    Log.i("READ_Prakhar_IMU","Double tap negative");
                }
                if(doubleTapP==1){
                    valueIMU = valueIMU + " +ve ";
                    Log.i("READ_Prakhar_IMU","Double tap postive");
                }
                if(txtChangeAble){
                    txtChangeAble = false;
                    txtDisplay_IMU_Value.setText(valueIMU);
                    handler.postDelayed(runn,3000);
                }
            }else if(activityIMU==5){
                valueIMU = "Single Tap ";
                Log.i("READ_Prakhar_IMU","Single Tap");
                singleTapX = Integer.parseInt(String.valueOf(arr[1]), 16);
                singleTapY = Integer.parseInt(String.valueOf(arr[2]), 16);
                singleTapZ = Integer.parseInt(String.valueOf(arr[3]), 16);
                singleTapN = Integer.parseInt(String.valueOf(arr[4]), 16);
                singleTapP = Integer.parseInt(String.valueOf(arr[5]), 16);
                if(singleTapX==1){
                    valueIMU = valueIMU + " X ";
                    Log.i("READ_Prakhar_IMU","Double tap x");
                }
                if(singleTapY==1){
                    valueIMU = valueIMU + " Y ";
                    Log.i("READ_Prakhar_IMU","Double tap y");
                }
                if(singleTapZ==1){
                    valueIMU = valueIMU + " Z ";
                    Log.i("READ_Prakhar_IMU","Double tap z");
                }
                if(singleTapN==1){
                    valueIMU = valueIMU + " -ve ";
                    Log.i("READ_Prakhar_IMU","Double tap negative");
                }
                if(singleTapP==1){
                    valueIMU = valueIMU + " +ve ";
                    Log.i("READ_Prakhar_IMU","Double tap postive");
                }
                if(txtChangeAble){
                    txtChangeAble = false;
                    txtDisplay_IMU_Value.setText(valueIMU);
                    handler.postDelayed(runn,3000);
                }
            }else if(activityIMU==7){
                valueIMU = "six_d ";
                Log.i("READ_Prakhar_IMU","six_d");
                xh = Integer.parseInt(String.valueOf(arr[1]), 16);
                xl = Integer.parseInt(String.valueOf(arr[2]), 16);
                yh = Integer.parseInt(String.valueOf(arr[3]), 16);
                yl = Integer.parseInt(String.valueOf(arr[4]), 16);
                zh = Integer.parseInt(String.valueOf(arr[5]), 16);
                zl = Integer.parseInt(String.valueOf(arr[6]), 16);
                if(xh==1){
                    valueIMU = valueIMU + "xh ";
                    Log.i("READ_Prakhar_IMU","xh....");
                }
                if(xl==1){
                    valueIMU = valueIMU + "xl ";
                    Log.i("READ_Prakhar_IMU","xl....");
                }
                if(yh==1){
                    valueIMU = valueIMU + "yh ";
                    Log.i("READ_Prakhar_IMU","yh....");
                }
                if(yl==1){
                    valueIMU = valueIMU + "yl ";
                    Log.i("READ_Prakhar_IMU","yl....");
                }
                if(zh==1){
                    valueIMU = valueIMU + "zh ";
                    Log.i("READ_Prakhar_IMU","zh....");
                }
                if(zl==1) {
                    valueIMU = valueIMU + "zl ";
                    Log.i("READ_Prakhar_IMU", "zl....");
                }
            }else if(activityIMU==8){
                valueIMU = "free fall";
                Log.i("READ_Prakhar_IMU", "free fall");
                if(txtChangeAble){
                    txtChangeAble = false;
                    txtDisplay_IMU_Value.setText(valueIMU);
                    handler.postDelayed(runn,3000);
                }
            }
            if(activityIMU==6){
                Log.i("READ_Prakhar_IMU","tilt");
            }
            if(activityIMU==2){
                Log.i("READ_Prakhar_IMU","Step Detector");
                steps = Integer.parseInt(String.valueOf(arr[1]), 16);
                stepsIMU = String.valueOf(steps);
                txtDisplay_steps.setText(stepsIMU);
                Log.i("READ_Prakhar_IMU",String.valueOf(steps));
            }
            if(activityIMU==3){
                Log.i("READ_Prakhar_IMU","MLC1");
                mlcOut = Integer.parseInt(String.valueOf(arr[1]), 16);
                mlcIMU = String.valueOf(mlcOut);
                txtDisplay_mlc.setText(mlcIMU);
                Log.i("READ_Prakhar_IMU",String.valueOf(mlcOut));
            }

            // ****** Accelereto Gyro Magneto ******** //
            if(activityIMU==9){
                // accelero
                Log.i("READ_Prakhar_IMU_A","acce "+content.toString());
                byte[] bytesX = new byte[] { data[1], data[2], data[3], data[4] };
                float acc_x = ByteBuffer.wrap(bytesX).order(ByteOrder.LITTLE_ENDIAN).getFloat();

                byte[] bytesY = new byte[] { data[5], data[6], data[7], data[8] };
                float acc_y = ByteBuffer.wrap(bytesY).order(ByteOrder.LITTLE_ENDIAN).getFloat();

                byte[] bytesZ = new byte[] { data[9], data[10], data[11], data[12] };
                float acc_z = ByteBuffer.wrap(bytesZ).order(ByteOrder.LITTLE_ENDIAN).getFloat();

                accelerometerValues[0] = acc_x;
                accelerometerValues[1] = acc_y;
                accelerometerValues[2] = acc_z;
                txtDisplay_Accx.setText(String.format("%.2f", acc_x));
                txtDisplay_Accy.setText(String.format("%.2f", acc_y));
                txtDisplay_Accz.setText(String.format("%.2f", acc_z));

            }
            if(activityIMU==10){
                // Gyro
                Log.i("READ_Prakhar_IMU_G",String.valueOf("Gyro "+content.toString()));
                byte[] bytesX = new byte[] { data[1], data[2], data[3], data[4] };
                float gyro_x = ByteBuffer.wrap(bytesX).order(ByteOrder.LITTLE_ENDIAN).getFloat();

                byte[] bytesY = new byte[] { data[5], data[6], data[7], data[8] };
                float gyro_y = ByteBuffer.wrap(bytesY).order(ByteOrder.LITTLE_ENDIAN).getFloat();

                byte[] bytesZ = new byte[] { data[9], data[10], data[11], data[12] };
                float gyro_z = ByteBuffer.wrap(bytesZ).order(ByteOrder.LITTLE_ENDIAN).getFloat();

                gyroscopeValues[0] = gyro_x;
                gyroscopeValues[1] = gyro_y;
                gyroscopeValues[2] = gyro_z;
                txtDisplay_Gyrox.setText(String.format("%.2f", gyro_x));
                txtDisplay_Gyroy.setText(String.format("%.2f", gyro_y));
                txtDisplay_Gyroz.setText(String.format("%.2f", gyro_z));

            }
            if (accelerometerValues != null && gyroscopeValues != null) {
                long eventTimeStamp = System.currentTimeMillis();
                float deltaT = (eventTimeStamp - timestamp) * 1e-9f;
                timestamp = eventTimeStamp;

                float[] rotationMatrix = new float[9];
                SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerValues, gyroscopeValues);

                float[] orientationAngles = new float[3];
                SensorManager.getOrientation(rotationMatrix, orientationAngles);

                float roll = (float) Math.toDegrees(orientationAngles[2]);
                float pitch = (float) Math.toDegrees(orientationAngles[1]);
                float yaw = (float) Math.toDegrees(orientationAngles[0]);
                txtDisplay_Pitch.setText(String.valueOf(pitch));
                txtDisplay_Roll.setText(String.valueOf(roll));
                txtDisplay_Yaw.setText(String.valueOf(yaw));
                // use roll, pitch, and yaw values
            }
            if(activityIMU==11){
                // temp
                Log.i("READ_Prakhar_IMU_T",String.valueOf("Temp "+content.toString()));
                byte[] bytesT = new byte[] { data[1], data[2], data[3], data[4] };
                float temp_imu = ByteBuffer.wrap(bytesT).order(ByteOrder.LITTLE_ENDIAN).getFloat();
//                txtDisplay_Temp_IMU.setText(String.valueOf(temp_imu));
                txtDisplay_Temp_IMU.setText(String.format("%.1f",temp_imu));

            }
        }catch (Exception e){
            Log.i("READ_Prakhar_Error",e.toString());
        }
   }
    public void updateUI_HeartRate(byte[] data) {
        try {
            String content = HexUtil.formatHexString(data, true);
            Log.i("HeartRate Data", content);
            String[] contentArray = content.split(" ");

            int a0 = Integer.parseInt(contentArray[0], 16);
            int a1 = Integer.parseInt(contentArray[1], 16);
            int a2 = Integer.parseInt(contentArray[2], 16);

            String reverse = contentArray[2] + contentArray[1];
            int r = Integer.parseInt(reverse, 16);
            //Log.i(TAG+"-r", r+"");
            //Log.i("HeartRate", content);
            //double heartRate = (double) r / (double) 1;
            txtDisplay_HeartRate.setText(String.format("%d", r) + " bpm");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateUI_BatteryLevel(byte[] data) {
        try {
            String content = HexUtil.formatHexString(data, false);
            txtDisplay_BatteryLevel.setText(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateUI_ECG(byte[] data) {
        try {
            String content = HexUtil.formatHexString(data, true);
            Log.i("ECG Data", content);
            txtDisplay_ECG.setText(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateUI_PPG(byte[] data) {
        try {
            String content = HexUtil.formatHexString(data, true);
            String[] contentArray = content.split(" ");

            int a0 = Integer.parseInt(contentArray[0], 16);
            int a1 = Integer.parseInt(contentArray[1], 16);
            int a2 = Integer.parseInt(contentArray[2], 16);
            int a3 = Integer.parseInt(contentArray[3], 16);
            int a4 = Integer.parseInt(contentArray[4], 16);

            int ppgValue = a1 << 8 | a2;
            int hrValue = a3 << 8 | a4;

            Log.i("PPG Data", content);
            Log.i("PPG Time", new Date().getTime() + "");

            txtDisplay_PPG_SPO2.setText(ppgValue + "");
            txtDisplay_PPG_HR.setText(hrValue + "");
            txtDisplay_PPG_Quality.setText("0");
            txtDisplay_PPG_RR.setText("0");


            // graph process
            Date now = new Date();
            long date = now.getTime();
            UUID cc = UUID.fromString(Constants.characteristicPPG);
            BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(cc, BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ);

            mLastHr = hrValue;
            mLastRr = "";
            mLastHrDate = date;

            /*Log.i(TAG + " - mLastHr", mLastHr+"");
            Log.i(TAG + " - mLastRr", mLastRr+"");
            Log.i(TAG + " - mLastHrDate", mLastHrDate+"");*/

            if (mDbAdapter != null) {
                mDbAdapter.createData(mLastHrDate, mSessionStartTime,
                        mLastHr, mLastRr);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateUI_Barometer_Pressure(byte[] data) {
        try {
            String content = HexUtil.formatHexString(data, true);
            //String content = "DE 02 00 00";
            String[] contentArray = content.split(" ");

            String reverse = contentArray[3] + contentArray[2] + contentArray[1] + contentArray[0];
            int r = Integer.parseInt(reverse, 16);
            //Log.i(TAG+"-r", r+"");
            //Log.i("Barometer Pressure", content);
            double baroPressure = (double) r / (double) 10;
            updateHeightAboveSeaLevel(baroPressure);
            baroPressure = baroPressure / 100;
            txtDisplay_Baro_Pressure.setText(String.format("%.2f", baroPressure) + " hPa");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateUI_Barometer_Temperature(byte[] data) {
        try {
            String content = HexUtil.formatHexString(data, true);
//            String content = "EE 0B";
            String[] contentArray = content.split(" ");

            String reverse = contentArray[1] + contentArray[0];
            int r = Integer.parseInt(reverse, 16);
            //Log.i(TAG+"-r", r+"");
            //Log.i("Barometer Temperature", content);
            double baroTemperature = (double) r / (double) 100;
            txtDisplay_Baro_Temperature.setText(String.format("%.2f", baroTemperature) + " ºC");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateHeightAboveSeaLevel(double pressure) {
// calculate the altitude or height above sea level
        double R = 287.058; // gas constant for dry air in J/(kg·K)
        double T = 288.15; // temperature at sea level in Kelvin
        double g = 9.80665; // acceleration due to gravity in m/s^2
        double p0 = 101325; // atmospheric pressure at sea level in Pa

        double h = ((-R * T) / g) * Math.log(pressure / p0);

//        txtDisplay_Baro_HeightSea.setText(String.valueOf(h));
        txtDisplay_Baro_HeightSea.setText(String.format("%.2f", h) + " m");

// convert the altitude to a more convenient unit, such as meters or feet
    }

    public void onPPG() {
        service_Status_ECG = false;
        service_Status_PPG = true;
        service_Status_PPG_Raw = false;
        service_Status_HeartRate = false;
        String hexCode = getServiceCode();
        String successMessage = "PPG Service is on.";
        writeCharacteristic(hexCode, Constants.servicePowerOn, Constants.characteristicPowerOn, successMessage, "PPG");
    }

    public void offPPG() {
        service_Status_PPG = false;
        String hexCode = getServiceCode();
        String successMessage = "PPG Service is off.";
        writeCharacteristic(hexCode, Constants.servicePowerOn, Constants.characteristicPowerOn, successMessage, "");

        txtDisplay_PPG_SPO2.setText("");
        txtDisplay_PPG_HR.setText("");
        txtDisplay_PPG_Quality.setText("");
        txtDisplay_PPG_RR.setText("");

        notifyCharacteristicStop(bleDevice, Constants.servicePPG, Constants.characteristicPPG);
    }

    public void onPPG_Raw() {
    }

    public void offPPG_Raw() {
    }

    public void startPPGGraph() {
        onPPG();
        showGraphPPGPopup();
    }

    public void getUUIDs() {
        BleDevice device = getBleDevice();
        mBluetoothDevice = device.getDevice();
      if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
//            Toast.makeText(mContext, "Hello", Toast.LENGTH_SHORT).show();
            return;
        }
       mBluetoothGatt = mBluetoothDevice.connectGatt(this, true, mGattCallback);

        if (mBluetoothGatt != null) {
            if (mBluetoothGatt.discoverServices()) {
                Log.d("READ_Prakhar", "Service discovery started");
            } else {
                Log.d("READ_Prakhar", "Service discovery start failed");
            }
        } else {
            Log.d("READ_Prakhar", "BluetoothGatt is null");
        }
        boolean ans = mBluetoothGatt.discoverServices();
        Log.i("READ_Prakhar", String.valueOf(ans));

        mGattCallback.onServicesDiscovered(mBluetoothGatt, 1);


    }

    public void onECG(){
        service_Status_ECG = true;
        service_Status_PPG = false;
        service_Status_PPG_Raw = false;
        service_Status_HeartRate = false;
        String hexCode = getServiceCode();
        String successMessage = "ECG Service is on.";

//        notifyCharacteristic(serviceECG, characteristicECG, "ECG");
        //  ********* Ring
        writeCharacteristic(hexCode, Constants.servicePowerOn, Constants.characteristicPowerOn, successMessage, "ECG");
        //  ********* Ring


//                getUUIDs();   // function give all service id & their characteristics ID


        //checking for imu
        //readCharacteristic("00001816-0000-1000-8000-00805f9b34fb", "00002713-0000-1000-8000-00805f9b34fb");



        //  ******     // Patch start ****** Using Example thread ***** Working *************



//        BleDevice bleDevice = getBleDevice();
//        mBluetoothDevice = bleDevice.getDevice();
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }
//        try {
//            mBluetoothGatt = mBluetoothDevice.connectGatt(this,false,mGattCallback);
//            mBluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
//        }catch (Exception e){
//            Log.v("READ_PrakharErr",e.toString());
//        }
//        ecg_run_patch = true;
//        exampleThread = new ExampleThread();
//        exampleThread.start();

        //  **************     // Patch stop *********** Working *****************



        // *********** for patch // descriptor not work

//        BleDevice bleDevice = getBleDevice();
//        mBluetoothDevice = bleDevice.getDevice();
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }
//        BluetoothGatt gatt = mBluetoothDevice.connectGatt(getApplicationContext(), false, gattCallback);
//        String  sUUID = "6E400000-B5A3-F393-E0A9-E50E24DCCA9E";
//        String cUUID = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E";
//        sUUID = sUUID.toLowerCase();
//        cUUID = cUUID.toLowerCase();
//        BluetoothGattService service = gatt.getService(UUID.fromString(sUUID));
//        if(service!=null){
//            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(cUUID));
//            gatt.setCharacteristicNotification(characteristic, true);
//            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(descriptorClientConfiguration));
//            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//            gatt.writeDescriptor(descriptor);
//        }else{
//            Log.i("READ_Prakhar","Service is null");
//        }

        // ******************


        //
        //patch
//            while(true){
//


//            }

//       Patch
//        readCharacteristic("6E400000-B5A3-F393-E0A9-E50E24DCCA9E", "6E400001-B5A3-F393-E0A9-E50E24DCCA9E");

//        getUUIDs();   // function give all service id & their characteristics ID


    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] data = characteristic.getValue();
            // Process the data as needed
            String content = HexUtil.formatHexString(data, true);
            Log.i("READ_Prakhar", content);
        }
    };



    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            // Read the updated characteristic value
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            byte[] data = characteristic.getValue();
            // Process the data as needed
            String content = HexUtil.formatHexString(data, true);
            Log.i("READ_Prakhar_Characteristics", content);
            mBluetoothGatt.readCharacteristic(characteristic);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

//            Toast.makeText(mContext, "Hey Prakhar", Toast.LENGTH_SHORT).show();
            Log.i("READ_Prakhar_BGService ", "Service discovered ");


            if (status == BluetoothGatt.GATT_SUCCESS) {

                List<BluetoothGattService> services = gatt.getServices();

                for (BluetoothGattService service : services) {

//                    String vv = "6E400000-B5A3-F393-E0A9-E50E24DCCA9E";
//                    vv = vv.toLowerCase();
//                    if (service.getUuid().toString().equals(vv)) {
//                        Log.i("READ_Prakhar_Aman", "Found");
//                        String cUUID = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E";
//                        cUUID = cUUID.toLowerCase();
//                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(cUUID));
//                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                            // TODO: Consider calling
//                            //    ActivityCompat#requestPermissions
//                            // here to request the missing permissions, and then overriding
//                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                            //                                          int[] grantResults)
//                            // to handle the case where the user grants the permission. See the documentation
//                            // for ActivityCompat#requestPermissions for more details.
//                            return;
//                        }
//                        try {
//                            gatt.setCharacteristicNotification(characteristic, true);
//                            for (BluetoothGattDescriptor descriptor:characteristic.getDescriptors()){
//                                Log.e("READ_Prakhar_Descriptor", "BluetoothGattDescriptor: "+descriptor.getUuid().toString());
//                            }
//                            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(descriptorClientConfiguration));
//                            Log.i("READ_Prakhar_des",String.valueOf(descriptor));
//                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//                            gatt.writeDescriptor(descriptor);
//                        }catch (Exception e){
//                            Log.i("READ_Prakhar Error",e.toString());
//                        }
//                    }
                    Log.i("READ_Prakhar Service UUID: ", "READ_Prakhar Service UUID: " + service.getUuid().toString());
                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                    for (BluetoothGattCharacteristic characteristic : characteristics) {
                        Log.i(TAG, "READ_Prakhar Characteristic UUID: " + characteristic.getUuid().toString());

//                                BluetoothGattCharacteristic characteristic;

// ... code to retrieve the characteristic

                        // ****** checking notify property set or not **********
//                        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
//            // The PROPERTY_NOTIFY property is set for this characteristic
//                           Log.d("READ_Prakhar","Notify characteristic property is set!!");
//                      } else {
//                           Log.d("READ_Prakhar","Notify characteristic property is not set!!");
//                       // The PROPERTY_NOTIFY property is not set for this characteristic
//                       }

                        for (BluetoothGattDescriptor descriptor:characteristic.getDescriptors()){
                            Log.e("READ_Prakhar_Descriptor", "BluetoothGattDescriptor: "+descriptor.getUuid().toString());
                        }
                    }
                }
            }

//            if (status == BluetoothGatt.GATT_SUCCESS) {
//
//                BluetoothGattService ecgService = gatt.getService(ECG_SERVICE_UUID);
//                if (ecgService != null) {
//                    BluetoothGattCharacteristic ecgCharacteristic = ecgService.getCharacteristic(ECG_CHARACTERISTIC_UUID);
//                    if (ecgCharacteristic != null) {
//                        Log.i("READ_Prakhar", "READ_Prakhar ECG UUID Characteristic: " + ecgCharacteristic.getUuid().toString());
//                        Log.i("READ_Prakhar", "READ_Prakhar ECG UUID Service :  " + ecgService.getUuid().toString());
//                    } else {
//                        Log.i("READ_Prakhar", "ECG Characteristic not found");
//                    }
//                } else {
//                    Log.i("READ_Prakhar", "ECG Service not found");
//                }
//            }


        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                /**
                 * In case already connected to Pas device
                 */
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                Log.i("READ_Prakhar", "Connected to GATT server. " + gatt.discoverServices());
                // CALL THIS METHOD TO BEGIN DISCOVER SERVICES
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                gatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                /**
                 * In case disconnected Pas device
                 */
                Log.i("READ_Prakhar", "Disconnected from GATT server.");
            }
        }
    };


    public void offECG() {

//        try{
            //exampleThread.interrupt();
//        }catch (Exception e){

//        }
        service_Status_ECG = false;
        String hexCode = getServiceCode();
        String successMessage = "ECG Service is off.";
        writeCharacteristic(hexCode, Constants.servicePowerOn, Constants.characteristicPowerOn, successMessage, "");
        txtDisplay_ECG.setText("");
        notifyCharacteristicStop(bleDevice, Constants.serviceECG, Constants.characteristicECG);
    }

    public void startECGGraph() {
//        General.showAlert(mContext, "ECG Graph coming soon");
//        onECG();
        showGraphECGPopup();

    }

    private void showGraphECGPopup() {
        try {
            if (popupWindowGraphECG != null) {
                popupWindowGraphECG.dismiss();
            }
            ecg_graph_update = false;
            LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(mContext.LAYOUT_INFLATER_SERVICE);
            final View customView = layoutInflater.inflate(R.layout.popup_graph_ecg, null);
            customView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            TextView txtClose = (TextView) customView.findViewById(R.id.txtClose_ecg_graph);
            x_ecg = 0f;
            chartECG = (LineChart) customView.findViewById(R.id.chart_ecg);
//            chartECG.setOnChartValueSelectedListener((OnChartValueSelectedListener) this);
            chartECG.setDrawGridBackground(false);
            chartECG.getDescription().setEnabled(false);
            chartECG.setNoDataText("No chart data available. Use the menu to add entries and data sets!");
            XAxis xAxis = chartECG.getXAxis();
//            xAxis.setLabelCount(500);
//            ViewPortHandler viewPortHandler = chartECG.getViewPortHandler();
//            viewPortHandler.setMinMaxScaleX(0, 500);
////            float minXRange = 10;
//            float maxXRange = 10;
//            chartECG.setVisibleXRange(minXRange, maxXRange);
            chartECG.invalidate();
            dataSet_ecg = new LineDataSet(entries_ecg, "ECG");
            dataSet_ecg.setDrawCircles(false);
            dataSet_ecg.setCubicIntensity(2000f);
            dataSet_ecg.setDrawValues(false);
            dataSet_ecg.setLineWidth(3f);
//            chartECG.setDragDecelerationFrictionCoef(0.9f);
            lineData_ecg = new LineData(dataSet_ecg);
            chartECG.setData(lineData_ecg);

            LineData data = chartECG.getData();

            if (data == null) {
                data = new LineData();
                chartECG.setData(data);
            }

            ILineDataSet set = data.getDataSetByIndex(0);
            // set.addEntry(...); // can be called as well

            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }



//            x_ecg_start_time = System.currentTimeMillis();

            CountDownTimer myTimer = new CountDownTimer(100000,10){

                @Override
                public void onTick(long l) {
                    readCharacteristic("6E400000-B5A3-F393-E0A9-E50E24DCCA9E","6E400001-B5A3-F393-E0A9-E50E24DCCA9E");

                }

                @Override
                public void onFinish() {
                    Log.i("x_ecg_value",String.valueOf(x_ecg));
                }
            };


            XAxis xAxisECG = chartECG.getXAxis();
            xAxis.setValueFormatter(new IAxisValueFormatter() {
                @Override
                public String getFormattedValue(float value, AxisBase axis) {
//                    Log.i("READ_Aman",String.valueOf(x_ecg)+" "+value+" "+axis);
                    if(value%250==0){
                        return  String.valueOf((int)value/250);
                    }else{
                        return "";
                    }
//                    return xValues.get((int) value % xValues.size());
                }
            });
//
//            final Handler handler = new Handler();
//            Runnable runn = new Runnable(){
//
//                @Override
//                public void run() {
//                    handler.postDelayed(this,1000);
//                }
//            };
//
//            handler.post(runn);


            Random random = new Random();
            int result = random.nextInt(10) + 1;
            for(int i=0;i<2;i++){
                result = random.nextInt(10) + 1;
                entries_ecg.add(new Entry(x_ecg,0));
                x_ecg = x_ecg + 1f;
                Log.i("x_ecg_value_runn",String.valueOf(x_ecg));
                dataSet_ecg.notifyDataSetChanged();
                lineData_ecg.notifyDataChanged();
                chartECG.notifyDataSetChanged();
                chartECG.setVisibleXRangeMinimum(0);
                chartECG.setVisibleXRangeMaximum(500);
                chartECG.moveViewTo(dataSet_ecg.getEntryCount() - 5, 50f, YAxis.AxisDependency.LEFT);


            }

//
             new CountDownTimer(40,10){

                @Override
                public void onTick(long l) {
                    // from timing
//                    x_ecg_current_time = System.currentTimeMillis();
//                    x_ecg_plot_time = x_ecg_current_time - x_ecg_start_time;
//                    entries_ecg.add(new Entry(x_ecg_plot_time,0));

                   // current
//                    entries_ecg.add(new Entry(x_ecg,0));
//                    x_ecg = x_ecg + 1f;
//                    Log.i("x_ecg_value_runn",String.valueOf(x_ecg));
//                    dataSet_ecg.notifyDataSetChanged();
//                    lineData_ecg.notifyDataChanged();
//                    chartECG.notifyDataSetChanged();
//                    chartECG.setVisibleXRangeMinimum(0);
//                    chartECG.setVisibleXRangeMaximum(500);
//                    chartECG.moveViewTo(dataSet_ecg.getEntryCount() - 5, 50f, YAxis.AxisDependency.LEFT);

                }

                @Override
                public void onFinish() {

                    ecg_graph = true;
                    onECG();

//                  Toast.makeText(mContext, "Hello", Toast.LENGTH_SHORT).show();
//                  myTimer.start(); // Patch
                  Log.i("x_ecg_value",String.valueOf(x_ecg));
                  //
                  // Ring
//                    String hexCode = getServiceCode();
//                    String successMessage = "ECG Service is on.";
//                    service_Status_ECG = true;
//                    writeCharacteristic(hexCode, servicePowerOn, characteristicPowerOn, successMessage, "ECG");

                }
            }.start();


             // Patch

//            CountDownTimer myTimer = new CountDownTimer(100000,10){
//
//                @Override
//                public void onTick(long l) {
//                    readCharacteristic("6E400000-B5A3-F393-E0A9-E50E24DCCA9E","6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
//
//                 }
//
//                @Override
//                public void onFinish() {
//
//                }
//            };
//
//            myPlotECG();

            //instantiate popup window
            popupWindowGraphECG = new PopupWindow(customView, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            //display the popup window
            popupWindowGraphECG.showAtLocation(layout_CustomOperationActivity, Gravity.CENTER, 0, 0);
            popupWindowGraphECG.setOutsideTouchable(true);
            popupWindowGraphECG.setFocusable(true);
            popupWindowGraphECG.update();

            txtClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (popupWindowGraphECG != null) {
                        ecg_graph = false;

                        x_ecg = 0;
                        entries_ecg = new ArrayList<>();
                        dataSet_ecg = new LineDataSet(entries_ecg, "ECG");
                        dataSet_ecg.setDrawCircles(false);
                        dataSet_ecg.setCubicIntensity(2000f);
                        dataSet_ecg.setDrawValues(false);
                        dataSet_ecg.setLineWidth(3f);

                        lineData_ecg = new LineData(dataSet_ecg);
                        chartECG.setData(lineData_ecg);

                        dataSet_ecg.notifyDataSetChanged();
                        lineData_ecg.notifyDataChanged();
                        chartECG.notifyDataSetChanged();
                        chartECG.setVisibleXRangeMinimum(0);
                        chartECG.setVisibleXRangeMaximum(500);
                        chartECG.moveViewTo(x_ecg - 7, 50f, YAxis.AxisDependency.LEFT);

                       // off the ring ecg
                         offECG();

//                        longRunningTaskFuture.cancel(true);
                        popupWindowGraphECG.dismiss();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private LineDataSet createSet() {

        LineDataSet set = new LineDataSet(null, "DataSet 1");
        set.setLineWidth(2.5f);
        set.setCircleRadius(4.5f);
        set.setColor(Color.rgb(240, 99, 99));
        set.setCircleColor(Color.rgb(240, 99, 99));
        set.setHighLightColor(Color.rgb(190, 190, 190));
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setValueTextSize(10f);

        return set;
    }


    public void onHeartRate() {
        service_Status_ECG = false;
        service_Status_PPG = false;
        service_Status_PPG_Raw = false;
        service_Status_HeartRate = true;
        String hexCode = getServiceCode();
        String successMessage = "Heart Rate Service is on.";
        writeCharacteristic(hexCode, Constants.servicePowerOn, Constants.characteristicPowerOn, successMessage, "HeartRate");
        //readCharacteristic(serviceHeartRate, characteristicHeartRate);
    }

    public void offHeartRate() {
        service_Status_HeartRate = false;
        String hexCode = getServiceCode();
        String successMessage = "Heart Rate Service is off.";
        writeCharacteristic(hexCode, Constants.servicePowerOn, Constants.characteristicPowerOn, successMessage, "");
        txtDisplay_HeartRate.setText("");
        notifyCharacteristicStop(bleDevice, Constants.serviceHTS, Constants.characteristicHeartRate);
    }

    public void onBatteryLevel() {
        service_Status_BatteryLevel = true;
        String hexCode = getServiceCode();
        String successMessage = "Battery Level Service is on.";
        writeCharacteristic(hexCode, Constants.servicePowerOn, Constants.characteristicPowerOn, successMessage, "BatteryLevel");
    }

    public void offBatteryLevel() {
        service_Status_BatteryLevel = false;
        String hexCode = getServiceCode();
        String successMessage = "Battery Level Service is off.";
        writeCharacteristic(hexCode, Constants.servicePowerOn, Constants.characteristicPowerOn, successMessage, "");
        txtDisplay_BatteryLevel.setText("");
        notifyCharacteristicStop(bleDevice, Constants.serviceBatteryLevel, Constants.characteristicBatteryLevel);
    }

    public void onHTS() {
        service_Status_HTS = true;
        String hexCode = getServiceCode();
        String successMessage = "HTS Service is on.";
        writeCharacteristic(hexCode, Constants.servicePowerOn, Constants.characteristicPowerOn, successMessage, "HTS");
    }

    public void offHTS() {
        service_Status_HTS = false;
        String hexCode = getServiceCode();
        String successMessage = "HTS Service is off.";
        writeCharacteristic(hexCode, Constants.servicePowerOn, Constants.characteristicPowerOn, successMessage, "");
        txtDisplay_HTS_Celsius.setText("");
        txtDisplay_HTS_Core.setText("");
        txtDisplay_HTS_Env.setText("");
//        txtDisplay_HTS_Fahrenheit.setText("");

        notifyCharacteristicStop(bleDevice, Constants.serviceHTS, Constants.characteristicHTS);
    }

    public void onBarometer() {
        service_Status_Barometer = true;
        String hexCode = getServiceCode();
        String successMessage = "Barometer Service is on.";
        writeCharacteristic(hexCode, Constants.servicePowerOn, Constants.characteristicPowerOn, successMessage, "Barometer");

//        readCharacteristic(serviceBarometer, characteristicBaroPressure);

        //readCharacteristic(serviceBarometer, characteristicBaroTemperature);

        //writeDescriptor("0100", Constants.descriptorClientConfiguration);
//        writeCharacteristic(hexCode, servicePowerOn, characteristicPowerOn, successMessage, "Barometer");
    }

    public void offBarometer() {
        service_Status_Barometer = false;
        String hexCode = getServiceCode();
        String successMessage = "Barometer Service is off.";
        writeCharacteristic(hexCode, Constants.servicePowerOn, Constants.characteristicPowerOn, successMessage, "");
        txtDisplay_Baro_Pressure.setText("");
        txtDisplay_Baro_Temperature.setText("");
        txtDisplay_Baro_HeightSea.setText("");
        notifyCharacteristicStop(bleDevice, Constants.serviceBarometer, Constants.characteristicBaroPressure);
        notifyCharacteristicStop(bleDevice, Constants.serviceBarometer, Constants.characteristicBaroTemperature);
    }

    private void onIMU() {
//        Log.i("READ_Prakhar","It's IMU call");

//        getUUIDs();

//        readCharacteristic(serviceIMU, characteristicIMU);


//        String hexCode = getServiceCode();
//        writeCharacteristic(hexCode, servicePowerOn, characteristicPowerOn, successMessage, "IMU");

//        String successMessage = "IMU Service is on.";
//        service_Status_IMU = true;
//        notifyCharacteristic(serviceIMU,characteristicIMU,"IMU");

        txtChangeAble = true;
        txtIMURunn = true;
        service_Status_IMU = true;
        String hexCode = getServiceCode();
        String successMessage = "IMU Service is on.";
        accelerometerValues = new float[3];
        gyroscopeValues = new float[3];
        timestamp = 0;
        writeCharacteristic(hexCode, Constants.servicePowerOn, Constants.characteristicPowerOn, successMessage, "IMU");

    }

    private void offIMU(){
//        service_Status_IMU = false;
//        String hexCode = getServiceCode();
//        String successMessage = "IMU Service is off.";
////        writeCharacteristic(hexCode, servicePowerOn, characteristicPowerOn, successMessage, "");
////        txtDisplay_HTS_Celsius.setText("");
//        notifyCharacteristicStop(bleDevice, serviceIMU, characteristicIMU);

        txtChangeAble = false; // for displaying the value for 3 seconds..
        service_Status_IMU = false;
        String hexCode = getServiceCode();
        String successMessage = "IMU Service is off.";
        writeCharacteristic(hexCode, Constants.servicePowerOn, Constants.characteristicPowerOn, successMessage, "");
        notifyCharacteristicStop(bleDevice, Constants.serviceIMU, Constants.characteristicIMU); //**********
        txtDisplay_Accx.setText("");
        txtDisplay_Accy.setText("");
        txtDisplay_Accz.setText("");
        txtDisplay_Gyrox.setText("");
        txtDisplay_Gyroy.setText("");
        txtDisplay_Gyroz.setText("");
        txtDisplay_mlc.setText("");
        txtDisplay_Temp_IMU.setText("");

    }


    public void initActionBar(String strTitle) {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setBackgroundDrawable(getApplicationContext().getResources().getDrawable(R.drawable.bg_custom_bar_border_bottom));

        final Drawable upArrow = mContext.getDrawable(R.drawable.ic_arrow_back);
        upArrow.setColorFilter(getResources().getColor(R.color.colorWhite), PorterDuff.Mode.SRC_ATOP);
        getSupportActionBar().setHomeAsUpIndicator(upArrow);

        SpannableString s = new SpannableString(strTitle);
        int c_PRIMARY = Color.parseColor(Constants.COLOR_WHITE);
        s.setSpan(new ForegroundColorSpan(c_PRIMARY), 0, strTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        getSupportActionBar().setTitle(s);
    } // end initActionBar()

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_layout, menu);
        return true;
    }

    @SuppressLint({"MissingPermission"})
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @SuppressLint({"MissingPermission"})
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDbAdapter != null) {
            mDbAdapter.close();
            mDbAdapter = null;
        }
    }

    private void returnResult(int resultCode, String msg) {
        Intent data = new Intent();
        if (msg != null) {
            data.putExtra(IConstants.MSG_CODE, msg);
        }
        setResult(resultCode, data);
        finish();
    }

} // end class