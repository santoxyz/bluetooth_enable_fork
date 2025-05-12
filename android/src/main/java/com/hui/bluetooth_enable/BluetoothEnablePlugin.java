package com.hui.bluetooth_enable;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener;
import io.flutter.plugin.common.PluginRegistry.RequestPermissionsResultListener;

public class BluetoothEnablePlugin implements FlutterPlugin, ActivityAware, MethodCallHandler,
        ActivityResultListener, RequestPermissionsResultListener {

    private static final String TAG = "BluetoothEnablePlugin";
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;

    private MethodChannel channel;
    private Activity activity;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private Result pendingResult;

    @Override
    public void onAttachedToEngine(FlutterPluginBinding binding) {
        channel = new MethodChannel(binding.getBinaryMessenger(), "bluetooth_enable");
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onDetachedFromEngine(FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
        channel = null;
        bluetoothAdapter = null;
        bluetoothManager = null;
    }

    @Override
    public void onAttachedToActivity(ActivityPluginBinding binding) {
        activity = binding.getActivity();
        bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        binding.addActivityResultListener(this);
        binding.addRequestPermissionsResultListener(this);
    }

    @Override
    public void onReattachedToActivityForConfigChanges(ActivityPluginBinding binding) {
        onAttachedToActivity(binding);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        activity = null;
    }

    @Override
    public void onDetachedFromActivity() {
        activity = null;
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if (bluetoothAdapter == null && !"isAvailable".equals(call.method)) {
            result.error("bluetooth_unavailable", "The device does not have Bluetooth", null);
            return;
        }

        switch (call.method) {
            case "enableBluetooth":
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        REQUEST_ENABLE_BLUETOOTH);
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
                pendingResult = result;
                break;

            case "customEnable":
                try {
                    if (!bluetoothAdapter.isEnabled()) {
                        bluetoothAdapter.disable();
                        Thread.sleep(500);
                        bluetoothAdapter.enable();
                    }
                    result.success("true");
                } catch (InterruptedException e) {
                    Log.e(TAG, "customEnable", e);
                    result.error("interrupted", "Interrupted during custom enable", null);
                }
                break;

            default:
                result.notImplemented();
                break;
        }
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (pendingResult != null) {
                if (resultCode == Activity.RESULT_OK) {
                    pendingResult.success("true");
                } else {
                    pendingResult.success("false");
                }
                pendingResult = null;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // Handle permission result if needed
        Log.d(TAG, "onRequestPermissionsResult");
        return false;
    }

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "Bluetooth STATE_OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "Bluetooth STATE_TURNING_OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "Bluetooth STATE_ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "Bluetooth STATE_TURNING_ON");
                        break;
                }
            }
        }
    };
}
