package com.slimiot.hrv.Home;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import hrv.HRVLibFacade;
import hrv.RRData;
import hrv.calc.parameter.HRVParameter;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.clj.fastble.BleManager;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.utils.HexUtil;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.slimiot.hrv.BleAction.BluetoothConnection;
import com.slimiot.hrv.BleOperations.BleActivities;
import com.slimiot.hrv.R;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;



public class MainActivity extends AppCompatActivity {

    private String TAG = "READ_Prakhar";
    Context mContext;
    private TextView bleStateTimer,txtDisplayHRAvg,txtDisplayHR,txtDisplayHRMax,txtDisplayRRIndex,txtDisplayStressIndex;
    private BleDevice bleDevice;
    public static final String KEY_DATA = "key_data";
    long mSessionStartTime;

    private Button startHRV,exitHRV;

    private boolean rrDetector = true;

    private ArrayList<Double> rrIntervalValues;

    private int rrIndexCount = 0,artifactCount =0;
    //    private int hrIntervalSize = 0;
    private Queue<Integer> hrValuesList;

    private Handler rrHandler,hrHandler;
    private Runnable rrRunnable,hrRunnable;
    private int hrValueDisplay,hrValuesSum = 0;
    private double hrvValue = 0;
    private int hrMaxValue = Integer.MIN_VALUE,hrMinValue = Integer.MAX_VALUE, rrMaxValue = Integer.MIN_VALUE,rrMinValue = Integer.MAX_VALUE,rrMinGraph = Integer.MAX_VALUE;

    private TextView txtDisplayHRVScore,txtDisplaySDNN,txtDisplayLfByHf,txtDisplayRMSSD,txtDisplayPNN50,txtDisplayArtifact;
    private TextView txtDisplayRRMax,txtDisplayRRMin,txtDisplayRRAvg,txtDisplayAtrialFibrillationChances;
    private Map<Integer,Integer> rrDataMapGraph;
    private ArrayList<BarEntry> barEntries;
    private ArrayList<BarEntry> barEntriesRed;

    private ArrayList<Entry> lineEntries;
    private CombinedChart combinedChart;

    private BroadcastReceiver mRecieverHeartRate;
    private BroadcastReceiver mDeviceConnectionReciever;

    // Applying 3 sigma var >>>>>>
    private  double sum = 0;
    private  double sumSquared = 0;
    private  double mean = 0;
    private  double stdDev = 0;

    private CountDownTimer countDownTimer;
    private int yMaxRR = 0;

    private int counter = 0;
    //  >>>>>>


    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;
    private String userId, gender, dob;
    private int age, dobM, dobY, currentYear, currentMonth;
    private ImageView stressIndexHint,heartRateHint,hrvScoreHint;

    private int conditionAtrialFibrillationChances = 0;

    // values global pass to result activity, after test
    private int hrGlobal=0,ratioLfHfGlobal=0;
    private double sdnnGlobal=0,rmssdGlobal=0,pnn50Global=0,sd1Global=0,sd2Global=0,hrvScoreGlobal=0,stressIndexGlobal=0;

    private double pnnStandard = 0;

