package com.lc.dear.utils;

import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public interface HttpUtil {

    static void get(HttpParam param){
        doHttp("GET",param);
    }

    static void post(HttpParam param){
        doHttp("POST",param);
    }

    static void doHttp(String method, HttpParam param){
        new Thread(){
            @Override
            public void run() {
                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL(param.url).openConnection();
                    connection.setConnectTimeout(param.connectTimeout);
                    connection.setReadTimeout(param.readTimeout);
                    connection.setRequestMethod(method);
                    for(Map.Entry<String,String> entry : param.header.entrySet()){
                        connection.setRequestProperty(entry.getKey(),entry.getValue());
                    }
                    if(connection.getResponseCode()==200){
                        Message message = Message.obtain();
                        message.what= HttpParam.LISTEN_SUCCESS;
                        if(param.download){
                            download(param,connection.getContentLength());
                        }else{
                            message.obj=connection.getInputStream();
                        }
                        param.sendMessage(message);
                    }else{
                        param.sendEmptyMessage(HttpParam.LISTEN_FAILURE);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    param.sendEmptyMessage(HttpParam.LISTEN_ERROR);
                }
            }
        }.start();
    }

    static void download(HttpParam param,int fileLength){
        int threadCount=param.threadCount<0?1:param.threadCount;
        int avg=fileLength/threadCount;
        HttpParam taskParam;
        int start,end;
        for(int i=1;i<=threadCount;i++){
            start=(i-1)*avg;
            end=i*avg-1;
            taskParam=new HttpParam();
            taskParam.url=param.url;
            taskParam.addRequestProperty("Range","bytes="+start+"-"+end);
            taskParam.inner=true;
            taskParam.setHttpListener(new SimpleHttpListener() {
                @Override
                public void onSuccess(InputStream inputStream) {
                    if(inputStream!=null){
                        try {

                        } catch (Exception e){
                            e.printStackTrace();
                        } finally {
                            try {
                                inputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
            get(taskParam);
        }
    }

    class HttpParam{
        private static final int LISTEN_SUCCESS=1;
        private static final int LISTEN_FAILURE=2;
        private static final int LISTEN_ERROR=3;
        private static final int LISTEN_PROGRESS=4;

        public String url;
        public int readTimeout=3000;
        public int connectTimeout=3000;
        public boolean inner=false;
        public Map<String,String> header=new HashMap<String,String>();
        private HttpListener httpListener;
        private Handler handler;

        public boolean download=false;
        public boolean downloadTask=false;
        public int threadCount=3;

        public HttpParam(){}

        public HttpParam(String url,HttpListener httpListener){
            this.url=url;
            this.setHttpListener(httpListener);
        }

        public void setHttpListener(HttpListener httpListener){
            if(!inner && httpListener!=null && handler==null){
                handler=new Handler(new Handler.Callback(){

                    @Override
                    public boolean handleMessage(@NonNull Message msg) {
                        exec(msg.what,msg.obj);
                        return false;
                    }
                });
            }
            this.httpListener=httpListener;
        }

        private void exec(int status,Object obj){
            switch (status){
                case LISTEN_SUCCESS:
                    if(httpListener!=null){
                        httpListener.onSuccess((InputStream) obj);
                    }
                    break;
                case LISTEN_FAILURE:
                    if(httpListener!=null){
                        httpListener.onFailure();
                    }
                    break;
                case LISTEN_ERROR:
                    if(httpListener!=null){
                        httpListener.onError();
                    }
                    break;
                case LISTEN_PROGRESS:
                    if(httpListener!=null){
                        httpListener.onProgress();
                    }
                    break;
            }
        }

        private void sendMessage(Message message){
            if(inner){
                exec(message.what,message.obj);
            }else if(handler!=null){
                handler.sendMessage(message);
            }
        }

        private void sendEmptyMessage(int status){
            if(inner){
                exec(status,null);
            }else if(handler!=null){
                handler.sendEmptyMessage(status);
            }
        }

        public void addRequestProperty(String key,String value){
            header.put(key,value);
        }

        public static HttpParam create(String url){
            return new HttpParam(url,null);
        }

        public static HttpParam create(String url,HttpListener httpListener){
            return new HttpParam(url,httpListener);
        }
    }

    interface HttpListener {
        void onSuccess(InputStream inputStream);
        void onFailure();
        void onError();
        void onProgress();
    }

    abstract class SimpleHttpListener implements HttpListener{
        @Override
        public void onFailure() {

        }

        @Override
        public void onError() {

        }

        @Override
        public void onProgress() {

        }
    }
}
