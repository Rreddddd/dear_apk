package com.lc.dear.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.lc.dear.R;
import com.lc.dear.utils.HttpUtil;

import java.io.InputStream;
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

    private void checkUpdate(){
        HttpUtil.HttpParam httpParam = HttpUtil.HttpParam.create("http://10.0.2.2:8080/public/WeChatSetup.exe", new HttpUtil.HttpListener() {
            @Override
            public void onSuccess(InputStream inputStream) {
                Log.i("SplashActivity", "连接成功");
            }

            @Override
            public void onFailure() {
                Log.i("SplashActivity", "连接失败");
            }

            @Override
            public void onError() {
                Log.i("SplashActivity", "连接错误");
            }

            @Override
            public void onProgress() {
                Log.i("SplashActivity", "下载中。。。。。。");
            }
        });
        httpParam.download=true;
        HttpUtil.get(httpParam);
    }
}