    private SharedPreferences sharedPreferences;
    private String genderValue = null , dobValue=null ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();
        sharedPreferences = this.getSharedPreferences("com.slimiot.hrv",MODE_PRIVATE);
//        bottomNavigationView = findViewById(R.id.bottomNavigation);
         getSavedData();
        mContext = this;
        txtDisplayRRMax = findViewById(R.id.txtDisplayRRMax);
        txtDisplayRRMin = findViewById(R.id.txtDisplayRRMin);
        txtDisplayRRAvg = findViewById(R.id.txtDisplayRRAvg);
        txtDisplayHRMax = findViewById(R.id.txtDisplayHRMax);
        txtDisplayHR = findViewById(R.id.txtDisplayHR);
        txtDisplayHRAvg = findViewById(R.id.txtDisplayHRAvg);
        txtDisplayHRVScore = findViewById(R.id.txtDisplayHRVScore);
        txtDisplaySDNN = findViewById(R.id.txtDisplaySDNN);
        txtDisplayLfByHf = findViewById(R.id.txtDisplayLfByHf);
        txtDisplayRMSSD = findViewById(R.id.txtDisplayRMSSD);
        txtDisplayPNN50 = findViewById(R.id.txtDisplayPNN50);
        txtDisplayRRIndex = findViewById(R.id.txtDisplayRRIndex);
        txtDisplayStressIndex = findViewById(R.id.txtDisplayStressIndex);
        txtDisplayArtifact = findViewById(R.id.artifactsValue);
        heartRateHint = findViewById(R.id.heartRateHint);
        hrvScoreHint = findViewById(R.id.hrvScoreHint);
        stressIndexHint = findViewById(R.id.stressIndexHint);
        txtDisplayAtrialFibrillationChances = findViewById(R.id.txtDisplayAtrialFibrillationChances);
        mRecieverHeartRate = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Handle the sensor data here
                byte[] data = intent.getByteArrayExtra("data");
                String content = HexUtil.formatHexString(data, true);
                Log.i("READ_UI_DATA_HR_HRVS", content);
                if(BleActivities.measureHRV) {
//                    updateUI_HeartRate(data);
                    updateUI_RRView(data);
                }
            }
        };
        IntentFilter filterHR = new IntentFilter("sensor-dataHR");
        LocalBroadcastManager.getInstance(this).registerReceiver(mRecieverHeartRate, filterHR);

        // making timer to run for 3 min..

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        countDownTimer =  new CountDownTimer(181000, 1000) {

            public void onTick(long millisUntilFinished) {
                // Do nothing on tick
                Duration duration = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
//                    millisUntilFinished = 181000 - millisUntilFinished;
                    millisUntilFinished = millisUntilFinished;
                    duration = Duration.ofMillis(millisUntilFinished);
                    // Format the duration to "mm:ss" using a DateTimeFormatter
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("mm:ss");
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        if(duration.toSecondsPart()>=0&&duration.toSecondsPart()<=9){
                            String mmss = duration.toMinutesPart() + ":0" + duration.toSecondsPart();
                            bleStateTimer.setText(mmss);
                        }else{
                            String mmss = duration.toMinutesPart() + ":" + duration.toSecondsPart();
                            bleStateTimer.setText(mmss);
                        }
                    }
                }
            }
            @Override
            public void onFinish() {
                Toast.makeText(mContext, "HRV measurement 3 minutes done.", Toast.LENGTH_SHORT).show();
                BleActivities.measureHRV = false;
                startHRV.setAlpha(0.7f);
                if(hrRunnable!=null) {
                    hrHandler.removeCallbacks(hrRunnable);
                }
                if(rrRunnable!=null) {
                    rrHandler.removeCallbacks(rrRunnable);
                }
                performAfterTest();
//                doSomethingonRR();
            }
        };
        //******************************


        rrDataMapGraph = new HashMap<Integer,Integer>();
        barEntries = new ArrayList<>();
        barEntriesRed = new ArrayList<>();
        lineEntries = new ArrayList<>();
        rrIntervalValues = new ArrayList<>();
        hrValuesList = new LinkedList<>();


        lineEntries.add(new Entry(0f, 0f));
        lineEntries.add(new Entry(0f, 0f));
        lineEntries.add(new Entry(0f, 0f));
        combinedChart = findViewById(R.id.combinedChart);
        combinedChart.setVisibility(View.INVISIBLE);
        bleStateTimer = findViewById(R.id.connectBleStateHRV);
        startHRV = findViewById(R.id.startHRVButton);
        exitHRV = findViewById(R.id.exitHRVButton);
        bleStateTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                String val = bleStateTimer.getText().toString();
//                if(val.equals("Connect Now")){
//                    Intent intent = new Intent(getApplicationContext(), BluetoothConnection.class);
//                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                    startActivity(intent);
//                }
            }
        });
        startHRV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(bleDevice == null){
                    Toast.makeText(MainActivity.this, "Device not connected", Toast.LENGTH_SHORT).show();
                }else {
//                    BleActivities.onHeartRate();
                    if(!BleActivities.measureHRV) {
                        BleActivities.onHeartRate();
                        BleActivities.measureHRV = true;
//                        if(BleActivities.hrPPGState){
//                            // hr is already on no need to start//
//                         //   Toast.makeText(mContext, "hr is already on no need to start", Toast.LENGTH_SHORT).show();
//                            BleActivities.measureHRV = true;
//                        }else{
//                            // we have to start the hr && turn off the ppg.....
//                            try {
//                            //    Toast.makeText(mContext, "ppg turned off hr on", Toast.LENGTH_SHORT).show();
//                                BleActivities.measureHRV = true;
////                                BleActivities.offPPG();
////                                Thread.sleep(1000);
//                                BleActivities.onHeartRate();
//                            }catch (Exception e){
//
//                            }
//                        }
                        if (countDownTimer != null) {
                            countDownTimer.cancel();
                        }
                        startHRV.setAlpha(0.7f);
                        countDownTimer.start();
                        exitHRV.setText("Reset");
                    }
                }
            }
        });
        exitHRV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(exitHRV.getText().toString().equals("Reset")){
                    resetData();
                }else {
                    BleActivities.measureHRV = false;
                    finish();
                }
            }
        });
        initData();
        heartRateHint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showHeartRateHintPopUp();
            }
        });
        hrvScoreHint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showHrvScoreHintPopUp();
            }
        });
        stressIndexHint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showStressIndexPopUp();
            }
        });
    }

    private void getSavedData() {
            genderValue = sharedPreferences.getString("gender","csc");
            dobValue = sharedPreferences.getString("dob","csc");
//        Toast.makeText(getApplicationContext(), genderValue, Toast.LENGTH_SHORT).show();
//        Toast.makeText(getApplicationContext(), dobValue, Toast.LENGTH_SHORT).show();
        calculateAge();
    }

    private void calculateAge() {
        dob = dobValue;
        if (dob != null) {
            int lengthDob = dob.length();
            String dobYear = dob.substring(lengthDob - 4, lengthDob);
            String dobMonth = dob.substring(0, 3);
            if (dobMonth.equals("JAN"))
                dobM = 1;
            else if (dobMonth.equals("FEB"))
                dobM = 2;
            else if (dobMonth.equals("MAR"))
                dobM = 3;
            else if (dobMonth.equals("APR"))
                dobM = 4;
            else if (dobMonth.equals("MAY"))
                dobM = 5;
            else if (dobMonth.equals("JUN"))
                dobM = 6;
            else if (dobMonth.equals("JUL"))
                dobM = 7;
            else if (dobMonth.equals("AUG"))
                dobM = 8;
            else if (dobMonth.equals("SEP"))
                dobM = 9;
            else if (dobMonth.equals("OCT"))
                dobM = 10;
            else if (dobMonth.equals("NOV"))
                dobM = 11;
            else if (dobMonth.equals("DEC"))
                dobM = 12;
            dobY = Integer.parseInt(dobYear);
            Calendar cal = Calendar.getInstance();
            currentYear = cal.get(Calendar.YEAR);
            currentMonth = cal.get(Calendar.MONTH);
            currentMonth = currentMonth + 1;
            age = currentYear - dobY;
            if (currentMonth < dobM) {
                age--;
            }
//                    Toast.makeText(mContext, age+" ", Toast.LENGTH_SHORT).show();
        }
    }

    private void initData() {
        bleDevice = getIntent().getParcelableExtra(KEY_DATA);

        BleActivities.mBleDevice = bleDevice;

        if (bleDevice == null) {
//            connectBle.setText("Connect Now");
            bleStateTimer.setText("0:00 / 3:00");
            startHRV.setAlpha(0.7f);
        } else {
//            connectBle.setText("Connected");
//            bleDevice = getIntent().getParcelableExtra(KEY_DATA);
//            if (bleDevice == null) {
////            bleStateTimer.setText("Connect Now");
//                bleStateTimer.setText("0:00 / 3:00");
//                startHRV.setAlpha(0.7f);
//            }else{
                bleStateTimer.setText("0:00 / 3:00");
//            }
            mSessionStartTime = new Date().getTime();
            BleActivities.setBleDevice(bleDevice);
            BleActivities.setContext(mContext);

            if(mDeviceConnectionReciever==null) {
//             Toast.makeText(MainActivity.this, "Hello", Toast.LENGTH_SHORT).show();
                mDeviceConnectionReciever = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        // Handle the sensor data here
                        String deviceName = intent.getStringExtra("deviceName");
                        String deviceAddress = intent.getStringExtra("deviceAddress");
//                        connectBle.setText("Connect Now");
                        resetData();
                        Toast.makeText(context, deviceName + " is disconnected", Toast.LENGTH_SHORT).show();
                        Log.i("READ_UI_DATA_Dev", deviceName + " " + deviceAddress + " disconnected");
                    }
                };
                IntentFilter filterDeviceConnection = new IntentFilter("DEVICE_DISCONNECTED");
                LocalBroadcastManager.getInstance(this).registerReceiver(mDeviceConnectionReciever, filterDeviceConnection);
            }

        }
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();


        // showPopUp - Want to disconnect device.  if yes send back to starting screen

