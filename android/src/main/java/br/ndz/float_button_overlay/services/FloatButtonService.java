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
import android.os.Binder;
import android.os.Build;
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

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import br.ndz.float_button_overlay.R;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.dart.DartExecutor;
import io.flutter.plugin.common.MethodChannel;

import static br.ndz.float_button_overlay.utils.Constants.BROADCAST_FINISH_APP;
import static br.ndz.float_button_overlay.utils.Constants.BROADCAST_STATE_HIDE_FLOAT;
import static br.ndz.float_button_overlay.utils.Constants.BROADCAST_STATE_SHOW_FLOAT;
import static br.ndz.float_button_overlay.utils.Constants.CHANNEL;
import static br.ndz.float_button_overlay.utils.Constants.MAX_CLICK_DURATION;
import static br.ndz.float_button_overlay.utils.Constants.NOTIFICATION_TEXT;
import static br.ndz.float_button_overlay.utils.Constants.NOTIFICATION_TITLE;
import static br.ndz.float_button_overlay.utils.Constants.PACKAGE;
import static br.ndz.float_button_overlay.utils.Constants.TAG;

public class FloatButtonService extends Service {

    Context context;
    FlutterEngine flutterEngine;
    MethodChannel channel;
    LocalBroadcastManager mLocalBroadcastManager;

    private MyReceiver myReceiver = new MyReceiver();
    private WindowManager mWindowManager;
    private RelativeLayout mFloatingWidget;
    private long startClickTime;
    private WindowManager.LayoutParams params;
    private int maxY = 0;
    private int endArea = 0;
    private Timer timer;
    private TimerTask timerTask;

