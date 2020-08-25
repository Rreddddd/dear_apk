package com.lc.dear.utils;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionUtil {

    public static boolean check(Activity activity, String permission){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            if(PackageManager.PERMISSION_GRANTED==ContextCompat.checkSelfPermission(activity,permission)){
                return true;
            }
            ActivityCompat.requestPermissions(activity,new String[]{permission},AppConstant.REQUEST_PERMISSION_CODE);
            return false;
        }else{
            return true;
        }
    }
}
