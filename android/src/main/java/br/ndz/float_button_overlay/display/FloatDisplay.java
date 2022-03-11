package br.ndz.float_button_overlay.display;

import static android.content.Context.WINDOW_SERVICE;
import static br.ndz.float_button_overlay.utils.Constants.BROADCAST_FINISH_APP;
import static br.ndz.float_button_overlay.utils.Constants.CHANNEL;
import static br.ndz.float_button_overlay.utils.Constants.MAX_CLICK_DURATION;
import static br.ndz.float_button_overlay.utils.Constants.TAG;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.skyfishjy.library.RippleBackground;

import java.util.Calendar;

import br.ndz.float_button_overlay.FloatButtonOverlayPlugin;
import br.ndz.float_button_overlay.R;
import br.ndz.float_button_overlay.services.FloatButtonService;
import de.hdodenhof.circleimageview.CircleImageView;

public class FloatDisplay {
    private long startClickTime;
    private WindowManager mWindowManager;
    private WindowManager mWindowManager2;
    private ImageView imageView;
    private RelativeLayout mFloatingWidget;
    private LinearLayout mFloatingWidget2;
    private TextView estimatedPriceWidget;
    private TextView durationWidget;
    private TextView distanceWidget;
    private TextView fromTextWidget;
    private TextView toTextWidget;
    private CircleImageView passengerProfileImageWidget;
    private RippleBackground rippleBackground;
    private WindowManager.LayoutParams params;
    private WindowManager.LayoutParams params2;
    private String iconPath;
    private String notificatitionTitle;
    private String notificatitionText;
    private boolean showTransparentCircle;
    private String wsRoom;
    private String wsUrl;
    private String driverId;
    private String recipientId;
    private String driverImageProfileUrl;
    private String driverName;
    private String driverPlate;
    private String driverCarModel;
    private String driverRateValue;
    private String acceptUrl;
    private String driverPositionUrl;
    private String packageName;
    private String activityName;
    private int iconWidth;
    private int iconHeight;
    private int transpCircleWidth;
    private int transpCircleHeight;
    Display display;
    Point floatButtonsize;
    private int maxY = 0;
    private int endArea = 0;
    private Context context;
    private boolean isButtonDisplayed = false;

    public FloatDisplay(Context context){

        this.context = context;

        mFloatingWidget = (RelativeLayout) LayoutInflater.from(context).inflate(R.layout.button_activity, null);
        mFloatingWidget.findViewById(R.id.button1);

        mFloatingWidget2 = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.notification_layout, null);
        mFloatingWidget2.findViewById(R.id.button2);

        estimatedPriceWidget = (TextView) mFloatingWidget2.findViewById(R.id.estimated_price);
        durationWidget = (TextView) mFloatingWidget2.findViewById(R.id.duration);
        distanceWidget = (TextView) mFloatingWidget2.findViewById(R.id.distance);
        fromTextWidget = (TextView) mFloatingWidget2.findViewById(R.id.from_text);
        toTextWidget = (TextView) mFloatingWidget2.findViewById(R.id.to_text);
        passengerProfileImageWidget = (CircleImageView) mFloatingWidget2.findViewById(R.id.profile_image);

