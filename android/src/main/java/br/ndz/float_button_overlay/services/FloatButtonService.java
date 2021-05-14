package br.ndz.float_button_overlay.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.Shape;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import br.ndz.float_button_overlay.R;
import br.ndz.float_button_overlay.FloatButtonOverlayPlugin;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.dart.DartExecutor;
import io.flutter.plugin.common.MethodChannel;

import static br.ndz.float_button_overlay.utils.Constants.BROADCAST_FINISH_APP;
import static br.ndz.float_button_overlay.utils.Constants.BROADCAST_STATE_HIDE_FLOAT;
import static br.ndz.float_button_overlay.utils.Constants.BROADCAST_STATE_SHOW_FLOAT;
import static br.ndz.float_button_overlay.utils.Constants.CHANNEL;
import static br.ndz.float_button_overlay.utils.Constants.MAX_CLICK_DURATION;
import static br.ndz.float_button_overlay.utils.Constants.PACKAGE;
import static br.ndz.float_button_overlay.utils.Constants.TAG;

;

public class FloatButtonService extends Service {

    Context context;
    FlutterEngine flutterEngine;
    MethodChannel channel;
    LocalBroadcastManager mLocalBroadcastManager;
    FloatButtonService mService;

    private WindowManager mWindowManager;
    private ImageView imageView;
    private RelativeLayout mFloatingWidget;
    private View bgFloat;
    private long startClickTime;
    private WindowManager.LayoutParams params;
    private int maxY = 0;
    private int endArea = 0;
    private Timer timer;
    private TimerTask timerTask;
    private String packageName;
    private String activityName;
    private String iconPath;
    private String notificatitionTitle;
    private String notificatitionText;
    private boolean showTransparentCircle;
    private int iconWidth;
    private int iconHeight;
    private int transpCircleWidth;
    private int transpCircleHeight;

    Display display;
    Point floatButtonsize;

    private final IBinder mBinder = new LocalBinder();
    private static final int NOTIFICATION_ID = 1326875;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate() {

        flutterEngine = new FlutterEngine(this);
        flutterEngine.getDartExecutor().executeDartEntrypoint(DartExecutor.DartEntrypoint.createDefault());
        channel = new MethodChannel(flutterEngine.getDartExecutor(), CHANNEL);

        mFloatingWidget = (RelativeLayout) LayoutInflater.from(this).inflate(R.layout.button_activity, null);
        mFloatingWidget.findViewById(R.id.button1);

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

    private void StartAppIntent(String packageName, String activityName) {

        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e){
            e.printStackTrace();
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(TAG, "Service started");
        context = getApplicationContext();

        try {

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

                Log.i(TAG, extras.toString());

            } else {
                Log.i(TAG, "No intent Extras");
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        StartFloatButtonLayout();

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
                                        stopSelf();
                                        stopForeground(true);
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
                            if(packageName != null && !packageName.isEmpty()){
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(NOTIFICATION_ID, getNotification());

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {

        Log.i(TAG, "in onBind()");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(NOTIFICATION_ID, getNotification());

        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.i(TAG, "in onRebind()");
        stopForeground(true);
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "Unbinding Service");
        return false;
    }

    @Override
    public void onDestroy() {        

        try {
            if (mFloatingWidget != null) mWindowManager.removeView(mFloatingWidget);
            mFloatingWidget = null;
            mWindowManager = null;
            channel = null;
            flutterEngine.destroy();
            SendBroadcastToFinishApp();
        } catch (Exception e) {
            
        }
    }

    private void startMyOwnForeground() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel chan = new NotificationChannel(PACKAGE, notificatitionTitle, NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.YELLOW);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);

            Notification.Builder notificationBuilder = new Notification.Builder(this, PACKAGE);
            notificationBuilder.setOngoing(true)
                    .setContentText(notificatitionText)
                    .setContentTitle(notificatitionTitle)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setColor(0xffffffff);

            if (iconPath == null) {
                notificationBuilder.setSmallIcon(R.drawable.ic_flutter);
            } else {
                notificationBuilder.setSmallIcon(Icon.createWithBitmap(BitmapFactory.decodeFile(iconPath)));
            }

            Notification notification = notificationBuilder.build();
            startForeground(2, notification);
        }
    }

    private void SendBroadcastToFinishApp() {

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastManager.sendBroadcast(new Intent(BROADCAST_FINISH_APP));

    }

    private void StartFloatButtonLayout() {

        try {

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

            mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(iconWidth, iconHeight);
            lp.addRule(RelativeLayout.CENTER_IN_PARENT);
            imageView = new ImageView(getApplicationContext());
            imageView.setLayoutParams(lp);
            imageView.setImageBitmap(BitmapFactory.decodeFile(iconPath));
            mFloatingWidget.addView(imageView);

            ShapeDrawable shapeBg = new ShapeDrawable(new OvalShape());
            shapeBg.setIntrinsicHeight(transpCircleHeight);
            shapeBg.setIntrinsicWidth(transpCircleWidth);
            shapeBg.setAlpha(10);

            if(!showTransparentCircle){
                mFloatingWidget.setBackgroundResource(R.drawable.null_selector);
            }else{
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
            FloatButtonOverlayPlugin.invokeCallBack("callback", null);
        }catch (Exception e){
            Log.e(TAG, "Exception: " + e.getMessage());
        }
    }

    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        channel.invokeMethod("callback", null);
                        Log.i(TAG, "Send to Flutter");
                    }
                });
            }
        };
    }

    private Notification getNotification() {

        Notification.Builder builder = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            builder = new Notification.Builder(this)
                    .setContentText(notificatitionText)
                    .setContentTitle(notificatitionTitle)
                    .setOngoing(true)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setLargeIcon(BitmapFactory.decodeFile(iconPath))
                    .setTicker(notificatitionText)
                    .setWhen(System.currentTimeMillis())
                    .setColor(0xffffffff)
                    .setVisibility(Notification.VISIBILITY_SECRET);
        }

        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(PACKAGE); // Channel ID
        }

        if (iconPath == null) {
            builder.setSmallIcon(R.drawable.ic_flutter);
        } else {
            builder.setSmallIcon(Icon.createWithBitmap(BitmapFactory.decodeFile(iconPath)));
        }

        return builder.build();
    }

    public void stopTimerTask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void startTimer() {

        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, send event to dart code
        timer.schedule(timerTask, 5000, 5000); //
    }

    public class LocalBinder extends Binder {
        public FloatButtonService getService() {
            return FloatButtonService.this;
        }
    }

}