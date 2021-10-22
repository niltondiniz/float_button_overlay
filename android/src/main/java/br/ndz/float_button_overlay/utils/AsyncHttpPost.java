package br.ndz.float_button_overlay.utils;

import static br.ndz.float_button_overlay.utils.Constants.TAG;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AsyncHttpPost extends AsyncTask<String, String, String> {

    JSONObject localData;
    String localUrl;
    OkHttpClient client = new OkHttpClient();
    public String json;
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");


    public String doPostRequest() throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(localUrl)
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    public AsyncHttpPost(JSONObject data, String url){
        localData = data;
        localUrl = url;
        json = localData.toString();
    }

    @Override
    protected String doInBackground(String... params) {

        try {

            String response = doPostRequest();
            return response;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(String resultData) {
        super.onPostExecute(resultData);
        Log.i(TAG, "Driver position response: " + resultData);
    }
}