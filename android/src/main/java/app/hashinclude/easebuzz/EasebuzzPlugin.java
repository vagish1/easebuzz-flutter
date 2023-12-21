package app.hashinclude.easebuzz;

import static androidx.core.app.ActivityCompat.startActivityForResult;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.easebuzz.payment.kit.PWECouponsActivity;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import datamodels.PWEStaticDataModel;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;


public class EasebuzzPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;
  private boolean startPayment = true;
  private MethodChannel.Result channel_result;
  private Activity activity;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "easebuzz");
    channel.setMethodCallHandler(this);

  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    channel_result =result;
    if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + android.os.Build.VERSION.RELEASE);
      return;
    }

    if(call.method.equals("openGateway")){
      if (startPayment) {
        startPayment = false;
        startPayment(call.arguments);
      }
      return;
    }


  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  private void startPayment(Object arguments){
    try {
      if(activity!=null) {
        Gson gson = new Gson();
        JSONObject parameters = new JSONObject(gson.toJson(arguments));
        Intent intentProceed = new Intent(activity, PWECouponsActivity.class);
        intentProceed.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        Iterator<?> keys = parameters.keys();
        while (keys.hasNext()) {
          String value = "";
          String key = (String) keys.next();
          value = parameters.optString(key);
          if (key.equals("amount")) {
            Double amount = Double.valueOf("amount");
            intentProceed.putExtra(key, amount);
          } else {
            intentProceed.putExtra(key, value);
          }
        }
        activity.startActivityForResult(intentProceed, PWEStaticDataModel.PWE_REQUEST_CODE);
      }
    }catch (Exception e) {
      startPayment=true;
      Map<String, Object> error_map = new HashMap<>();
      Map<String, Object> error_desc_map = new HashMap<>();
      String error_desc = "exception occured:"+e.getMessage();
      error_desc_map.put("error","Exception");
      error_desc_map.put("error_msg",error_desc);
      error_map.put("result",PWEStaticDataModel.TXN_FAILED_CODE);
      error_map.put("payment_response",error_desc_map);
      channel_result.success(error_map);
    }
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding activityPluginBinding) {
    activity = activityPluginBinding.getActivity();
    activityPluginBinding.addActivityResultListener(this);

  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    activity = null;
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding activityPluginBinding) {
      activity = activityPluginBinding.getActivity();
      activityPluginBinding.addActivityResultListener(this);
  }

  @Override
  public void onDetachedFromActivity() {
    activity = null;


  }

  @Override
  public boolean onActivityResult(int requestCode, int i1, @Nullable Intent data) {
    if(data != null ) {
      if(requestCode==PWEStaticDataModel.PWE_REQUEST_CODE)
      {
        startPayment=true;
        JSONObject response = new JSONObject();
        Map<String, Object> error_map = new HashMap<>();
        if(data != null ) {
          String result = data.getStringExtra("result");
          String payment_response = data.getStringExtra("payment_response");
          try {
            assert payment_response != null;
            JSONObject obj = new JSONObject(payment_response);
            response.put("result", result);
            response.put("payment_response", obj);
            channel_result.success(JsonConverter.convertToMap(response));
          }catch (Exception e){
            Map<String, Object> error_desc_map = new HashMap<>();
            error_desc_map.put("error",result);
            error_desc_map.put("error_msg",payment_response);
            error_map.put("result",result);
            error_map.put("payment_response",error_desc_map);
            channel_result.success(error_map);
          }
        }else{
          Map<String, Object> error_desc_map = new HashMap<>();
          String error_desc = "Empty payment response";
          error_desc_map.put("error","Empty error");
          error_desc_map.put("error_msg",error_desc);
          error_map.put("result","payment_failed");
          error_map.put("payment_response",error_desc_map);
          channel_result.success(error_map);
        }
      }else
      {
        return false;
      }
    }
    return true;
  }
}
