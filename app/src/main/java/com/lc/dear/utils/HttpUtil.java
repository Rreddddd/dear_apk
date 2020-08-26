package com.lc.dear.utils;

import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public abstract class HttpUtil {

    public static void get(HttpParam param){
        doHttp("GET",param);
    }

    public static void post(HttpParam param){
        doHttp("POST",param);
    }

    public static void doHttp(String method, HttpParam param){
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
                    if(connection.getResponseCode()==(param.downloadTask?206:200)){
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

    private static File[] getProgressFiles(String downloadPath){
        File cacheDir = new File("/data/data/com.lc.dear/cache");
        int hashCode = downloadPath.hashCode();
        return cacheDir.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.contains(hashCode+"");
            }
        });
    }

    private static int[] getCurrentProgress(int threadCount,String downloadPath){
        int[] result=new int[threadCount];
        File[] files = getProgressFiles(downloadPath);
        if(files!=null && files.length==threadCount){
            for(int i=0;i<threadCount;i++){
                BufferedReader br=null;
                try {
                    br=new BufferedReader(new FileReader(files[i]));
                    StringBuilder sb=new StringBuilder();
                    String temp;
                    while ((temp=br.readLine())!=null){
                        sb.append(temp);
                    }
                    result[i]+=Integer.parseInt(sb.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if(br!=null){
                        try {
                            br.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return result;
    }

    private static void download(HttpParam param,int fileLength){
        int threadCount=param.threadCount<0?1:param.threadCount;
        int avg=fileLength/threadCount;
        HttpParam taskParam;
        int[] current=getCurrentProgress(threadCount,param.downloadPath);
        boolean initState=true;
        for(int record : current){
            if(record>0){
                initState=false;
                break;
            }
        }
        File downloadFile=new File(param.downloadPath);
        if(initState && downloadFile.exists()){
            downloadFile.delete();
        }
        for(int i=1;i<=threadCount;i++){
            final int taskNumber=i-1;
            final int start,end;
            start=(i-1)*avg+current[taskNumber];
            if(i==threadCount){
                end=fileLength;
            }else{
                end=i*avg-1;
            }
            if(start>=end){
                continue;
            }
            taskParam=new HttpParam();
            taskParam.url=param.url;
            taskParam.addRequestProperty("Range","bytes="+start+"-"+end);
            taskParam.inner=true;
            taskParam.download=false;
            taskParam.downloadTask=true;
            taskParam.setHttpListener(new HttpListener() {
                @Override
                public void onSuccess(InputStream inputStream) {
                    if(inputStream!=null){
                        RandomAccessFile outputStream=null;
                        RandomAccessFile randomAccessFile=null;
                        try {
                            randomAccessFile = new RandomAccessFile(downloadFile, "rw");
                            randomAccessFile.seek(start);
                            byte[] buffer=new byte[1024*128];
                            int len,count;
                            Message message;
                            Map<String,Integer> property;
                            while((len=inputStream.read(buffer,0,buffer.length))!=-1){
                                current[taskNumber]+=len;
                                outputStream=new RandomAccessFile("/data/data/com.lc.dear/cache/doanload_progress_" + param.downloadPath.hashCode()+"_"+taskNumber+".json","rwd");
                                outputStream.write((current[taskNumber]+"").getBytes());
                                outputStream.close();
                                outputStream=null;
                                randomAccessFile.write(buffer,0,len);
                                message = Message.obtain();
                                message.what=HttpParam.LISTEN_PROGRESS;
                                property=new HashMap<String,Integer>();
                                count=0;
                                for(int selfCount : current){
                                    count+=selfCount;
                                }
                                property.put("current",count);
                                property.put("fileLength",fileLength);
                                message.obj=property;
                                param.sendMessage(message);
                            }
                            synchronized (param){
                                count=0;
                                for(int selfCount : current){
                                    count+=selfCount;
                                }
                                if(count==fileLength){
                                    File[] progressFiles = getProgressFiles(param.downloadPath);
                                    if(progressFiles!=null && progressFiles.length>0){
                                        for(File progressFile : progressFiles){
                                            progressFile.delete();
                                        }
                                    }
                                    param.sendEmptyMessage(HttpParam.LISTEN_FINISH);
                                }
                            }
                        } catch (Exception e){
                            e.printStackTrace();
                        } finally {
                            try {
                                inputStream.close();
                                inputStream=null;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            if(outputStream!=null){
                                try {
                                    outputStream.close();
                                    outputStream=null;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            if(randomAccessFile!=null){
                                try {
                                    randomAccessFile.close();
                                    randomAccessFile=null;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }

                @Override
                public void onFailure() {
                    param.sendEmptyMessage(HttpParam.LISTEN_FAILURE);
                }

                @Override
                public void onError() {
                    param.sendEmptyMessage(HttpParam.LISTEN_ERROR);
                }

                @Override
                public void onProgress(int current, int fileLength) {

                }

                @Override
                public void onFinish() {

                }
            });
            get(taskParam);
        }
    }

    public static class HttpParam{
        private static final int LISTEN_SUCCESS=1;
        private static final int LISTEN_FAILURE=2;
        private static final int LISTEN_ERROR=3;
        private static final int LISTEN_PROGRESS=4;
        private static final int LISTEN_FINISH=5;

        public String url;
        public int readTimeout=3000;
        public int connectTimeout=3000;
        public boolean inner=false;
        public Map<String,String> header=new HashMap<String,String>();
        private HttpListener httpListener;
        private Handler handler;

        public String downloadPath;
        public boolean download=false;
        private boolean downloadTask=false;
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
                        Map<String, Integer> property = (HashMap<String, Integer>) obj;
                        if(property!=null){
                            Integer current = property.get("current");
                            Integer fileLength = property.get("fileLength");
                            if(current!=null && fileLength!=null){
                                httpListener.onProgress(current,fileLength);
                            }
                        }
                    }
                    break;
                case LISTEN_FINISH:
                    if(httpListener!=null){
                        httpListener.onFinish();
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

    public interface HttpListener {
        void onSuccess(InputStream inputStream);
        void onFailure();
        void onError();
        void onProgress(int current,int fileLength);
        void onFinish();
    }
}
