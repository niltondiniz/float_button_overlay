package br.ndz.float_button_overlay;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import br.ndz.float_button_overlay.services.FloatButtonService;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import static br.ndz.float_button_overlay.utils.Constants.CHANNEL;
import static br.ndz.float_button_overlay.utils.Constants.TAG;

/** FloatButtonOverlayPlugin */
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
  }

  @RequiresApi(api = Build.VERSION_CODES.O)
  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {

    if (call.method.equals("getPlatformVersion")) {

      result.success("Android " + android.os.Build.VERSION.RELEASE);

    } else if (call.method.equals("openOverlay")){

      Log.d(TAG, "Will show FloatButton");
      final Intent i = new Intent(mContext, FloatButtonService.class);

      i.putExtra("iconPath", (String) call.argument("iconPath"));
      i.putExtra("packageName", (String) call.argument("packageName"));
      i.putExtra("activityName", (String) call.argument("activityName"));
      i.putExtra("notificationTitle", (String) call.argument("notificationTitle"));
      i.putExtra("notificationText", (String) call.argument("notificationText"));

      Log.i(TAG, "NotificationText" + " - " + call.argument("notificationText"));

      mContext.startService(i);
      result.success(true);

    } else if (call.method.equals("closeOverlay")){

      final Intent i = new Intent(mContext, FloatButtonService.class);
      Log.d(TAG, "Stopping service");
      mContext.stopService(i);
      result.success("Called close method");

    } else if (call.method.equals("checkPermissions")){

      if (checkPermission()) {
        result.success("Permissions are granted");
      } else {
        result.success("Permissions are not granted");
      }

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
        Log.e(TAG, "Float Button Overlay will not work without 'Can Draw Over Other Apps' permission");
        Toast.makeText(mContext, "Float Button Overlay will not work without 'Can Draw Over Other Apps' permission", Toast.LENGTH_LONG).show();
      }
    }

  }

  public boolean checkPermission() {
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
      initNotificationManager();
      if (!notificationManager.areBubblesAllowed()) {
        Log.e(TAG, "System Alert Window will not work without enabling the android bubbles");
        Toast.makeText(mContext, "System Alert Window will not work without enabling the android bubbles", Toast.LENGTH_LONG).show();
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
            Toast.makeText(mContext, "Please grant, Can Draw Over Other Apps permission.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Can't detect the permission change, as the mActivity is null");
          } else {
            Log.e(TAG, "'Can Draw Over Other Apps' permission is not granted");
            Toast.makeText(mContext, "Can Draw Over Other Apps permission is required. Please grant it from the app settings", Toast.LENGTH_LONG).show();
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
      Toast.makeText(mContext, "Enable android bubbles in the developer options, for System Alert Window to work", Toast.LENGTH_LONG).show();
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

}
