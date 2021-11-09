package br.ndz.float_button_overlay.services;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
import static br.ndz.float_button_overlay.utils.Constants.BROADCAST_FINISH_APP;
import static br.ndz.float_button_overlay.utils.Constants.CHANNEL;
import static br.ndz.float_button_overlay.utils.Constants.MAX_CLICK_DURATION;
import static br.ndz.float_button_overlay.utils.Constants.PACKAGE;
import static br.ndz.float_button_overlay.utils.Constants.TAG;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.Icon;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.skyfishjy.library.RippleBackground;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import br.ndz.float_button_overlay.FloatButtonOverlayPlugin;
import br.ndz.float_button_overlay.R;
import br.ndz.float_button_overlay.utils.ApplicationMonitor;
import br.ndz.float_button_overlay.utils.AsyncBitmapDownload;
import br.ndz.float_button_overlay.utils.AsyncHttpPost;
import br.ndz.float_button_overlay.utils.AsyncResponse;
import de.hdodenhof.circleimageview.CircleImageView;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.dart.DartExecutor;
import io.flutter.plugin.common.MethodChannel;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.WebSocket;

public class FloatButtonService extends Service implements LocationListener {

    Context context;
    FlutterEngine flutterEngine;
    MethodChannel channel;

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

    private long startClickTime;
    private WindowManager.LayoutParams params;
    private WindowManager.LayoutParams params2;
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
    private String wsRoom;
    private String wsUrl;
    private String driverId;
    private String recipientId;
    private String driverImageProfileUrl;
    private String driverName;
    private String driverPlate;
    private String driverCarModel;
    private String acceptUrl;
    private String driverPositionUrl;
    private int iconWidth;
    private int iconHeight;
    private int transpCircleWidth;
    private int transpCircleHeight;
    private JSONObject tripObject;
    private CountDownTimer cdtTimer;

    private LocationManager locationManager;
    private Location location;
    private Criteria criteria;
    private AsyncHttpPost asyncHttpPost;

    Display display;
    Point floatButtonsize;

    private final IBinder mBinder = new LocalBinder();
    private static final int NOTIFICATION_ID = 1326875;
    Socket mSocket;
    MediaPlayer mediaPlayer;

