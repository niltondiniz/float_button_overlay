package br.ndz.float_button_overlay;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;

import br.ndz.float_button_overlay.display.FloatDisplay;
import br.ndz.float_button_overlay.services.FloatButtonService;

import br.ndz.float_button_overlay.utils.ApplicationMonitor;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import static br.ndz.float_button_overlay.utils.Constants.CHANNEL;
import static br.ndz.float_button_overlay.utils.Constants.TAG;

import java.util.Timer;
import java.util.TimerTask;

/**
 * FloatButtonOverlayPlugin
 */
public class FloatButtonOverlayPlugin extends Activity implements MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    public static MethodChannel methodChannel;
    private Context mContext;
    @SuppressLint("StaticFieldLeak")
    private static Activity mActivity;
    public static int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 1237;
    private static NotificationManager notificationManager;
    private FloatButtonService mService;
    private FloatDisplay floatDisplay;
    private ApplicationMonitor applicationMonitor;
    private Timer applicationMonitorTimer;
    private TimerTask applicationMonitorTimerTask;
    private String packageName;
    private Intent openServiceIntent;

    @SuppressWarnings("unused")
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), CHANNEL);
        channel.setMethodCallHandler(new FloatButtonOverlayPlugin(registrar.context(), registrar.activity(), channel));
    }

    private FloatButtonOverlayPlugin(Context context, Activity activity, MethodChannel newMethodChannel) {
        this.mContext = context;
        mActivity = activity;
        methodChannel = newMethodChannel;
        methodChannel.setMethodCallHandler(this);
        floatDisplay = new FloatDisplay(this.mContext);
        applicationMonitor = new ApplicationMonitor();
    }

    public static void invokeCallBack(String type, Object params) {
        methodChannel.invokeMethod(type, params);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {

        if (call.method.equals("getPlatformVersion")) {

            result.success("Android " + android.os.Build.VERSION.RELEASE);

        } else if (call.method.equals("openOverlay")) {

            Log.d(TAG, "Will show FloatButton");
            setIntentData(call);
            floatDisplay.openBubble(getIntentData());
            result.success(true);

        } else if (call.method.equals("closeOverlay")) {

            floatDisplay.closeBubble();
            result.success("Called close bubble");

        } else if (call.method.equals("openAppByPackage")) {

            String packageName = (String) call.argument("packageName");

            Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(packageName);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            PendingIntent pendingIntent;
            if (Build.VERSION.SDK_INT >= 31) {
                pendingIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_IMMUTABLE);
            } else {
                pendingIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_ONE_SHOT);
            }

            try {
                pendingIntent.send();
                result.success(true);
            } catch (PendingIntent.CanceledException e) {
                result.success(false);
                e.printStackTrace();
            }

        } else if (call.method.equals("checkPermissions")) {

            boolean locationPermissions;
            boolean notificationPermissions;

            if (checkLocationPermission()) {
                locationPermissions = true;
            } else {
                locationPermissions = false;
            }

            if (checkPermission()) {
                notificationPermissions = true;

            } else {
                notificationPermissions = false;
            }

            if (notificationPermissions && locationPermissions) {
                result.success("Permissions are granted");
            } else {
                result.success("Permissions are not granted");
            }

        } else if (call.method.equals("startService")) {

            Log.d(TAG, "Will start the service without open bubble");

            if (!applicationMonitor.isServiceRunning(mContext, "br.ndz.float_button_overlay")) {
                setIntentData(call);
                final Intent i = getIntentData();

                //Verify if packageName app is running to control visibility of float button overlay
                applicationMonitor();

                mContext.startService(i);
            }
            result.success(true);

        } else if (call.method.equals("stopService")) {
            final Intent i = new Intent(mContext, FloatButtonService.class);
            Log.d(TAG, "Stopping service");
            floatDisplay.closeBubble();
            mContext.stopService(i);
            applicationMonitorTimer.cancel();
            result.success("Called stop service");
        } else {

            result.notImplemented();

        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE) {
            if (!Settings.canDrawOverlays(mContext)) {
                //Log.e(TAG, "Float Button Overlay will not work without 'Can Draw Over Other Apps' permission");
                //Toast.makeText(mContext, "Float Button Overlay will not work without 'Can Draw Over Other Apps' permission", Toast.LENGTH_LONG).show();
            }
        }

    }

    public boolean checkLocationPermission() {

        if (Build.VERSION.SDK_INT >= 23) {
            if (mContext.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED &&
                    mContext.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                            PackageManager.PERMISSION_GRANTED) {

                return true;

            } else {
                //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                Intent intent = new Intent(Settings.ACTION_LOCALE_SETTINGS,
                        Uri.parse("package:" + mContext.getPackageName()));
                if (mActivity == null) {
                    if (mContext != null) {
                        mContext.startActivity(intent);
                        //Toast.makeText(mContext, "Please grant, Can Draw Over Other Apps permission.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Can't detect the permission change, as the mActivity is null");
                    } else {
                        Log.e(TAG, "'Can Draw Over Other Apps' permission is not granted");
                        //Toast.makeText(mContext, "Can Draw Over Other Apps permission is required. Please grant it from the app settings", Toast.LENGTH_LONG).show();
                    }
                } else {
                    mActivity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                }
            }
        } else {
            int accessFineLocation = PermissionChecker.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION);
            int accessCoarseLocation = PermissionChecker.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_COARSE_LOCATION);
            if (accessFineLocation == PermissionChecker.PERMISSION_GRANTED && accessCoarseLocation == PermissionChecker.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;

    }

    public boolean checkPermission() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            initNotificationManager();
            if (!notificationManager.areBubblesAllowed()) {
                Log.e(TAG, "System Alert Window will not work without enabling the android bubbles");
                //Toast.makeText(mContext, "System Alert Window will not work without enabling the android bubbles", Toast.LENGTH_LONG).show();
            } else {
                //TODO to check for higher android versions, post their release
                Log.d(TAG, "Android bubbles are enabled");
                return true;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(mContext)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + mContext.getPackageName()));
                if (mActivity == null) {
                    if (mContext != null) {
                        mContext.startActivity(intent);
                        //Toast.makeText(mContext, "Please grant, Can Draw Over Other Apps permission.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Can't detect the permission change, as the mActivity is null");
                    } else {
                        Log.e(TAG, "'Can Draw Over Other Apps' permission is not granted");
                        //Toast.makeText(mContext, "Can Draw Over Other Apps permission is required. Please grant it from the app settings", Toast.LENGTH_LONG).show();
                    }
                } else {
                    mActivity.startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
                }
            } else {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unused")
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private boolean handleBubblesPermissionForAndroidQ() {
        int devOptions = Settings.Secure.getInt(mContext.getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);
        if (devOptions == 1) {
            Log.d(TAG, "Android bubbles are enabled");
            return true;
        } else {
            Log.e(TAG, "System Alert Window will not work without enabling the android bubbles");
            //Toast.makeText(mContext, "Enable android bubbles in the developer options, for System Alert Window to work", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initNotificationManager() {
        if (notificationManager == null) {
            if (mContext == null) {
                if (mActivity != null) {
                    mContext = mActivity.getApplicationContext();
                }
            }
            if (mContext == null) {
                Log.e(TAG, "Context is null. Can't show the System Alert Window");
                return;
            }
            notificationManager = mContext.getSystemService(NotificationManager.class);
        }
    }

    private void initPermissionManager() {

    }

    public void applicationMonitor() {
        applicationMonitorTimer = new Timer();

        applicationMonitorTimerTask = new TimerTask() {
            @Override
            public void run() {
                if (applicationMonitor.isAppRunning(mContext, packageName)) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (floatDisplay.isOpen()) {
                                floatDisplay.closeBubble();
                            }
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (!floatDisplay.isOpen()) {
                                floatDisplay.openBubble(getIntentData());
                            }
                        }
                    });
                }
            }
        };

        applicationMonitorTimer.schedule(applicationMonitorTimerTask, 1000, 1000); //
    }

    private void setIntentData(MethodCall call) {

        if (openServiceIntent == null) {
            openServiceIntent = new Intent(mContext, FloatButtonService.class);

            openServiceIntent.putExtra("iconPath", (String) call.argument("iconPath"));
            openServiceIntent.putExtra("packageName", (String) call.argument("packageName"));
            openServiceIntent.putExtra("activityName", (String) call.argument("activityName"));
            openServiceIntent.putExtra("notificationTitle", (String) call.argument("notificationTitle"));
            openServiceIntent.putExtra("notificationText", (String) call.argument("notificationText"));
            openServiceIntent.putExtra("showTransparentCircle", (boolean) call.argument("showTransparentCircle"));
            openServiceIntent.putExtra("iconWidth", (int) call.argument("iconWidth"));
            openServiceIntent.putExtra("iconHeight", (int) call.argument("iconHeight"));
            openServiceIntent.putExtra("transpCircleWidth", (int) call.argument("transpCircleWidth"));
            openServiceIntent.putExtra("transpCircleHeight", (int) call.argument("transpCircleHeight"));
            openServiceIntent.putExtra("wsRoom", (String) call.argument("wsRoom"));
            openServiceIntent.putExtra("wsUrl", (String) call.argument("wsUrl"));
            openServiceIntent.putExtra("driverId", (String) call.argument("wsRoom"));
            openServiceIntent.putExtra("recipientId", (String) call.argument("recipientId"));
            openServiceIntent.putExtra("driverImageProfileUrl", (String) call.argument("driverImageProfileUrl"));
            openServiceIntent.putExtra("driverName", (String) call.argument("driverName"));
            openServiceIntent.putExtra("acceptUrl", (String) call.argument("acceptUrl"));
            openServiceIntent.putExtra("driverPositionUrl", (String) call.argument("driverPositionUrl"));
            openServiceIntent.putExtra("driverPlate", (String) call.argument("driverPlate"));
            openServiceIntent.putExtra("driverCarModel", (String) call.argument("driverCarModel"));
            openServiceIntent.putExtra("driverRateValue", (String) call.argument("driverRateValue"));

            packageName = call.argument("packageName");

        }

    }

    private Intent getIntentData() {
        return openServiceIntent;
    }

}
