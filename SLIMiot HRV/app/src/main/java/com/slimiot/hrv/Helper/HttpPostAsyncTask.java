package com.slimiot.hrv.Helper;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;


import com.slimiot.hrv.Config.Messages;
import com.slimiot.hrv.OnTaskCompleted;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class HttpPostAsyncTask extends AsyncTask<String, Void, String> {

    JSONObject postData;
    private OnTaskCompleted taskCompleted;
    Context mContext;
    String apiResponse;
    String apiName;
    boolean showProgress;
    String TAG = "HttpPostAsyncTask";

    // This is a constructor that allows you to pass in the JSON body
    public HttpPostAsyncTask(
            Map<String, String> postData,
            OnTaskCompleted activityContext,
            Context context,
            String apiName,
            boolean showProgress) {

        if (postData != null) {
            this.postData = new JSONObject(postData);
        }
        this.taskCompleted = activityContext;
        this.mContext = context;
        this.apiName = apiName;
        this.showProgress = showProgress;

    }

    @Override
    protected String doInBackground(String... params) {

        try {
            // This is getting the url from the string we passed in
            URL url = new URL(params[0]);

            // Create the urlConnection
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setConnectTimeout(30000);
            urlConnection.setRequestMethod("POST");

            // OPTIONAL - Sets an authorization header
            //urlConnection.setRequestProperty("Authorization", "someAuthString");

            // Send the post body
            if (this.postData != null) {
                OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream());
                writer.write(postData.toString());
                writer.flush();
            }

            try {
                int statusCode = urlConnection.getResponseCode();
                Log.i("statusCode", statusCode+"");
                if (statusCode == 500 || statusCode == 404) {
                    InputStream errorStream = new BufferedInputStream(urlConnection.getErrorStream());
                    String errorResponse = Format.convertInputStreamToString(errorStream);
                    Log.i("errorResponse", errorResponse);
                } else if (statusCode ==  200) {
                    InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                    this.apiResponse = Format.convertInputStreamToString(inputStream);
                    Log.i("apiResponse", this.apiName + ":" + this.apiResponse);
                } else {
                    General.showToast(mContext, Messages.getMessage("Unauthorized_Access"));
                }
            } catch (IOException ioe) {
                Log.i("IOException", ioe.getLocalizedMessage());
                ioe.printStackTrace();
            } finally {
                if(this.showProgress) {
                    General.stopProgressDialog();
                }
            }

        } catch (Exception e) {
            Log.d("Exception", e.getLocalizedMessage());
        }
        return null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if(this.showProgress) {
            General.startProgressDialog(mContext);
        }
    }

    @Override
    protected void onPostExecute(String response) {
        if(this.showProgress) {
            General.stopProgressDialog();
        }
        taskCompleted.onTaskCompleted(this.apiResponse, this.apiName);
    }

} // end class