        rippleBackground = (RippleBackground) mFloatingWidget2.findViewById(R.id.content);

    }

    private void animateButton(float scaleX, float scaleY) {
        imageView.animate()
                .setStartDelay(1000) //prevent the animation from starting
                .scaleY(scaleY)
                .scaleX(scaleX)
                .setDuration((long) 100f)
                .setStartDelay(0)
                .start();
    }

    public void closeBubble(){
        try {
            if (mFloatingWidget != null) mWindowManager.removeView(mFloatingWidget);
            isButtonDisplayed = false;
        }catch (Exception e){

        }
    }


    public void openBubble(Intent intent){
        try {

            setIntentValues(intent);

            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                            : WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

            params.gravity = Gravity.TOP | Gravity.LEFT;
            params.x = 0;
            params.y = 100;

            mWindowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(iconWidth, iconHeight);
            lp.addRule(RelativeLayout.CENTER_IN_PARENT);
            imageView = new ImageView(context);
            imageView.setLayoutParams(lp);
            imageView.setImageBitmap(BitmapFactory.decodeFile(iconPath));
            mFloatingWidget.addView(imageView);

            ShapeDrawable shapeBg = new ShapeDrawable(new OvalShape());
            shapeBg.setIntrinsicHeight(transpCircleHeight);
            shapeBg.setIntrinsicWidth(transpCircleWidth);
            shapeBg.setAlpha(10);

            if (!showTransparentCircle) {
                mFloatingWidget.setBackgroundResource(R.drawable.null_selector);
            } else {
                mFloatingWidget.setBackground(shapeBg);
            }

            mWindowManager.addView(mFloatingWidget, params);

            /**
             * Setting close area height.
             * The close area corresponds to screen height - 20%
             *
             * */
            display = mWindowManager.getDefaultDisplay();
            floatButtonsize = new Point();
            display.getSize(floatButtonsize);
            maxY = floatButtonsize.y;
            endArea = maxY - (int) (maxY * 0.20);
            
            //FloatButtonOverlayPlugin.invokeCallBack("callback", null);
            isButtonDisplayed = true;
            bubbleClick();

        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
        }
    }

    public boolean isOpen(){
        return isButtonDisplayed;
    }

    private void setIntentValues(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            Log.i(TAG, extras.toString());
            packageName = extras.getString("packageName");
            activityName = extras.getString("activityName");
            iconPath = extras.getString("iconPath");
            notificatitionText = extras.getString("notificationText");
            notificatitionTitle = extras.getString("notificationTitle");
            iconWidth = extras.getInt("iconWidth");
            iconHeight = extras.getInt("iconHeight");
            transpCircleWidth = extras.getInt("transpCircleWidth");
            transpCircleHeight = extras.getInt("transpCircleHeight");
            showTransparentCircle = extras.getBoolean("showTransparentCircle");
            wsRoom = extras.getString("wsRoom");
            wsUrl = extras.getString("wsUrl");
            driverId = extras.getString("wsRoom");
            recipientId = extras.getString("recipientId");
            driverImageProfileUrl = extras.getString("driverImageProfileUrl");
            driverName = extras.getString("driverName");
            acceptUrl = extras.getString("acceptUrl");
            driverPositionUrl = extras.getString("driverPositionUrl");
            driverPlate = extras.getString("driverPlate");
            driverCarModel = extras.getString("driverCarModel");
            driverRateValue = extras.getString("driverRateValue");

            Log.i(TAG, extras.toString());
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void bubbleClick(){
        mFloatingWidget.setOnTouchListener(new View.OnTouchListener() {

            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private int initialWidth;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int initialHeight = mFloatingWidget.getHeight();
                initialWidth = mFloatingWidget.getWidth();

                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:

                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();

                        animateButton(1.1f, 1.1f);

                        //Getting press time
                        startClickTime = Calendar.getInstance().getTimeInMillis();
                        return false;

                    case MotionEvent.ACTION_UP:

                        int Xdiff = (int) (event.getRawX() - initialTouchX);
                        int Ydiff = (int) (event.getRawY() - initialTouchY);

                        animateButton(1f, 1f);

                        if (params.y >= endArea) {
                            params.alpha = 1;

                            final Handler handler = new Handler();
                            handler.post(new Runnable() {
                                @Override
                                public void run() {

                                    Log.i(TAG, "Runnable: Alpha Decrease" + params.alpha);
                                    if (params.alpha > 0) {
                                        params.alpha = (float) (params.alpha - 0.06);
                                        params.x = params.x + 50;
                                        try {
                                            mWindowManager.updateViewLayout(mFloatingWidget, params);
                                        } catch (Exception e) {
                                            Log.i(TAG, "An error has occurred: " + e.getMessage() + "StackTrace: " + e.getStackTrace());
                                        }

                                        Log.i(TAG, "Alpha Decrease" + params.alpha);
                                        handler.postDelayed(this, 1);
                                    }
                                    Log.i(TAG, "Runnable: Finished");

                                    if (params.alpha <= 0) {

                                        SendBroadcastToFinishApp();

                                        Log.i(TAG, "Stopping service");
                                        final Intent i = new Intent(context, FloatButtonService.class);
                                        context.stopService(i);
                                        //stopForeground(true);
                                        android.os.Process.killProcess(android.os.Process.myPid());
                                    }

                                }
                            });
                        }

                        /**
                         * Starts app on click in Float Button
                         * */
                        long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                        if (clickDuration < MAX_CLICK_DURATION) {
                            if (packageName != null && !packageName.isEmpty()) {
                                StartAppIntent(packageName, activityName);
                            }

                            Log.i(TAG, "Will click on channel " + CHANNEL);
                            FloatButtonOverlayPlugin.invokeCallBack("onClickCallback", null);
                            Log.i(TAG, "Clicked");
                        }

                        return false;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        mWindowManager.updateViewLayout(mFloatingWidget, params);
                        return false;
                }
                return false;
            }
        });
    }

    private void SendBroadcastToFinishApp() {

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
        localBroadcastManager.sendBroadcast(new Intent(BROADCAST_FINISH_APP));

    }

    private void StartAppIntent(String packageName, String activityName) {

        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent;

        if (Build.VERSION.SDK_INT >= 31) {
            pendingIntent =
                    PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        }else{
            pendingIntent =
                    PendingIntent.getActivity(context, 0, intent, 0);
        }
        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }

    }


}
