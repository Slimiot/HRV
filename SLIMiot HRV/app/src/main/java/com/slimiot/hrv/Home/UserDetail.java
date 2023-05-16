package com.slimiot.hrv.Home;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.slimiot.hrv.BleAction.BluetoothConnection;
import com.slimiot.hrv.BleOperations.BleActivities;
import com.slimiot.hrv.R;

import java.util.Calendar;

public class UserDetail extends AppCompatActivity {

    private DatePickerDialog datePickerDialog;
    private Button dateButton,nextButton;
    private MaterialAutoCompleteTextView gender;
    private TextInputLayout genderLayout;
    private String genderValue = null , dobValue=null ;
    private SharedPreferences sharedPreferences;

    String[] spinValues;
    Spinner spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_detail);
        getSupportActionBar().hide();
        initDatePicker();
        dateButton = findViewById(R.id.datePickerButton);
        nextButton = findViewById(R.id.nextButton);
        genderLayout=findViewById(R.id.genderLayout);
        gender=findViewById(R.id.genderProfile);
        spinValues = new String[]{"Hr & Hrv (180D)", "Timestamp 4byte + Hr Hrv"};
        spinner = findViewById(R.id.dataFormatSpinner);
        sharedPreferences = this.getSharedPreferences("com.slimiot.hrv",MODE_PRIVATE);
        getData();
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hrvAnalysis();
            }
        });
        genderLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                workOnGender();
            }
        });
        gender.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                workOnGender();
            }
        });
        genderLayout.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(mContext, "Hello", Toast.LENGTH_SHORT).show();
                workOnGender();
                // do some code
            }
        });
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
//                Toast.makeText(mContext, spinValues[i], Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        ArrayAdapter ad = new ArrayAdapter(this,android.R.layout.simple_spinner_item,spinValues);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(ad);
    }

    private void getData(){
        genderValue = sharedPreferences.getString("gender",null);
        dobValue = sharedPreferences.getString("dob",null);
        if(genderValue!=null){
            gender.setText(genderValue);
        }
        if(dobValue!=null){
            dateButton.setText(dobValue);
        }
//        sharedPreferences.edit().putString("gender",genderValue).apply();
//        sharedPreferences.edit().putString("dob",dobValue).apply();
    }

    private void hrvAnalysis() {
        String genderValue = gender.getText().toString();
        String dobValue = dateButton.getText().toString();
//        Toast.makeText(getApplicationContext(), genderValue , Toast.LENGTH_SHORT).show();

        if(genderValue==null||genderValue.equals("")){
            Toast.makeText(this, "Please fill the required fields", Toast.LENGTH_SHORT).show();
        }else{

            String spinValue = spinner.getSelectedItem().toString();
            if(spinValue.equals("Hr & Hrv (180D)")){
                BleActivities.k = 0;
            }else if(spinValue.equals("Timestamp 4byte + Hr Hrv")){
                BleActivities.k = 4;
            }


            sharedPreferences.edit().putString("gender",genderValue).apply();
            sharedPreferences.edit().putString("dob",dobValue).apply();
            startActivity(new Intent(getApplicationContext(), BluetoothConnection.class));
        }
    }

    private void workOnGender() {
        final AlertDialog.Builder d = new AlertDialog.Builder(UserDetail.this);
        LayoutInflater inflater = UserDetail.this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_gender, null);
        d.setView(dialogView);
        final RadioButton male = dialogView.findViewById(R.id.maleRadio);
        final RadioButton female = dialogView.findViewById(R.id.femaleRadio);
        final RadioButton other = dialogView.findViewById(R.id.otherRadio);
        final TextView ok = dialogView.findViewById(R.id.okGender);
        final TextView cancel =dialogView.findViewById(R.id.cancelGender);
        final RadioGroup radioGroup = dialogView.findViewById(R.id.genderRadioGroup);
        if(genderValue!=null){
            if(genderValue.equals("Male")){
                male.setChecked(true);
            }else if(genderValue.equals("Female")){
                female.setChecked(true);
            }else if(genderValue.equals("Other")){
                other.setChecked(true);
            }
        }
        AlertDialog alertDialog = d.create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertDialog.getWindow().setLayout(600, 400);
        alertDialog.show();
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int selectedId = radioGroup.getCheckedRadioButtonId();
                switch (selectedId){
                    case R.id.maleRadio: gender.setText("Male");
                        break;
                    case R.id.femaleRadio: gender.setText("Female");
                        break;
                    case R.id.otherRadio: gender.setText("Other");
                        break;
                }
                gender.clearFocus();
                alertDialog.dismiss();
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gender.clearFocus();
                alertDialog.dismiss();
            }
        });
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                genderLayout.clearFocus();
            }
        });
    }


    public void openDatePicker(View view)
    {
        datePickerDialog.show();
    }

    private String getTodaysDate()
    {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        month = month + 1;
        year=year-7;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        return makeDateString(day, month, year);
    }

    private void initDatePicker()
    {
        DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener()
        {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day)
            {
                month = month + 1;
                String date = makeDateString(day, month, year);
                dateButton.setText(date);
            }
        };

        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
//        year =Integer.parseInt(bdayYear);
//        month=dobM;
//        day=Integer.parseInt(bdayDate);
        int style = AlertDialog.THEME_HOLO_LIGHT;


        datePickerDialog = new DatePickerDialog(this, style, dateSetListener, year, month, day);
        long millis = System.currentTimeMillis() - 365L *5 * 24 * 60 * 60 * 1000;
        datePickerDialog.getDatePicker().setMaxDate(millis);
        Log.i("timecurrent",String.valueOf(System.currentTimeMillis()));

    }

    private String makeDateString(int day, int month, int year)
    {
        return getMonthFormat(month) + " " + day + " " + year;
    }

    private String getMonthFormat(int month)
    {
        if(month == 1)
            return "JAN";
        if(month == 2)
            return "FEB";
        if(month == 3)
            return "MAR";
        if(month == 4)
            return "APR";
        if(month == 5)
            return "MAY";
        if(month == 6)
            return "JUN";
        if(month == 7)
            return "JUL";
        if(month == 8)
            return "AUG";
        if(month == 9)
            return "SEP";
        if(month == 10)
            return "OCT";
        if(month == 11)
            return "NOV";
        if(month == 12)
            return "DEC";

        //default should never happen
        return "JAN";
    }
    @Override
    public void onPause() {
        super.onPause();
        overridePendingTransition(0, 0);
    }
}