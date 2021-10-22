package br.ndz.float_button_overlay.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import java.io.InputStream;
import java.net.URL;

public class AsyncBitmapDownload extends AsyncTask<String, Integer, Bitmap> {

    public AsyncResponse delegate = null;

    public AsyncBitmapDownload(AsyncResponse asyncResponse){
        delegate = asyncResponse;
    }

    @Override
    protected Bitmap doInBackground(String... urls) {
        String imageURL = urls[0];
        Bitmap bimage = null;
        try {
            InputStream in = new java.net.URL(imageURL).openStream();
            bimage = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Log.e("Error Message", e.getMessage());
            e.printStackTrace();
        }
        return bimage;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);
        delegate.processFinish(bitmap);
    }
}