//        if (BleManager.getInstance().isConnected(bleDevice)&&BleActivities.mBleDevice != null) {
//            showPopUPbackPressed();
//        }else{
//            Intent intent = new Intent(getApplicationContext(), BluetoothConnection.class);
//            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//            startActivity(intent);
//        }


        if (BleManager.getInstance().isConnected(bleDevice)&&BleActivities.mBleDevice != null) {
            disconnectDevicePopUp();
        }else{
            Intent intent = new Intent(getApplicationContext(), BluetoothConnection.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }




//        Intent intent = new Intent(getApplicationContext(), BluetoothConnection.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        startActivity(intent);


//        Intent intent = new Intent(getApplicationContext(), BluetoothConnection.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        startActivity(intent);

//        finish();
//        disconnectDevicePopUp("Do you want to close the application?");



//        finish();
    }
    @Override
    public void onPause() {
        super.onPause();
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mDeviceConnectionReciever);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRecieverHeartRate);
        if(hrRunnable!=null) {
//            Log.i("READ_Prakhar_Des","Done");
            hrHandler.removeCallbacks(hrRunnable);
        }
        if(rrRunnable!=null) {
//            Log.i("READ_Prakhar_Des","Done");
            rrHandler.removeCallbacks(rrRunnable);
        }
        if(countDownTimer!=null){
            countDownTimer.cancel();
        }
        BleActivities.measureHRV = false;
    }
    public void disconnectDevicePopUp(){
        final AlertDialog.Builder d = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = MainActivity.this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_disconnect_ble_device, null);
        d.setView(dialogView);
        final TextView ok =dialogView.findViewById(R.id.okDisconnectDevice);
        final TextView cancel=dialogView.findViewById(R.id.cancelDisconnectDevice);
        AlertDialog alertDialog = d.create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertDialog.getWindow().setLayout(600, 400);
        alertDialog.show();
        ok.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onClick(View view) {
//                disconnectFromDevice();
//                unregisterServices();

                Intent intent = new Intent(getApplicationContext(), BluetoothConnection.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                alertDialog.dismiss();

//                alertDialog.dismiss();
//                if(title.equals("Do you want to close the application?")){
////                    System.exit(0);
//
//                }
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });
    }
    public void disconnectFromDevice() {
//        BleManager bleManager = BleManager.getInstance();
        if (BleManager.getInstance().isConnected(bleDevice)) {
//            String deviceAddress = bleDevice.getDevice().getAddress();
            BleManager.getInstance().disconnect(bleDevice);
            BleManager.getInstance().disconnectAllDevice();

            boolean val = BleManager.getInstance().isConnected(bleDevice);
            Log.i("Ble connection",val+" ");


//            BluetoothDevice mBluetoothDevice = bleDevice.getDevice();
//            mBluetoothDevice.

            BleActivities.mBleDevice = null;

//            Intent i = new Intent(getApplicationContext(),BluetoothConnection.class);
//            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
//            sharedPreferences.edit().putString("previousBluetoothDeviceAddress","").apply();
//            startActivity(i);
//            finish();
        }
    }
    private void showStressIndexPopUp() {
        final android.app.AlertDialog.Builder d = new android.app.AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = MainActivity.this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_stressindex_hint, null);
        d.setView(dialogView);
        final TextView ok = dialogView.findViewById(R.id.okStressIndexHint);
        android.app.AlertDialog alertDialog = d.create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertDialog.getWindow().setLayout(600, 400);
        alertDialog.show();
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });
    }

    private void showHrvScoreHintPopUp() {
        final android.app.AlertDialog.Builder d = new android.app.AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = MainActivity.this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_hrvscore_hint, null);
        d.setView(dialogView);
        final TextView ok = dialogView.findViewById(R.id.okHRVScoreHint);
        android.app.AlertDialog alertDialog = d.create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertDialog.getWindow().setLayout(600, 400);
        alertDialog.show();
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });
    }

    private void showHeartRateHintPopUp() {
        final android.app.AlertDialog.Builder d = new android.app.AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = MainActivity.this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_heartrate_hint, null);
        d.setView(dialogView);
        final TextView ok = dialogView.findViewById(R.id.okHeartRateHint);
        android.app.AlertDialog alertDialog = d.create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertDialog.getWindow().setLayout(600, 400);
        alertDialog.show();
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });
    }

    private void resetData() {
        if(hrRunnable!=null) {
            hrHandler.removeCallbacks(hrRunnable);
        }
        if(rrRunnable!=null) {
            rrHandler.removeCallbacks(rrRunnable);
        }
        if(countDownTimer!=null){
            countDownTimer.cancel();
        }
        txtDisplayHRVScore.setText("0");
        txtDisplayHR.setText("0");
        txtDisplayStressIndex.setText("0");
        txtDisplayHRAvg.setText("Avg ");
        txtDisplayHRMax.setText("Max ");
        txtDisplaySDNN.setText("");
        txtDisplayRMSSD.setText("");
        txtDisplayLfByHf.setText("");
        txtDisplayPNN50.setText("");
        txtDisplayAtrialFibrillationChances.setText("");
        txtDisplayRRIndex.setText("RR Values: "+"0");
        txtDisplayArtifact.setText("Artifacts: "+"0");
        rrDataMapGraph = new HashMap<Integer,Integer>();
        rrIntervalValues = new ArrayList<>();
        barEntries = new ArrayList<>();
        bleStateTimer.setText("00:00");
        txtDisplayRRMin.setText("Min");
        txtDisplayRRMax.setText("Max");
        txtDisplayRRAvg.setText("Avg");
        hrValuesSum = 0;
        hrValueDisplay = 0;
        hrvValue = 0;
        hrValuesList = new LinkedList<>();
        bleStateTimer.setText("00:00");
        rrDetector = true;
        rrIndexCount = 0;
        artifactCount = 0;
        hrMaxValue = Integer.MIN_VALUE;
        hrMinValue = Integer.MAX_VALUE;
        rrMaxValue = Integer.MIN_VALUE;
        rrMinValue = Integer.MAX_VALUE;
        rrMinGraph = Integer.MAX_VALUE;
        sum = 0;
        sumSquared = 0;
        mean = 0;
        stdDev = 0;
        yMaxRR = 0;
        BleActivities.measureHRV = false;
        counter = 0;
        plotGraph();
        startHRV.setAlpha(1f);
    }

    public void goBack(View view) {
        BleActivities.measureHRV = false;
        finish();
    }
    public void updateUI_RRView(byte[] data){
        gender = genderValue;
        if(data.length!=4) {
            int kAdd = BleActivities.k;
            String content = HexUtil.formatHexString(data, true);
            Log.i("HeartRate Data", content);
            String[] contentArray = content.split(" ");
            String reverse = contentArray[2+kAdd] + contentArray[1+kAdd];
            String rrString = contentArray[4+kAdd] + contentArray[3+kAdd];
            int currentHR = Integer.parseInt(reverse, 16);
            // first doing hr work
            // find max & min & avg.......
            hrMaxValue = Math.max(hrMaxValue, currentHR);
            hrMinValue = Math.min(hrMinValue, currentHR);
//            txtDisplayHRMin.setText("Min "+hrMinValue);
            txtDisplayHRMax.setText("Max " + hrMaxValue);

            double hrMax =0,hrMax1=0,hrMax2=0;
            if(gender.equals("Female")){
                if(age<15) {
                    hrMax = 206 - (0.88 * 15);
                }else{
                    hrMax = 206 - (0.88 * age);
                }
            }else if(gender.equals("Male")){
                if(age<15) {
                    hrMax = 217-(0.85*15);
                }else{
                    hrMax = 217-(0.85*age);
                }
            }else{
                if(age<15) {
                    hrMax1 = 206 - (0.88 * 15);
                }else{
                    hrMax1 = 206 - (0.88 * age);
                }
                if(age<15) {
                    hrMax2 = 217-(0.85*15);
                }else{
                    hrMax2 = 217-(0.85*age);
                }
                hrMax = hrMax1 + hrMax2;
                hrMax = hrMax/2;
            }

            if(currentHR<=0.5*hrMax){
                txtDisplayHR.setTextColor(Color.rgb(0,204,0));
            }else if(currentHR>0.5*hrMax&&currentHR<=0.6*hrMax){
                txtDisplayHR.setTextColor(Color.rgb(102,255,102));
            }else if(currentHR>0.6*hrMax){
                txtDisplayHR.setTextColor(Color.RED);
            }


            txtDisplayHR.setText(String.valueOf(currentHR));


            hrValuesList.offer(currentHR);
            hrValuesSum = hrValuesSum + currentHR;
            if (rrDetector) {
                hrHandler = new Handler();
                hrRunnable = new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hrValueDisplay = hrValuesSum / hrValuesList.size();
//                                Log.i("READ_Prakhar_HRVC2",hrValuesSum+" "+hrValuesList.size());
                                txtDisplayHRAvg.setText("Avg " + String.valueOf(hrValueDisplay));
                                hrGlobal = hrValueDisplay;
                                hrHandler.postDelayed(hrRunnable, 1000);
                            }
                        });
                    }
                };
            }
            //***************** RR calculations*************************** //

            // d = rr in milli second....
            int rrmis = Integer.parseInt(rrString, 16); // rrmis
            Log.i("READ_Prakhar_MeHRV",currentHR+" "+rrmis);
            double rrValue = (double) rrmis / 1000; // rr in seconds
