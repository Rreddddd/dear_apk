package com.lc.dear.activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.lc.dear.R;
import com.lc.dear.utils.HttpUtil;

import java.io.InputStream;

public class SplashActivity extends AppCompatActivity {

    private ImageView iv_bg;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initView();
        checkUpdate();
    }

    private void initView(){
        iv_bg=findViewById(R.id.iv_bg);
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