package ru.profit.printpdf.utils;

import android.os.Environment;

import java.io.File;

// TODO: 07.03.17 привести в порядок работу с файлами в проекте
public class FileUtils {

    public static void createFolders(String[] dirs) {
        for (String dir : dirs) {
            File folder = new File(Environment.getExternalStorageDirectory() + dir);
            if (!folder.exists()) {
                boolean success = folder.mkdir();
                // TODO: 07.03.17 log
            }
        }
    }

    public static File[] filesInFolder(String dir) {
        File directory = new File(dir);
        return directory.listFiles();
    }

    public static boolean folderIsEmpty(String dir) {
        return filesInFolder(dir).length == 0;
    }
}
