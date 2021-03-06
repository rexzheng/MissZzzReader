package com.zhao.myreader.application;


import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.zhao.myreader.base.BaseActivity;
import com.zhao.myreader.callback.ResultCallback;
import com.zhao.myreader.common.APPCONST;
import com.zhao.myreader.common.URLCONST;
import com.zhao.myreader.creator.DialogCreator;
import com.zhao.myreader.entity.UpdateInfo;
import com.zhao.myreader.util.CacheHelper;
import com.zhao.myreader.util.DownloadMangerUtils;
import com.zhao.myreader.util.HttpUtil;
import com.zhao.myreader.util.OpenFileHelper;
import com.zhao.myreader.util.TextHelper;
import com.zhao.myreader.util.UriFileUtil;
import com.zhao.myreader.webapi.CommonApi;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Created by zhao on 2016/10/20.
 */

public class MyApplication extends Application {

    private static Handler handler = new Handler();
    private static MyApplication application;
    private ExecutorService mFixedThreadPool;



    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
        HttpUtil.trustAllHosts();//信任所有证书
        mFixedThreadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());//初始化线程池

        BaseActivity.setCloseAntiHijacking(true);

    }




    public static Context getmContext() {
        return application;
    }


    public void newThread(Runnable runnable) {

        try {
            mFixedThreadPool.execute(runnable);
        } catch (Exception e) {
            e.printStackTrace();
            mFixedThreadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());//初始化线程池
            mFixedThreadPool.execute(runnable);
        }
    }

    public void shutdownThreadPool(){
        mFixedThreadPool.shutdownNow();
    }


    /*@TargetApi(26)
    private void createNotificationChannel() {

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.createNotificationChannel(new NotificationChannel("gxdw_push_and_im", "gxdw", NotificationManager.IMPORTANCE_DEFAULT));
    }*/


    /**
     * 主线程执行
     *
     * @param runnable
     */
    public static void runOnUiThread(Runnable runnable) {
        handler.post(runnable);
    }

    public static MyApplication getApplication() {
        return application;
    }


    private boolean isFolderExist(String dir) {
        File folder = Environment.getExternalStoragePublicDirectory(dir);
        return (folder.exists() && folder.isDirectory()) || folder.mkdirs();
    }

    /**
     * 获取app版本号
     *
     * @return
     */
    public static int getVersionCode() {
        try {
            PackageManager manager = application.getPackageManager();
            PackageInfo info = manager.getPackageInfo(application.getPackageName(), 0);

            return info.versionCode;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 获取app版本号(String)
     *
     * @return
     */
    public static String getStrVersionName() {
        try {
            PackageManager manager = application.getPackageManager();
            PackageInfo info = manager.getPackageInfo(application.getPackageName(), 0);

            return info.versionName;
        } catch (Exception e) {
            e.printStackTrace();
            return "1.0.0";
        }
    }

    /**
     * 检查更新
     */
    public static void checkVersion(Activity activity) {
        UpdateInfo updateInfo = (UpdateInfo) CacheHelper.readObject(APPCONST.FILE_NAME_UPDATE_INFO);
        int versionCode = getVersionCode();
        if (updateInfo != null) {
            if (updateInfo.getNewestVersionCode() > versionCode) {
                updateApp(activity, updateInfo.getDownLoadUrl(), versionCode);
            }
        }
    }

    /**
     * 检查更新
     */
    public static void checkVersionByServer(final Activity activity) {
        CommonApi.getNewestAppVersion(new ResultCallback() {
            @Override
            public void onFinish(Object o, int code) {
                int versionCode = getVersionCode();
                int newestVersion = Integer.valueOf((String)o) ;

                if (newestVersion > versionCode) {
                    updateApp(activity,URLCONST.method_downloadApp, versionCode);
                }

            }

            @Override
            public void onError(Exception e) {

            }
        });

    }

    /**
     * App自动升级
     *
     * @param activity
     * @param versionCode
     */
    public static void updateApp(final Activity activity, final String url, final int versionCode) {
        DialogCreator.createCommonDialog(activity, "发现新版本", null, "立即更新",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        TextHelper.showText("正在下载更新...");
                        DownloadMangerUtils.downloadFileOnNotificationByFinishListener(activity, APPCONST.UPDATE_APK_FILE_DIR,
                                "MissZzzReader_" + versionCode + ".apk", url, "大钊阅读更新下载",
                                new DownloadMangerUtils.DownloadCompleteListener() {
                                    @Override
                                    public void onFinish(Uri uri) {
                                        ((BaseActivity) activity).setDisallowAntiHijacking(true);//暂停防界面劫持
                                        OpenFileHelper.openFile(application, new File(UriFileUtil.getPath(application, uri)));
                                    }

                                    @Override
                                    public void onError(String s) {

                                    }
                                });
                    }
                });
    }

}
