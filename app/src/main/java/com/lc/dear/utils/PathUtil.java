package com.lc.dear.utils;

import android.Manifest;
import android.app.Activity;

import java.io.File;

public class PathUtil {

    public static String getDownloadPath(Activity activity,String fileName){
        if(PermissionUtil.check(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            File file = activity.getExternalFilesDir(null);
            if(file!=null){
                file=new File(file,"download");
            }else{
                file=new File(activity.getCacheDir(),"download");
            }
            if(!file.exists()){
                if(!file.mkdir()){
                    return null;
                }
            }
            return new File(file,fileName).getPath();
        }
        return null;
    }

    public static String getFileName(String url){
        if(url!=null){
            int pos = Math.max(url.lastIndexOf("/"), url.lastIndexOf("\\"));
            if(pos<0){
                pos=0;
            }
            return url.substring(pos+1);
        }
        return null;
    }
}