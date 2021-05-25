package com.example.r_upgrade.common;

import android.content.Context;
import android.content.ContextWrapper;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import io.flutter.util.PathUtils;


public class HotUpgradeManager extends ContextWrapper {
    private static final String TAG = "HotUpgradeManager";
    private static final String FLUTTER_ASSETS = "flutter_assets";

    public HotUpgradeManager(Context context) {
        super(context);
    }

    private File getFlutterAssets() {
        return new File(PathUtils.getDataDirectory(this));
    }

    private File getHotAssets() {
        File file;
        file = new File(getFlutterAssets(), System.currentTimeMillis() + ".zip");
        RUpgradeLogger.get().d(TAG,  "Hot Assets Path : " + file.getPath());
        if (!file.exists()) {
            try {
                boolean isSuccess = file.createNewFile();
                return isSuccess ? file : null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    private void deleteFlutterAssets() {
        RUpgradeLogger.get().d(TAG,  "deleteFlutterAssets() : deleting flutter assets...");
        File file = new File(getFlutterAssets(), FLUTTER_ASSETS);
        if (file.exists()) {
            if (file.isDirectory()) {
                for (File item : file.listFiles()) {
                    if (item.exists()) {
                        boolean result = item.delete();
                        RUpgradeLogger.get().d(TAG,  "deleteFlutterAssets() : deleting flutter " + item.getPath() + ' ' + result);
                    }
                }
            }
            boolean result = file.delete();
            RUpgradeLogger.get().d(TAG,  "deleteFlutterAssets() : deleting flutter " + file.getPath() + ' ' + result);
        }
    }

    public Boolean hotUpgrade(Uri uri) {
        //获取文件流
        try {
            //复制下载的文件到资源文件中
            ParcelFileDescriptor descriptor = getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = descriptor.getFileDescriptor();
            if (fileDescriptor == null) return false;
            FileInputStream stream = new FileInputStream(fileDescriptor);
            File zipFile = getHotAssets();
            FileOutputStream outputStream = new FileOutputStream(zipFile);
            byte[] buffer = new byte[1024];
            int byteRead;
            while (-1 != (byteRead = stream.read(buffer))) {
                outputStream.write(buffer, 0, byteRead);
            }
            stream.close();
            outputStream.flush();
            outputStream.close();

            deleteFlutterAssets();
            unZipFile(zipFile.getPath(), getFlutterAssets().getPath() + File.separator + FLUTTER_ASSETS, true);

            File file = new File(getFlutterAssets(), FLUTTER_ASSETS);
            if (file.isDirectory()){
                final File[] files = file.listFiles();
                for (int i = 0; i < files.length; i++) {
                   File fileL = files[i];
                    RUpgradeLogger.get().d(TAG,  "hotUpgrade() : new file created " + fileL.getPath());
                }
            }
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * @param archive       解压文件得路径
     * @param decompressDir 解压文件目标路径
     * @param isDeleteZip   解压完毕是否删除解压文件
     * @throws IOException
     */
    public static void unZipFile(String archive, String decompressDir, boolean isDeleteZip) throws IOException {
        RUpgradeLogger.get().d(TAG,  "unZipFile() : unzipping files ");
        RUpgradeLogger.get().d(TAG,  "unZipFile() : " + archive);
        RUpgradeLogger.get().d(TAG,  "unZipFile() : " + decompressDir);
        BufferedInputStream bi;
        ZipFile zf = new ZipFile(archive);
        Enumeration e = zf.entries();
        while (e.hasMoreElements()) {
            ZipEntry ze2 = (ZipEntry) e.nextElement();
            String entryName = ze2.getName();
            String path = decompressDir + "/" + entryName;
            if (ze2.isDirectory()) {
                File decompressDirFile = new File(path);
                if (!decompressDirFile.exists()) {
                    decompressDirFile.mkdirs();
                }
            } else {
                if (decompressDir.endsWith(".zip")) {
                    decompressDir = decompressDir.substring(0, decompressDir.lastIndexOf(".zip"));
                }
                File fileDirFile = new File(decompressDir);
                if (!fileDirFile.exists()) {
                    fileDirFile.mkdirs();
                }
                RUpgradeLogger.get().d(TAG,  "unZipFile() : unzipping " + fileDirFile.getPath());
                String substring = entryName.substring(entryName.lastIndexOf("/") + 1, entryName.length());
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(decompressDir + "/" + substring));
                bi = new BufferedInputStream(zf.getInputStream(ze2));
                byte[] readContent = new byte[1024];
                int readCount = bi.read(readContent);
                while (readCount != -1) {
                    bos.write(readContent, 0, readCount);
                    readCount = bi.read(readContent);
                }
                bos.close();
            }
        }
        zf.close();
        RUpgradeLogger.get().d(TAG,  "unZipFile() : finished");
        if (isDeleteZip) {
            File zipFile = new File(archive);
            if (zipFile.exists() && zipFile.getName().endsWith(".zip")) {
                zipFile.delete();
                RUpgradeLogger.get().d(TAG,  "unZipFile() : deleting zip file " + zipFile.getPath());
            }
        }
    }
}
