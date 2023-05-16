package com.slimiot.hrv.Home;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.slimiot.hrv.R;

import java.text.SimpleDateFormat;
import java.util.Date;

public class HRVResult_MeasureHRV extends AppCompatActivity {

    private int hr=0,ratioLfHf=0;
    private double sdnn=0,rmssd=0,pnn50=0,sd1=0,sd2=0,hrvScore=0,stressIndex=0;

    private boolean normalSinusRhythm,arrhythmia,congestiveHeartAnalysis,diabetes,tmt;
    private TextView txtDisplayHr,txtDisplayHrv,txtDisplayDate,txtDisplayTime,txtDisplaySDNN;
    private TextView txtDisplayRMSSD,txtDisplayLfHf,txtDisplayPNN,txtDisplayStress,txtDisplaySD1;
    private TextView txtDisplaySD2,txtDisplayNSR,txtDisplayAr,txtDisplayCha,txtDisplayDia,txtDisplayTMT;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hrvresult_measure_hrv);
        getSupportActionBar().hide();

        txtDisplayHr = findViewById(R.id.txtDisplayHrReport);
        txtDisplayHrv = findViewById(R.id.txtDisplayHrvReport);
        txtDisplayDate = findViewById(R.id.txtDisplayDateReport);
        txtDisplayTime = findViewById(R.id.txtDisplayTimeReport);
        txtDisplaySDNN = findViewById(R.id.txtDisplaySDNNReport);
        txtDisplayRMSSD = findViewById(R.id.txtDisplayRMSSDReport);
        txtDisplayLfHf = findViewById(R.id.txtDisplayLfHfReport);
        txtDisplayPNN = findViewById(R.id.txtDisplayPNN50Report);
        txtDisplayStress = findViewById(R.id.txtDisplayStressIndexReport);
        txtDisplaySD1 = findViewById(R.id.txtDisplaySD1Report);
        txtDisplaySD2 = findViewById(R.id.txtDisplaySD2Report);
        txtDisplayNSR = findViewById(R.id.txtDisplayNSRReport);
        txtDisplayAr = findViewById(R.id.txtDisplayArReport);
        txtDisplayCha = findViewById(R.id.txtDisplayChaReport);
        txtDisplayDia = findViewById(R.id.txtDisplayDiaReport);
        txtDisplayTMT = findViewById(R.id.txtDisplayTMTReport);

        hr = getIntent().getIntExtra("heartRate",0);
        txtDisplayHr.setText(String.valueOf(hr));
        ratioLfHf = getIntent().getIntExtra("lfHfR",0);
        txtDisplayLfHf.setText(String.valueOf(ratioLfHf));
        sdnn = getIntent().getDoubleExtra("sdnn",0);
        txtDisplaySDNN.setText(String.format("%.1f", sdnn));
//        rmssd = getIntent().getDoubleExtra("rmssd",0);
//        txtDisplayRMSSD.setText(String.format("%.1f", rmssd));
        pnn50 = getIntent().getDoubleExtra("pnn50",0);
        txtDisplayPNN.setText(String.format("%.1f", pnn50));
        sd1 = getIntent().getDoubleExtra("sd1",0);
        txtDisplaySD1.setText(String.format("%.1f", sd1));
        sd2 = getIntent().getDoubleExtra("sd2",0);
        txtDisplaySD2.setText(String.format("%.1f", sd2));
        hrvScore = getIntent().getDoubleExtra("hrv",0);
        txtDisplayHrv.setText(String.format("%.1f", hrvScore));
        stressIndex = getIntent().getDoubleExtra("stressIndex",0);
        txtDisplayStress.setText(String.format("%.1f", stressIndex));
        rmssd = getIntent().getDoubleExtra("rmssd",0);
        txtDisplayRMSSD.setText(String.format("%.1f", rmssd));
        normalSinusRhythm = getIntent().getBooleanExtra("NormalSinusRhythm",false);
        if(normalSinusRhythm){
            txtDisplayNSR.setText(String.valueOf("Yes"));
        }else{
            txtDisplayNSR.setText(String.valueOf("No"));
        }
        arrhythmia = getIntent().getBooleanExtra("Arrhythmia",false);
        if(arrhythmia){
            txtDisplayAr.setText(String.valueOf("Yes"));
        }else{
            txtDisplayAr.setText(String.valueOf("No"));
        }
        congestiveHeartAnalysis = getIntent().getBooleanExtra("CongestiveHeartAnalysis",false);
        if(congestiveHeartAnalysis){
            txtDisplayCha.setText(String.valueOf("Yes"));
        }else{
            txtDisplayCha.setText(String.valueOf("No"));
        }
        diabetes = getIntent().getBooleanExtra("Diabetes",false);
        if(diabetes) {
            txtDisplayDia.setText(String.valueOf("Yes"));
        }else{
            txtDisplayDia.setText(String.valueOf("No"));
        }
        tmt = getIntent().getBooleanExtra("TMTTestSuggested",false);
        if(tmt){
            txtDisplayTMT.setText(String.valueOf("Yes"));
        }else{
            txtDisplayTMT.setText(String.valueOf("No"));
        }

        // Date & time

        // Get current timestamp
        long currentTimeStamp = System.currentTimeMillis();

// Convert timestamp to Date object
        Date currentDate = new Date(currentTimeStamp);

// Format date and time
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String formattedDate = dateFormat.format(currentDate);
        String[] disDateT = formattedDate.split(" ");
        txtDisplayDate.setText(disDateT[0]);
        txtDisplayTime.setText(disDateT[1]);
// Display date and time
//        System.out.println("Current date and time: " + formattedDate);

    }
    public void goBack(View view) {
        finish();
    }
    @Override
    public void onPause() {
        super.onPause();
        overridePendingTransition(0, 0);
    }
}