//            if (rrValue == 0) {
            // rrValue are in second
            if (rrmis<280 || rrmis>1500) {
                artifactCount++;
                txtDisplayArtifact.setText("Artifacts: " + artifactCount);
            } else {
                if (rrIntervalValues.size() >= 2) {
                    int k = rrIntervalValues.size() - 1;
//                    Log.i("RR_Values", k + 1 + " " + rrIntervalValues.get(k) + " " + rrIntervalValues.get(k - 1) + " " + rrIntervalValues.get(k - 2) + " " + rrValue);

                    String a1 = String.valueOf(rrIntervalValues.get(k));
                    String a2 = String.valueOf(rrIntervalValues.get(k - 1));
//                    String a3 = String.valueOf(rrIntervalValues.get(k - 2));
                    String a4 = String.valueOf(rrValue);
                    if (a1.equals(a2) && a2.equals(a4)) {
                        artifactCount++;
                        txtDisplayArtifact.setText("Artifacts: " + artifactCount);
                        Log.i("RR_Values", "Equal");
                    } else {
                        addRRValue(rrValue);
                        Log.i("RR_Values", "Not Equal");
                    }

                } else {
                    addRRValue(rrValue);
                }
            }

            // HRV SDNN PNN RMSSD NN50 calculation //

            if (rrDetector) {
                rrHandler = new Handler();
                rrRunnable = new Runnable() {
                    @Override
                    public void run() {
                        // Stuff that updates the UI
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateHRV();
                            }
                        });
                        rrHandler.postDelayed(this, 1000);
                    }
                };
            }

            // *********************************** //
            if (rrDetector) {
                rrDetector = false;
                hrHandler.post(hrRunnable);
                rrHandler.post(rrRunnable);
            }
        }
    }


    private void updateHRV() {

        Log.i("READ_Prakhar_RR2",rrIntervalValues.size()+" "+rrIntervalValues.toString());
        double hrv = HRVCalculator.getHRV(rrIntervalValues);

        double SDNN_1=0.95*(0.0063*Math.pow(age,2)-1.375*age+110); // less than red
        double PNN50_1=0.95*(-0.105*age+10.5);                    //   same
        pnnStandard = PNN50_1;
        double RMSSD_1 = 90-age;


        // orange  between them

        double SDNN_2 = SDNN_1 * 1.105;     // more than then green
        double PNN50_2 = PNN50_1 * 1.105;   // same
        double RMSSD_2 = RMSSD_1 * 1.25; // more than that green

        double hrvStandard = 0,hrvStandard1=0,hrvStandard2=0;
        if(gender.equals("Male")){
            if(age<20){
                hrvStandard =  0.0034*Math.pow(20,2)-0.7282*20+97.744;
            }else {
                hrvStandard =  0.0034*Math.pow(age,2)-0.7282*age+97.744;
            }
        }else if(gender.equals("Female")){
            if(age<20) {
                hrvStandard = 0.0067 * Math.pow(20, 2) - 0.9862 * 20 + 100.26;
            }else {
                hrvStandard = 0.0067 * Math.pow(age, 2) - 0.9862 * age + 100.26;
            }
        }else if(gender.equals("Other")){
            if(age<20){
                hrvStandard1 =  0.0034*Math.pow(20,2)-0.7282*20+97.744;
            }else {
                hrvStandard1 =  0.0034*Math.pow(age,2)-0.7282*age+97.744;
            }
            if(age<20) {
                hrvStandard2 = 0.0067 * Math.pow(20, 2) - 0.9862 * 20 + 100.26;
            }else {
                hrvStandard2 = 0.0067 * Math.pow(age, 2) - 0.9862 * age + 100.26;
            }
            hrvStandard = hrvStandard1 + hrvStandard2;
            hrvStandard = hrvStandard/2;
        }


        hrvValue = hrv;
        hrvValue = hrvValue*1000;


        if(hrvValue>1.05*hrvStandard){
            txtDisplayHRVScore.setTextColor(Color.rgb(0,204,0));
//            txtDisplayHRVScore.setTextColor(Color.GREEN);
        }else if(hrvValue>0.9*hrvStandard&&hrvValue<=1.05*hrvStandard){
            txtDisplayHRVScore.setTextColor(Color.rgb(102,255,102)); // orange
        }else if(hrvValue>0.5*hrvStandard&&hrvValue<=0.9*hrvStandard){
            txtDisplayHRVScore.setTextColor(Color.rgb(255,165,0));
        }else if(hrvValue<=0.5*hrvStandard){
            txtDisplayHRVScore.setTextColor(Color.rgb(255,0,0));
        }

        String hrvVal = String.format("%.1f", hrvValue);
        if(!hrvVal.equals("NaN")) {
            hrvScoreGlobal = hrvValue;
            txtDisplayHRVScore.setText(hrvVal);
        }

        double[] rrArray = new double[rrIntervalValues.size()];
        for(int i=0;i<rrIntervalValues.size();i++){
            rrArray[i] = rrIntervalValues.get(i);
        }



        HRVLibFacade hrvCalc = new HRVLibFacade(RRData.createFromRRInterval(rrArray, units.TimeUnit.SECOND));
        try {
            conditionAtrialFibrillationChances = 0;
            List<HRVParameter> hrvParameters = hrvCalc.calculateParameters();
            double lf = 0,hf = 0,sdNN = 0,pnn50 = 0,rmssd=0,streesIndex=0;
            double totalVal=0;
            for (int i = 0; i < hrvParameters.size(); i++) {
                HRVParameter para = hrvParameters.get(i);
                if(para.getName().equals("SDNN")){
                    sdNN = 1000*para.getValue();

                    if(sdNN<50){
                        conditionAtrialFibrillationChances++;
                    }

                    if(sdNN<=SDNN_1){
                        txtDisplaySDNN.setTextColor(Color.RED);
                    }else if(sdNN>SDNN_1&&sdNN<SDNN_2){
                        txtDisplaySDNN.setTextColor(Color.rgb(255,165,0));
                    }else if(sdNN>=SDNN_2){
//                        txtDisplaySDNN.setTextColor(Color.GREEN);
                        txtDisplaySDNN.setTextColor(Color.rgb(0,204,0));
                    }
                    txtDisplaySDNN.setText(String.format("%.1f", 1000*para.getValue()));
                    sdnnGlobal = 1000*para.getValue();
                }else if(para.getName().equals("PNN50")){
                    pnn50 = para.getValue();
                    if(pnn50<=PNN50_1){
                        txtDisplayPNN50.setTextColor(Color.RED);
                    }else if(pnn50>PNN50_1&&pnn50<PNN50_2){
                        txtDisplayPNN50.setTextColor(Color.rgb(255,165,0));
                    }else if(pnn50>=PNN50_2){
                        txtDisplayPNN50.setTextColor(Color.rgb(0,204,0));
                    }
                    txtDisplayPNN50.setText(String.format("%.1f", para.getValue()));
                    pnn50Global = para.getValue();
                }else if(para.getName().equals("SD1")){
                    sd1Global = para.getValue()*1000;
                }else if(para.getName().equals("SD2")){
                    sd2Global = para.getValue()*1000;
                }else if(para.getName().equals("RMSSD")){
                    rmssd = 1000*para.getValue();
                    if(rmssd<20){
                        conditionAtrialFibrillationChances++;
                    }
                    if(rmssd<=RMSSD_1){
                        txtDisplayRMSSD.setTextColor(Color.RED);
                    }else if(rmssd>RMSSD_1&&rmssd<RMSSD_2){
                        txtDisplayRMSSD.setTextColor(Color.rgb(255,165,0));
                    }else if(rmssd>=RMSSD_2){
                        txtDisplayRMSSD.setTextColor(Color.rgb(0,204,0));
                    }
                    txtDisplayRMSSD.setText(String.format("%.1f", 1000*para.getValue()));
                    rmssdGlobal = 1000*para.getValue();
                }else if(para.getName().equals("LNRMSSD")){
                }else if(para.getName().equals("NN50")) {
//                    txtDisplayNN50.setText(String.format("%.1f", para.getValue()));

                }else if(para.getName().equals("BAEVSKY")){
                    streesIndex = 10*para.getValue();
                    if(streesIndex>15){
                        txtDisplayStressIndex.setTextColor(Color.RED);
                    }else if(streesIndex>=10&&streesIndex<=15){
                        txtDisplayStressIndex.setTextColor(Color.rgb(255,165,0));
                    }else if(streesIndex<10){
                        txtDisplayStressIndex.setTextColor(Color.rgb(0,204,0));
                    }
                    txtDisplayStressIndex.setText(String.format("%.1f", 10*para.getValue()));
                    stressIndexGlobal = streesIndex;
                }else if(para.getName().equals("HF")){
                    hf = para.getValue();
                }else if(para.getName().equals("LF")){
                    lf = para.getValue();
                }

                Log.i("READ_Prakhar_HRVS__", para.getName() + " " + para.getUnit() + " " + para.getValue() + " " + para.getClass() + " " + para.getType()+" ");
            }
            int ratioIntLfHfNu = 0;
            if(hf!=0) {
                totalVal = lf + hf;
                double lfNu = lf / totalVal;
                double hfNu = hf / totalVal;
                double ratioLfHfNu = lfNu / hfNu;
                ratioLfHfNu = ratioLfHfNu * 100;
                ratioIntLfHfNu = (int) ratioLfHfNu;
                if(ratioIntLfHfNu>=200){
                    conditionAtrialFibrillationChances++;
                }
                Log.i("READ_Prakhar_HRVS__",ratioIntLfHfNu + " "+lf+" "+hf);
                if(ratioIntLfHfNu>=200){
                    txtDisplayLfByHf.setTextColor(Color.RED);
                }else if(ratioIntLfHfNu>=100){
                    txtDisplayLfByHf.setTextColor(Color.rgb(255,165,0));
                }else{
                    txtDisplayLfByHf.setTextColor(Color.rgb(0,204,0));
                }
                txtDisplayLfByHf.setText(String.valueOf(ratioIntLfHfNu));
                ratioLfHfGlobal = ratioIntLfHfNu;
            }else{
                ratioIntLfHfNu = 0;
                txtDisplayLfByHf.setTextColor(Color.rgb(0,204,0));
                txtDisplayLfByHf.setText(String.valueOf(ratioIntLfHfNu));
                ratioLfHfGlobal = ratioIntLfHfNu;
            }

            if(conditionAtrialFibrillationChances == 3){
//                Severe chnaces of atrial fibrillation
                txtDisplayAtrialFibrillationChances.setTextColor(Color.RED);
                txtDisplayAtrialFibrillationChances.setText("Severe chances of AF");
            }else if(conditionAtrialFibrillationChances == 2){
                // Moderate chnaces of atrial fibrillation
                txtDisplayAtrialFibrillationChances.setTextColor(Color.rgb(255,165,0));
                txtDisplayAtrialFibrillationChances.setText("Moderate chances of AF");
            }else if(conditionAtrialFibrillationChances == 1){
                //Slight chnaces of atrial fibrillation
                txtDisplayAtrialFibrillationChances.setTextColor(Color.rgb(255,165,0));
                txtDisplayAtrialFibrillationChances.setText("Slight chances of AF");
            }else if(conditionAtrialFibrillationChances == 0){
//                normal sinus rhythm
                txtDisplayAtrialFibrillationChances.setTextColor(Color.rgb(0,204,0));
                txtDisplayAtrialFibrillationChances.setText("Normal Sinus Rhythm");
            }
        }catch (Exception e){
            // may be artefacts wrong reading....
        }
    }

    private void addRRValue(double rrValue) {
        // Add new value to list

        Log.i("READ_Prakhar_HRVA","Artifacts : "+artifactCount+" RR Index :"+rrIndexCount+" "+rrIntervalValues.toString());

        int graphValue = (int) (rrValue * 1000); // rr value in sec

        // Adding in rr Interval  in milli second i.e. mis;
        rrIntervalValues.add(rrValue);

        rrMaxValue = (int) Math.max(rrMaxValue,graphValue);
        rrMinValue = (int) Math.min(rrMinValue,graphValue);
        txtDisplayRRMax.setText("Max :"+rrMaxValue);
        txtDisplayRRMin.setText("Min :"+rrMinValue);

        // Update statistics
        counter++;
        sum += rrValue;
        sumSquared += rrValue * rrValue;
//        mean = sum / rrIntervalValues.size();
        mean = sum / counter;
        txtDisplayRRAvg.setText("Avg :"+String.format("%.1f",mean*1000));
//        stdDev = Math.sqrt((sumSquared / rrIntervalValues.size()) - (mean * mean));

        stdDev = Math.sqrt((sumSquared / counter) - (mean * mean));


        // Also adding in graph data set
        if(!rrDataMapGraph.containsKey(graphValue)) {
            rrDataMapGraph.put(graphValue, 1);
        }else{
            int val = rrDataMapGraph.get(graphValue);
            val++;
            rrDataMapGraph.put(graphValue, val);
        }



        // Remove outliers beyond 3 sigma limit
        double lowerBound = mean - (3 * stdDev);
        double upperBound = mean + (3 * stdDev);

        float lowerBoundFloat = (float) lowerBound;
        float upperBoundFloat = (float) upperBound;
        float valFloat;

        Log.i("READ_Prakhar_Mean",lowerBound +" "+upperBound+" "+mean+" "+stdDev+" "+rrValue);

        for(int i=0;i<rrIntervalValues.size();i++){
            double val = rrIntervalValues.get(i);
            valFloat = (float) val;
            if(valFloat<lowerBoundFloat||valFloat>upperBoundFloat){
                artifactCount++;
                txtDisplayArtifact.setText("Artifacts: "+artifactCount);
                if(rrDataMapGraph.containsKey(val*1000)){
                    rrDataMapGraph.remove(val*1000);
                    // rrInterval Values in ms
                    // while rrDataMapGraph in second.
                    Log.i("READ_Prakhar_Mean_F","Removed");

                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // remove from intervals too....
            rrIntervalValues.removeIf(value -> value < lowerBound || value > upperBound);
        }
        Log.i("READ_Prakhar_HRVB","Artifacts : "+artifactCount+" RR Index :"+rrIndexCount+" "+rrIntervalValues.toString());
//        return  true;
        rrIndexCount = rrIntervalValues.size();
        txtDisplayRRIndex.setText("RR Values: "+rrIndexCount);
        plotGraph();
    }
    public void plotGraph() {
        barEntries = new ArrayList<>();
        barEntriesRed = new ArrayList<>();
        if(rrDataMapGraph.size()==0){
            combinedChart.setVisibility(View.INVISIBLE);
        }else {
            combinedChart.setVisibility(View.VISIBLE);
            for (Map.Entry<Integer, Integer> entry : rrDataMapGraph.entrySet()) {
//            String label = entry.getKey();
//            Float value = entry.getValue();
                int x_val = entry.getKey();
                int y_val = entry.getValue();
                yMaxRR = Math.max(y_val,yMaxRR);
                BarEntry barEntry = new BarEntry(x_val, y_val);
                barEntries.add(barEntry);
            }

            if(barEntriesRed.size()==0) {
                barEntriesRed.add(new BarEntry((float)(mean*1000),yMaxRR+1));
            }else{
                barEntriesRed.set(0,new BarEntry((float)(mean*1000),yMaxRR+1));
            }

            BarDataSet barDataSetRed = new BarDataSet(barEntriesRed,"bar_red");
            barDataSetRed.setColor(Color.RED);
            barDataSetRed.setDrawValues(false);

//            BarData barDataRed = new BarData(barDataSetRed);
//            barDataRed.setBarWidth(0.5f);

            BarDataSet barDataSet = new BarDataSet(barEntries, "Bar Chart");
            barDataSet.setColor(Color.rgb(66, 94, 246));
            barDataSet.setDrawValues(false);
            BarData barData = new BarData(barDataSet,barDataSetRed);
            barData.setBarWidth(2f);


//            lineEntries.set(0,new Entry(rrMinValue - 110,0));
//            lineEntries.set(1,new Entry((float)(mean*1000),yMaxRR+1));
//            lineEntries.set(2,new Entry(rrMaxValue + 110,0));


            // ***************************
            LineDataSet lineDataSet = new LineDataSet(lineEntries, "Line Chart");
            lineDataSet.setColor(Color.RED);
            lineDataSet.setLineWidth(2f);
            lineDataSet.setDrawValues(false);
            lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

            CombinedData combinedData = new CombinedData();
            combinedData.setData(barData);
//            combinedData.setData(barDataRed);
            combinedData.setData(new LineData(lineDataSet));
            combinedChart.setData(combinedData);
            XAxis xAxisCombine = combinedChart.getXAxis();
            xAxisCombine.setAxisMinimum(rrMinValue - 100);
            xAxisCombine.setAxisMaximum(rrMaxValue + 100);

            combinedChart.getDescription().setEnabled(false);
            combinedChart.setDrawOrder(new CombinedChart.DrawOrder[]{
                    CombinedChart.DrawOrder.BAR, CombinedChart.DrawOrder.LINE
            });
            combinedChart.getAxisRight().setEnabled(false);
            combinedChart.getXAxis().setGranularity(25f);
            combinedChart.getAxisLeft().setGranularity(1f);
            combinedChart.getAxisLeft().setValueFormatter(new IAxisValueFormatter() {
                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    return "";
                }
            });
            combinedChart.getXAxis().setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
            combinedChart.getAxisLeft().setDrawGridLines(true);
            Legend legend = combinedChart.getLegend();
            legend.setEnabled(false);
            combinedChart.notifyDataSetChanged();
            combinedChart.invalidate();
        }
    }
    private void performAfterTest() {
        Intent i = new Intent(getApplicationContext(),HRVResult_MeasureHRV.class);
        i.putExtra("heartRate",hrGlobal);
        i.putExtra("hrv",hrvScoreGlobal);
        i.putExtra("sdnn",sdnnGlobal);
        i.putExtra("rmssd",rmssdGlobal);
        i.putExtra("lfHfR",ratioLfHfGlobal);
        i.putExtra("pnn50",pnn50Global);
        i.putExtra("stressIndex",stressIndexGlobal);
        i.putExtra("sd1",sd1Global);
        i.putExtra("sd2",sd2Global);

        // conditions check.........

        if(sd1Global<52&&sd2Global<64){
//            Get yourself checked for Diabetes
            i.putExtra("Diabetes",true);
        }else{
            i.putExtra("Diabetes",false);
        }

        if((sd1Global<52||sd2Global<64)&&(ratioLfHfGlobal>=200||pnn50Global<pnnStandard)){
            // Get yourself checked for Congestive heart Analysis
            i.putExtra("CongestiveHeartAnalysis",true);
        }else{
            i.putExtra("CongestiveHeartAnalysis",false);
        }

        if(sdnnGlobal<50&&rmssdGlobal<20){
            // TMT test suggested
            i.putExtra("TMTTestSuggested",true);
        }else{
            i.putExtra("TMTTestSuggested",false);
        }

        if((sdnnGlobal<50&&rmssdGlobal<20)&&(ratioLfHfGlobal>=200||pnn50Global<pnnStandard)){
            // Normal Sinus Rhythm
            i.putExtra("Arrhythmia",true);
        }else{
            i.putExtra("Arrhythmia",false);
        }

        if(conditionAtrialFibrillationChances == 0){
//                normal sinus rhythm
            i.putExtra("NormalSinusRhythm",true);
        }else{
            i.putExtra("NormalSinusRhythm",false);
        }
        // ............................

        startActivity(i);
    }
}