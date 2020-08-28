package com.lc.dear.activity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.lc.dear.R;
import com.lc.dear.utils.AppConstant;
import com.lc.dear.utils.HttpUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

    public class SplashActivity extends AppCompatActivity {

    private LinearLayout ll_container;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ll_container = (LinearLayout) View.inflate(this,R.layout.activity_splash,null);
        setContentView(ll_container);

        showBg();
        checkUpdate();
    }

    private void showBg(){
        switch (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)){
            case 14:
            case 15:
            case 16:
                ll_container.setBackgroundResource(R.drawable.splash_afternoon);
            case 17:
            case 18:
            case 19:
                ll_container.setBackgroundResource(R.drawable.splash_dusk);
                break;
            case 20:
            case 21:
            case 22:
            case 23:
                ll_container.setBackgroundResource(R.drawable.splash_night);
                break;
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
                ll_container.setBackgroundResource(R.drawable.splash_midnight);
                break;
            default:
                ll_container.setBackgroundResource(R.drawable.splash_daytime);
        }
    }

    private void enterHome(){
        startActivity(new Intent(this,HomeActivity.class));
        finish();
    }

    private void checkUpdate(){
        HttpUtil.get(HttpUtil.HttpParam.create("http://10.0.2.2:8080/public/dear_version.json",new HttpUtil.StringHttpListener(){

            @Override
            public void onFinish(String result) {
                if(TextUtils.isEmpty(result)){
                    enterHome();
                }else{
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                        String versionName = packageInfo.versionName;
                        long versionCode;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            versionCode = packageInfo.getLongVersionCode();
                        }else{
                            versionCode = packageInfo.versionCode;
                        }
                        if(jsonObject.getLong("versionCode")!=versionCode || !jsonObject.getString("versionName").equals(versionName)){
                            Intent intent=new Intent(getApplicationContext(),UpdateActivity.class);
                            intent.putExtra("versionCode",versionCode);
                            intent.putExtra("versionName",versionName);
                            intent.putExtra("versionDes",jsonObject.getString("versionDes"));
                            intent.putExtra("downloadUrl",jsonObject.getString("downloadUrl"));
                            intent.putExtra("enterActivity",HomeActivity.class);
                            startActivityForResult(intent, AppConstant.ACTIVITY_RESULT_UPDATE);
                        }else{
                            try {
                                Thread.sleep(2000);
                                enterHome();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (JSONException | PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                        enterHome();
                    }
                }
            }

            @Override
            public void onFailure() {
                enterHome();
            }

            @Override
            public void onError() {
                enterHome();
            }
        }));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode==AppConstant.ACTIVITY_RESULT_UPDATE){
            enterHome();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}