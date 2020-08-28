package com.lc.dear.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.lc.dear.R;
import com.lc.dear.utils.AppConstant;
import com.lc.dear.utils.HttpUtil;
import com.lc.dear.utils.PathUtil;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;

public class UpdateActivity extends AppCompatActivity {

    private Button btn_confirm;
    private Button btn_cancel;
    private ImageView iv_shadow;
    private TextView tv_progress_out;
    private TextView tv_progress_in;
    private TextView tv_progress_text;
    private boolean mClick=false;
    private long beforeTime;
    private int beforeProgress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update);

        initView();
    }

    private void initView(){
        iv_shadow=findViewById(R.id.iv_shadow);
        AlphaAnimation alphaAnimation = new AlphaAnimation(0.6f, 0.4f);
        alphaAnimation.setDuration(800);
        alphaAnimation.setRepeatCount(AlphaAnimation.INFINITE);
        alphaAnimation.setRepeatMode(AlphaAnimation.REVERSE);
        iv_shadow.startAnimation(alphaAnimation);
        Intent intent = getIntent();
        TextView tv_des=findViewById(R.id.tv_des);
        tv_des.setText(intent.getStringExtra("versionDes"));
        btn_confirm=findViewById(R.id.btn_confirm);
        btn_confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDownload();
            }
        });
        btn_cancel=findViewById(R.id.btn_cancel);
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(AppConstant.ACTIVITY_RESULT_UPDATE);
                finish();
            }
        });
        tv_progress_out=findViewById(R.id.tv_progress_out);
        tv_progress_in=findViewById(R.id.tv_progress_in);
        tv_progress_text=findViewById(R.id.tv_progress_text);
    }

    private void startDownload(){
        if(!mClick){
            mClick=true;
            iv_shadow.clearAnimation();
            iv_shadow.setVisibility(View.INVISIBLE);
            btn_confirm.setVisibility(View.INVISIBLE);
            btn_cancel.setVisibility(View.VISIBLE);
            tv_progress_out.setVisibility(View.VISIBLE);
            tv_progress_out.setText("0%");
            tv_progress_in.setVisibility(View.VISIBLE);
            tv_progress_text.setVisibility(View.VISIBLE);
            tv_progress_text.setText(String.format(getString(R.string.activity_update_progress),"xx"));
            int width=tv_progress_out.getWidth()-50;

            String downloadUrl = getIntent().getStringExtra("downloadUrl");
            String downloadPath = PathUtil.getDownloadPath(UpdateActivity.this,PathUtil.getFileName(downloadUrl));
            if(downloadPath!=null){
                final int[] cache={-1,-1};
                HttpUtil.HttpParam httpParam = HttpUtil.HttpParam.create(downloadUrl, new HttpUtil.HttpListener() {
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
                    public void onProgress(int current,int fileLength) {
                        long nowTime = System.currentTimeMillis();
                        if(beforeTime==0L){
                            beforeTime=nowTime;
                            beforeProgress=current;
                        }else{
                            long minus = nowTime - beforeTime;
                            if(minus>=1000){
                                tv_progress_text.setText(String.format(getString(R.string.activity_update_progress),((fileLength-current) / (current-beforeProgress)+1)+""));
                                beforeTime=nowTime;
                                beforeProgress=current;

                                double percent=1.0*current/fileLength;
                                tv_progress_out.setText(String.format(getString(R.string.percentSuffix),BigDecimal.valueOf(Math.floor(percent*100)).intValue()+""));
                                ViewGroup.LayoutParams layoutParams = tv_progress_in.getLayoutParams();
                                layoutParams.width=BigDecimal.valueOf(width*percent).intValue();
                                tv_progress_in.setLayoutParams(layoutParams);
                            }
                        }
                        double percent=1.0*current/fileLength;
                        int nowPercent = BigDecimal.valueOf(Math.floor(percent * 100)).intValue();
                        if(cache[0]<0 || nowPercent!=cache[0]){
                            tv_progress_out.setText(String.format(getString(R.string.percentSuffix),nowPercent+""));
                            cache[0]=nowPercent;
                        }
                        int nowWidth = BigDecimal.valueOf(width*percent).intValue();
                        if(cache[1]<0 || nowWidth!=cache[1]){
                            ViewGroup.LayoutParams layoutParams = tv_progress_in.getLayoutParams();
                            layoutParams.width=nowWidth;
                            tv_progress_in.setLayoutParams(layoutParams);
                            cache[1]=nowWidth;
                        }
                    }

                    @Override
                    public void onFinish() {
                        tv_progress_text.setVisibility(View.INVISIBLE);
                        File apk=new File(downloadPath);
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_INSTALL_PACKAGE);
                        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            Uri uri = FileProvider.getUriForFile(getApplicationContext(), "com.lc.dear", apk);
                            intent.setDataAndType(uri, "application/vnd.android.package-archive");
                        }else{
                            intent.setDataAndType(Uri.fromFile(apk),"application/vnd.android.package-archive");
                        }
                        startActivity(intent);
                    }
                });
                httpParam.download=true;
                httpParam.downloadPath= downloadPath;
                HttpUtil.get(httpParam);
            }else{
                mClick=false;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode==AppConstant.REQUEST_PERMISSION_STORAGE_CODE){
            if(grantResults[0]==-1){
                tv_progress_text.setText("需要打开储存权限");
            }else{
                startDownload();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
