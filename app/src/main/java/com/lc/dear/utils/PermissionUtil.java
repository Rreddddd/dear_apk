package com.lc.dear.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public abstract class PermissionUtil {

    public static boolean check(Activity activity, String permission){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            if(PackageManager.PERMISSION_GRANTED==ContextCompat.checkSelfPermission(activity,permission)){
                return true;
            }
            activity.requestPermissions(new String[]{permission},getRefCode(permission));
//            ActivityCompat.requestPermissions(activity,new String[]{permission},getRefCode(permission));
            return false;
        }else{
            return true;
        }
    }

    private static int getRefCode(String permission){
        switch (permission){
            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                return AppConstant.REQUEST_PERMISSION_STORAGE_CODE;
        }
        return -10086;
    }
}
