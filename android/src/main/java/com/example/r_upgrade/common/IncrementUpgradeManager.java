package com.example.r_upgrade.common;

import java.io.File;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Environment;
import android.util.Log;

import me.ele.patch.BsPatch;

public class IncrementUpgradeManager extends ContextWrapper {
    private static final String TAG = "r_upgrade.Increment";
    final private String oldApkPath;

    public IncrementUpgradeManager(Context base) {
        super(base);
        BsPatch.init(base);
        oldApkPath = base.getPackageResourcePath();
    }

    public String mixinAndGetNewApk(String patchPath) {
        File parentFile = this.getExternalFilesDir(null);
        File newApkFile = new File(parentFile, oldApkPath.substring(oldApkPath.lastIndexOf("/") + 1));
        try {
            File patchFile = new File(patchPath);
            if (!patchFile.exists()) {
                return null;
            }
            if (patchFile.length() == 0) {
                return null;
            }
            if (newApkFile.exists()) {
                newApkFile.delete();
            }
            newApkFile.createNewFile();
            RUpgradeLogger.get().d(TAG, "patchFilePath : " + patchPath);
            RUpgradeLogger.get().d(TAG, "mixinAndGetNewApk : " + newApkFile.getPath());
            BsPatch.workSync(oldApkPath,newApkFile.getPath(),patchPath);
            RUpgradeLogger.get().d(TAG, "apkPatchingSucceeded" + newApkFile.getPath());
            return newApkFile.getPath();
        } catch (Exception e) {
            RUpgradeLogger.get().d(TAG, "合成失败：");
            e.printStackTrace();
        }
        return null;
    }

}
