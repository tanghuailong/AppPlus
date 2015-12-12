package com.gudong.appkit.ui.activity;

import android.os.Bundle;

import com.gudong.appkit.App;
import com.gudong.appkit.R;
import com.gudong.appkit.dao.AppEntity;
import com.gudong.appkit.dao.AppInfoEngine;
import com.gudong.appkit.dao.DBHelper;
import com.gudong.appkit.event.EEvent;
import com.gudong.appkit.event.EventCenter;
import com.gudong.appkit.process.ProcessManager;
import com.gudong.appkit.ui.control.NavigationManager;
import com.gudong.appkit.utils.FileUtil;
import com.gudong.appkit.utils.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class SplashActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStatusBarColorRes(R.color.colorPrimary);
        checkAndUpdateLocalDb();
        checkExportDirectoryIsChange();
        gotoMainActivity();
    }

    private void gotoMainActivity() {
        //delay 1500 mill and enter MainActivity
        getWindow().getDecorView().postDelayed(new Runnable() {
            @Override
            public void run() {
                NavigationManager.gotoMainActivityFromSplashView(SplashActivity.this);
            }
        },1500);
    }

    private void checkAndUpdateLocalDb(){
        final long startTime = System.currentTimeMillis();
        //TODO use RxJava
        //check and update local db data
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<AppEntity> list = AppInfoEngine.getInstance().getInstalledAppList();
                //AppInfoEngine.getInstance(getApplicationContext()).getRunningProcesses();
//                ProcessManager.getRunningAppEntity(getApplication());
                ProcessManager.getRunningAppProcesses();
                for (AppEntity entity : list) {
                    if (!DBHelper.installedAppIsExistInLocalDB(entity.getPackageName())) {
                        //insert installed app entity to local db
                        App.sDb.insert(entity);
                    }
                }
                List<AppEntity>listDB = App.sDb.query(AppEntity.class);
                //
                for(AppEntity entity : listDB){
                    if(!list.contains(entity)){
                        App.sDb.delete(entity);
                    }
                }
                Logger.i("prepare all installed data finish now notify AppListFragment ");
                EventCenter.getInstance().triggerEvent(EEvent.PREPARE_FOR_ALL_INSTALLED_APP_FINISH,null);
                long endTime = System.currentTimeMillis();
                Logger.i("checkAndUpdateLocalDb take "+(endTime-startTime)+" millis");
            }
        }).start();
    }

    /**
     * check the directory which used to store export apk file has some file ,if the old dir has
     * apk file ,move all file to new dir folder.
     * this change is begin with version 3.0, and the new folder name is AppPlus
     */
    private void checkExportDirectoryIsChange() {
        if (!FileUtil.isSdCardOnMounted()){
            return;
        }
        final File oldExportDir = new File(FileUtil.getSDPath(), FileUtil.KEY_EXPORT_DIR_OLDER);
        //user has not use older dir name,this condition is good , we need not deal
        if (!oldExportDir.exists()) {
            return;
        }
        final File[]files = oldExportDir.listFiles();
        if(files.length<=0){
            oldExportDir.delete();
            return;
        }
        Logger.i("发现"+files.length+"个文件");
        final File nowExportDir = FileUtil.createDir(FileUtil.getSDPath(), FileUtil.KEY_EXPORT_DIR);
        new Thread(new Runnable() {
            @Override
            public void run() {
                for(File file:files){
                    File dest = new File(nowExportDir,file.getName());
                    try {
                        FileUtil.copyFileUsingFileChannels(file,dest);
                        file.delete();
                        Logger.i("拷贝文件"+file.getName()+"完成 删除文件");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Logger.i("拷贝所有文件完成 删除文件夹");
                oldExportDir.delete();
            }
        }).start();

    }

    @Override
    protected int initLayout() {
        //splash layout is set by Theme in AndroidManifest file
        return -1;
    }
}
