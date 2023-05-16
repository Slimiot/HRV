package com.slimiot.hrv.Config;

import java.util.HashMap;
import java.util.Map;

public class Messages {

    public static final String appLanguage = "English";
    public static final Map<String, String> englishMessages = new HashMap<String, String>();

    // constructor
    public Messages() {
        englishMessages.put("Wait_Text", "Please Wait ...");
    }

    public static String getMessage(String key) {
        String message = "";
        if(appLanguage.equals("English")) {
            message = englishMessages.get(key);
        }
        return message;
    }

    public static final String Wait_Text = "Please Wait ...";
    public static final String Error_Not_Connected = "Please check your internet connection!";
    public static final String Error_Text = "Something went wrong, Please try again!";
    public static final String Unauthorized_Access = "Unauthorized Access!";
    public static final String Session_Expired = "Your session is expired, please login again!";
    public static final String No_Data_Available = "No data available!";
    public static final String InvalidItem = "Requested item does not exist!";

    public static final String Invalid_Email = "Please enter valid email address!";
    public static final String Invalid_Mobile = "Please enter valid mobile number!";
    public static final String Login_Success = "Login Successful!";
    public static final String Login_Invalid_Error = "Invalid Email or Password!";
    public static final String Login_Inactive_Error = "Your account is not active!";
    public static final String Logout_Text  = "Logout";

    public static final String Json_Data = "";

}
