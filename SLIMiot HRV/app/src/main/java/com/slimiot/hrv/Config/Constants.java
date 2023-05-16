package com.slimiot.hrv.Config;

public class Constants {

    // App
    public static final String App_Name = "Slim IOT Health";
    public static final String Current_Version = "1.0.0";
    public static final String Helpline_Number = "+918742934520";
    public static final Long Default_Expire_Time = new Long(3600);
    public static final int Max_Allowed_Product_In_Cart = 100;

    public static final int Max_Allowed_Image_Selection = 4;
    public static final int Max_Allowed_Video_Selection = 1;
    public static final int String_Trim_Limit = 15;

    public static final boolean Disable_Screen_Shot = false;

    public static final int Splash_Screen_Timeout = 1000;
    public static final long INVALID_DATE = Long.MIN_VALUE;
    public static final int PLOT_MAXIMUM_AGE = 300000; // Maximum item age for real-time plot, in ms
    public static final String DEFAULT_CURRENCY_SYMBOL = "₹";

    public static final String APP_ENVIRONMENT = "TEST"; // "TEST" or "PROD"

    /* Live Server API */
    public static final String API_BASE_URL = "https://slimiot.com/api/";

    /* QA Server API */
    //public static final String API_BASE_URL = "https://slimiot.com/api/";

    /* Localhost API */
    //public static final String API_BASE_URL = "";

    // TEST
    //public static final String Razorpay_KEY_ID = "rzp_test_xyz";
    //public static final String Razorpay_KEY_SECRET = "rzp_test_xyz";

    // PROD
    public static final String Razorpay_KEY_ID = "rzp_live_xyz";
    public static final String Razorpay_KEY_SECRET = "rzp_live_xyz";

    public static final String Facebook_App_ID = "1234567890";

    public static final String API_KEY = "";
    public static final String API_Secret = "";

    // SMS API
    public static final String SMS_API = "";

    // Firebase
    public static final String CHANNEL_ID = "my_channel_01";
    public static final String CHANNEL_NAME = "App Notification";
    public static final String CHANNEL_DESCRIPTION = "CHANNEL_DESCRIPTION";

    // Storage
    public static String InternalStorageDirectory = "InternalStorageDirectory";
    public static String InternalStorageDirectoryName = "InternalStorageDirectoryName";

    public static String UploadDirectoryName = "DataEntry";

    // Other Text
    public static final String COLOR_SUCCESS = "#2B910F";
    public static final String COLOR_DANGER = "#CC0033";
    public static final String COLOR_PRIMARY = "#ED1578";
    public static final String COLOR_WHITE = "#FFFFFF";
    public static final String COLOR_LIGHT_GREY = "#CCCCCC";

    public static final String descriptorClientConfiguration = "00002902-0000-1000-8000-00805f9b34fb"; // Client Characteristic Configuration

    public static final String servicePowerOn = "00000000-0000-1000-8000-00805f9b34fb"; // Power On
    public static final String characteristicPowerOn = "00000000-0000-1000-8000-00805f9b34fb"; // Power On

    public static final String servicePPG = "00001822-0000-1000-8000-00805f9b34fb"; // PPG
    public static final String characteristicPPG = "00002a5f-0000-1000-8000-00805f9b34fb"; // PLX Continuous Measurement

    public static final String serviceECG = "00001890-0000-1000-8000-00805f9b34fb"; // ECG
//    public static final String serviceECG = "6E400000-B5A3-F393-E0A9-E50E24DCCA9E" ; // ECG update 28-1-23 patch
    public static final String characteristicECG = "00001901-0000-1000-8000-00805f9b34fb"; // ECG

//    public static final String characteristicECG = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E" ; //ECG update 28-1-23 patch

    public static final String serviceHTS = "00001809-0000-1000-8000-00805f9b34fb"; // HTS
    public static final String characteristicHTS = "00002a1c-0000-1000-8000-00805f9b34fb"; // THS Temperature

    public static final String serviceHeartRate = "0000180d-0000-1000-8000-00805f9b34fb"; // Heart Rate
    public static final String characteristicHeartRate = "00002a37-0000-1000-8000-00805f9b34fb"; // Heart Rate

    public static final String serviceBatteryLevel = "0000180f-0000-1000-8000-00805f9b34fb"; // Battery Level
    public static final String characteristicBatteryLevel = "00002a19-0000-1000-8000-00805f9b34fb"; // Battery Level

    public static final String serviceBarometer = "0000181a-0000-1000-8000-00805f9b34fb"; // Barometer / Environmental Sensing
    public static final String characteristicBaroPressure = "00002a6d-0000-1000-8000-00805f9b34fb"; // Pressure
    public static final String characteristicBaroTemperature = "00002a6e-0000-1000-8000-00805f9b34fb"; // Temperature

    public static final String serviceIMU = "00001816-0000-1000-8000-00805f9b34fb";

    public static final String characteristicIMU = "00002713-0000-1000-8000-00805f9b34fb";
    public static final String serviceTimeStamp = "00001805-0000-1000-8000-00805f9b34fb";
    public static final String characteristicTimeStamp = "00002A0A-0000-1000-8000-00805f9b34fb";




    public static final String[] filterServicesUUID = new String[] {

            "00001800-0000-1000-8000-00805f9b34fb", // Generic Access
            "00001801-0000-1000-8000-00805f9b34fb", // Generic Attribute
            "0000181a-0000-1000-8000-00805f9b34fb", // Environmental Sensing
            "00001890-0000-1000-8000-00805f9b34fb", // ECG
            "00001809-0000-1000-8000-00805f9b34fb", // HTS / Health Thermometer
            "00001822-0000-1000-8000-00805f9b34fb", // PPG / HR & SPO2
            "0000180d-0000-1000-8000-00805f9b34fb", // Heart Rate
            "0000180f-0000-1000-8000-00805f9b34fb", // Battery Service

    };

    public static final String TEXT_Home = "Home";
    public static final String TEXT_Bluetooth = "BLE Scan";
    public static final String TEXT_Category = "Category";
    public static final String TEXT_User = "My Account";
    public static final String TEXT_Action = "Action";
    public static final String TEXT_Browse = "Browse";
    public static final String TEXT_Profile = "Profile";
    public static final String TEXT_More = "More";

}