    void websocketHandler() {
        try {

            IO.Options options = new IO.Options();
            options.transports = new String[]{WebSocket.NAME};
            mSocket = IO.socket(wsUrl, options);

            mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.i(TAG, Socket.EVENT_CONNECT);
                }

            }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.i(TAG, Socket.EVENT_DISCONNECT);

                }
            }).on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    String err = args[0].toString();
                    Log.i(TAG, "error " + err);
                }
            });

            mSocket.on(wsRoom, new Emitter.Listener() {
                @Override
                public void call(Object... args) {

                    try {
                        String data = (String) args[0];

                        tripObject = new JSONObject(data);
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            public void run() {
                                try {
                                    String estimatedPrice = "R$ " + String.format("%.2f", tripObject.getDouble("ESTIMATEDPRICE")).replace(".", ",");
                                    String distance = String.format("%.1f", tripObject.getDouble("DISTANCEM") / 1000) + " Km";
                                    String duration = String.format("%.0f", tripObject.getDouble("DURATION") / 60) + " min";

                                    secondScreen(tripObject.getString("PASSENGERIMAGEPROFILEURL"), distance,
                                            duration, tripObject.getString("FROMTEXT"),
                                            tripObject.getString("TOTEXT"),
                                            estimatedPrice);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            });

            mSocket.connect();

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate() {

        flutterEngine = new FlutterEngine(this);
        flutterEngine.getDartExecutor().executeDartEntrypoint(DartExecutor.DartEntrypoint.createDefault());
        channel = new MethodChannel(flutterEngine.getDartExecutor(), CHANNEL);

        mFloatingWidget = (RelativeLayout) LayoutInflater.from(this).inflate(R.layout.button_activity, null);
        mFloatingWidget.findViewById(R.id.button1);

        mFloatingWidget2 = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.notification_layout, null);
        mFloatingWidget2.findViewById(R.id.button2);

        estimatedPriceWidget = (TextView) mFloatingWidget2.findViewById(R.id.estimated_price);
        durationWidget = (TextView) mFloatingWidget2.findViewById(R.id.duration);
        distanceWidget = (TextView) mFloatingWidget2.findViewById(R.id.distance);
        fromTextWidget = (TextView) mFloatingWidget2.findViewById(R.id.from_text);
        toTextWidget = (TextView) mFloatingWidget2.findViewById(R.id.to_text);
        passengerProfileImageWidget = (CircleImageView) mFloatingWidget2.findViewById(R.id.profile_image);

        rippleBackground = (RippleBackground) mFloatingWidget2.findViewById(R.id.content);

        this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
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

    private void startAppIntent(String packageName, String activityName) {

        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent;

        if (Build.VERSION.SDK_INT >= 31) {
            pendingIntent =
                    PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE);
        }else{
            pendingIntent =
                    PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
        }
        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }

    }

    @SuppressLint({"ClickableViewAccessibility", "MissingPermission"})
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(TAG, "Service started");
        context = getApplicationContext();
        Bundle extras = intent.getExtras();
        notificatitionText = extras.getString("notificationText");
        notificatitionTitle = extras.getString("notificationTitle");
        iconPath = extras.getString("iconPath");
        wsUrl = extras.getString("wsUrl");
        wsRoom = extras.getString("wsRoom");
        driverPlate = extras.getString("driverPlate");
        driverCarModel = extras.getString("driverCarModel");
        driverName = extras.getString("driverName");
        driverId = extras.getString("driverId");
        driverImageProfileUrl = extras.getString("driverImageProfileUrl");
        driverPositionUrl = extras.getString("driverPositionUrl");
        packageName = extras.getString("packageName");
        activityName = extras.getString("activityName");
        acceptUrl = extras.getString("acceptUrl");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startMyOwnForeground(notificatitionText,
                    notificatitionTitle,
                    iconPath);
        else
            startForeground(FOREGROUND_SERVICE_TYPE_LOCATION, getNotification(notificatitionText,
                    notificatitionTitle,
                    iconPath));

        startTimer();        
        this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        this.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 4000, 10, this);
        websocketHandler();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {

        Bundle extras = intent.getExtras();

        Log.i(TAG, "in onBind()");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startMyOwnForeground(extras.getString("notificationText"),
                    extras.getString("notificationTitle"),
                    extras.getString("iconPath")
            );
        else
            startForeground(NOTIFICATION_ID, getNotification(extras.getString("notificationText"),
                    extras.getString("notificationTitle"),
                    extras.getString("iconPath")));

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
            stopTimerTask();
            locationManager.removeUpdates(this);
            mSocket.close();
            channel = null;
            flutterEngine.destroy();
            SendBroadcastToFinishApp();
        } catch (Exception e) {

        }
    }

    private void startMyOwnForeground(String notificationText, String notificationTitle, String iconPath) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel chan = new NotificationChannel(PACKAGE, notificationTitle, NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.YELLOW);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);

            Notification.Builder notificationBuilder = new Notification.Builder(this, PACKAGE);
            notificationBuilder.setOngoing(true)
                    .setContentText(notificationText)
                    .setContentTitle(notificationTitle)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setColor(0xffffffff);

            if (iconPath == null) {
                notificationBuilder.setSmallIcon(R.drawable.ic_flutter);
            } else {
                notificationBuilder.setSmallIcon(Icon.createWithBitmap(BitmapFactory.decodeFile(iconPath)));
            }

            Notification notification = notificationBuilder.build();
            startForeground(FOREGROUND_SERVICE_TYPE_LOCATION, notification);
        }
    }

    private void SendBroadcastToFinishApp() {

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastManager.sendBroadcast(new Intent(BROADCAST_FINISH_APP));

    }

    public void rejectClick(View view) {
        rejectTrip();
    }

    private void rejectTrip() {
        JSONObject data = new JSONObject();

        try {
            data.put("TRIPID", tripObject.getString("TRIPID"));
            data.put("DRIVERANSWER", false);
            data.put("DRIVERID", Integer.valueOf(driverId));

            mSocket.emit("driver-answer", data);

            mediaPlayer.stop();
            mWindowManager2.removeView(mFloatingWidget2);
            cdtTimer.cancel();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void acceptClick(View view) {
        JSONObject data = new JSONObject();

        try {
            data.put("TRIPID", tripObject.getString("TRIPID"));
            data.put("DRIVERANSWER", true);
            data.put("DRIVERID", Integer.valueOf(driverId));

            mSocket.emit("driver-answer", data);

            tripObject.put("DRIVERLAT", location.getLatitude());
            tripObject.put("DRIVERLON", location.getLongitude());
            tripObject.put("RECIPIENTID", recipientId);
            tripObject.put("DRIVERNAME", driverName);
            tripObject.put("DRIVERID", Integer.parseInt(driverId));
            tripObject.put("EVENTNAME", "ACCEPT_TRIP");
            tripObject.put("ORIGIN", "DRIVER");
            tripObject.put("STATUS", "DRIVER_ENROUTE");
            tripObject.put("DRIVERIMAGEPROFILEURL", driverImageProfileUrl);

            AsyncHttpPost asyncHttpPost = new AsyncHttpPost(tripObject, acceptUrl);
            asyncHttpPost.execute();

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @SuppressLint("MissingPermission")
                @Override
                public void run() {
                    Log.i(TAG,"Vai invocar o metodo onClickCallback");
                    channel.invokeMethod("onClickCallback", tripObject.toString() );
                }
            });

            mediaPlayer.stop();
            mWindowManager2.removeView(mFloatingWidget2);
            cdtTimer.cancel();

            ApplicationMonitor applicationMonitor = new ApplicationMonitor();
            if(!applicationMonitor.isAppRunning(context, packageName)){
                startAppIntent(packageName, activityName);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        //mWindowManager2.removeView(mFloatingWidget2);
    }

    public void sendDriverPosition() {
        final JSONObject data = new JSONObject();

        try {
            data.put("EVENTNAME", "DRIVER_POSITION");
            data.put("ORIGIN", "DRIVER");
            data.put("STATUS", "WANDERING");
            data.put("DRIVERPLATE", driverPlate);
            data.put("DRIVERCARMODEL", driverCarModel);
            data.put("DRIVERNAME", driverName);
            data.put("DRIVERLAT", location.getLatitude());
            data.put("DRIVERLON", location.getLongitude());
            data.put("DRIVERID", driverId);
            data.put("DRIVERPROFILEURLIMAGE", driverImageProfileUrl);
            data.put("RECIPIENTID", recipientId);

            asyncHttpPost = new AsyncHttpPost(data, driverPositionUrl);
            asyncHttpPost.execute();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void secondScreen(String passengerProfileUrlImage, String distance, String duration, String fromText, String toText, String estimatedPrice) {
        try {

            final String profileUrl = passengerProfileUrlImage;

            params2 = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                            : WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

            params2.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            mWindowManager2 = (WindowManager) getSystemService(WINDOW_SERVICE);

            estimatedPriceWidget.setText(estimatedPrice);
            distanceWidget.setText(distance);
            durationWidget.setText(duration);
            fromTextWidget.setText(fromText);
            toTextWidget.setText(toText);

            if (profileUrl.length() > 0) {

                AsyncBitmapDownload asyncBitmapDownload = new AsyncBitmapDownload(new AsyncResponse() {
                    @Override
                    public void processFinish(Object output) {
                        passengerProfileImageWidget.setImageBitmap((Bitmap) output);

                        try {
                            mWindowManager2.addView(mFloatingWidget2, params2);
                        } catch (Exception e) {

                        }
                        mWindowManager2.updateViewLayout(mFloatingWidget2, params2);
                        rippleBackground.startRippleAnimation();
                        playSound();
                        startTimeToCloseNotification();
                    }
                });
                asyncBitmapDownload.execute(profileUrl);
            } else {
                mWindowManager2.updateViewLayout(mFloatingWidget2, params2);
                playSound();
            }


        } catch (Exception e) {
            Log.i(TAG, e.getMessage());
        }
    }

    private void playSound() {
        mediaPlayer = MediaPlayer.create(context, R.raw.vite);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
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
            FloatButtonOverlayPlugin.invokeCallBack("callback", null);
        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
        }
    }

    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void run() {

                        if (locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) != null) {
                            //channel.invokeMethod("callback", null);
                            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            Log.i(TAG, Calendar.getInstance().getTime() + ": Latitude: " + String.valueOf(location.getLatitude()) + " Longitude: " + String.valueOf(location.getLongitude()));

                            //Send driver_position
                            sendDriverPosition();
                        }
                    }
                });
            }
        };
    }

    private Notification getNotification(String notificationText, String notificationTitle, String iconPath) {

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
        timer.schedule(timerTask, 7000, 7000); //
    }

    private void startTimeToCloseNotification(){
        final long EndTime   = 3600;
        cdtTimer = new CountDownTimer(15000, 1000) {
            public void onTick(long millisUntilFinished) {
                long secondUntilFinished = (long) (millisUntilFinished/1000);
                long secondsPassed = (EndTime - secondUntilFinished);
                long minutesPassed = (long) (secondsPassed/60);
                secondsPassed = secondsPassed%60;
            }

            public void onFinish() {
                //tvCounterTimer.setText("done!");
                mediaPlayer.stop();
                mWindowManager2.removeView(mFloatingWidget2);
            }
        }.start();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "Latitude: " + String.valueOf(location.getLatitude()) + " Longitude: " + String.valueOf(location.getLongitude()));
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        Log.i(TAG, s);
    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    public class LocalBinder extends Binder {
        public FloatButtonService getService() {
            return FloatButtonService.this;
        }
    }


}