    private final IBinder mBinder = new LocalBinder();
    private static final int NOTIFICATION_ID = 1326875;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate() {

        flutterEngine = new FlutterEngine(this);
        flutterEngine.getDartExecutor().executeDartEntrypoint(DartExecutor.DartEntrypoint.createDefault());
        channel = new MethodChannel(flutterEngine.getDartExecutor(), CHANNEL);
        RegisterFloatStateBroadcast();
        StartFloatButtonLayout();

        mFloatingWidget.setOnTouchListener(new View.OnTouchListener() {

            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:

                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();

                        //Getting press time
                        startClickTime = Calendar.getInstance().getTimeInMillis();
                        return false;

                    case MotionEvent.ACTION_UP:

                        int Xdiff = (int) (event.getRawX() - initialTouchX);
                        int Ydiff = (int) (event.getRawY() - initialTouchY);

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

                            Log.i(TAG, String.valueOf(channel));
                            channel.invokeMethod("callback", null, new MethodChannel.Result() {

                                @Override
                                public void success(@Nullable Object result) {
                                    HashMap<String, String> data = (HashMap<String, String>) result;
                                    StartAppIntent(data.get("packageName"), data.get("activityName"));
                                    Log.i(TAG, "App started");
                                }

                                @Override
                                public void error(String errorCode, @Nullable String errorMessage, @Nullable Object errorDetails) {
                                }

                                @Override
                                public void notImplemented() {
                                }
                            });
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

    private void StartAppIntent(String packageName, String activityName) {
        ComponentName cn = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cn = new ComponentName(packageName, packageName + "." + activityName);
        }
        Intent intent = new Intent();
        intent.setComponent(cn);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(TAG, "Service started");
        context = getApplicationContext();

        //Starting timer that invoke method on Flutter
        //startTimer();
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
    public void onDestroy() {

        //Stops the timer
        //stopTimerTask();

        try {
            if (mFloatingWidget != null) mWindowManager.removeView(mFloatingWidget);
            SendBroadcastToFinishApp();
        } catch (Exception e) {
            //
        }
    }

    private void startMyOwnForeground() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel chan = new NotificationChannel(PACKAGE, NOTIFICATION_TITLE, NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.YELLOW);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, PACKAGE);
            Notification notification = notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.drawable.ic_flutter)
                    .setContentText(NOTIFICATION_TEXT)
                    .setContentTitle(NOTIFICATION_TITLE)
                    .setPriority(NotificationManager.IMPORTANCE_MIN)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setColor(0xffffffff)
                    .build();
            startForeground(2, notification);
        }
    }

    private void SendBroadcastToFinishApp() {
        Log.i(TAG, "Enviando broadcast para finalizar app");
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastManager.sendBroadcast(new Intent(BROADCAST_FINISH_APP));
        Log.i(TAG, "Broadcast Enviado");
    }

    private void StartFloatButtonLayout() {

        mFloatingWidget = (RelativeLayout) LayoutInflater.from(this).inflate(R.layout.button_activity, null);

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
        mFloatingWidget.findViewById(R.id.button1);


        channel.invokeMethod("seticon", null, new MethodChannel.Result() {
            @Override
            public void success(@Nullable Object result) {

                HashMap<String, String> data = (HashMap<String, String>) result;
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(150, 150);
                lp.addRule(RelativeLayout.CENTER_IN_PARENT);
                ImageView imageView = new ImageView(getApplicationContext());
                imageView.setLayoutParams(lp);
                imageView.setImageBitmap(BitmapFactory.decodeFile(data.get("iconPath")));
                mFloatingWidget.addView(imageView);
                mWindowManager.addView(mFloatingWidget, params);

                /**
                 * Setting close area height.
                 * The close area corresponds to screen height - 20%
                 *
                 * */
                Display display = mWindowManager.getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                maxY = size.y;
                endArea = maxY - (int) (maxY * 0.20);
            }

            @Override
            public void error(String errorCode, @Nullable String errorMessage, @Nullable Object errorDetails) {

            }

            @Override
            public void notImplemented() {

            }
        });


    }

    private void RegisterFloatStateBroadcast() {
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(BROADCAST_STATE_SHOW_FLOAT);
        mIntentFilter.addAction(BROADCAST_STATE_HIDE_FLOAT);
        mLocalBroadcastManager.registerReceiver(myReceiver, mIntentFilter);
    }

    public void floatingView(boolean bShow) {
        if (!bShow) {
            if (mFloatingWidget != null) {
                try {
                    mWindowManager.removeView(mFloatingWidget);
                } catch (Exception e) {
                    Log.i(TAG, "Erro ao setar como invisivel o floatbutton: " + e.getMessage());
                }
            }
        } else {
            try {
                mWindowManager.addView(mFloatingWidget, params);
            } catch (Exception e) {
                Log.i(TAG, "Erro ao setar como visivel o floatbutton: " + e.getMessage());
            }
        }
    }

    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                Log.i(TAG, "Enviar event para dart");
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        //channel.invokeMethod("acionar_enviar_posicao", null);
                        Log.i(TAG, "Send to Flutter");
                    }
                });
            }
        };
    }

    private Notification getNotification() {

        Intent intent = new Intent(this, FloatButtonService.class);

        //PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
        PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, FloatButtonService.class), 0);

        @SuppressLint("WrongConstant") NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .addAction(0, "Abrir App", activityPendingIntent)
                .setContentText(NOTIFICATION_TEXT)
                .setContentTitle(NOTIFICATION_TITLE)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.ic_flutter)
                .setTicker(NOTIFICATION_TEXT)
                .setWhen(System.currentTimeMillis())
                .setColor(0xffffffff)
                .setVisibility(Notification.VISIBILITY_SECRET);

        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(PACKAGE); // Channel ID
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

        Log.i("HERE", "Timer Started");
        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, send event to dart code
        timer.schedule(timerTask, 5000, 5000); //
    }

    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.i(TAG, "Received Broadcast: " + intent.getAction());

            if (intent.getAction().equals(BROADCAST_STATE_SHOW_FLOAT)) {
                floatingView(true);
            } else if (intent.getAction().equals(BROADCAST_STATE_HIDE_FLOAT)) {
                floatingView(false);
            }
        }
    }

    public class LocalBinder extends Binder {
        FloatButtonService getService() {
            return FloatButtonService.this;
        }
    }


}