package ru.profit.printpdf.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.io.File;

import javax.inject.Inject;

import ru.profit.printpdf.App;
import ru.profit.printpdf.Config;
import ru.profit.printpdf.FoldersScanner;
import ru.profit.printpdf.R;
import ru.profit.printpdf.receivers.BarcodeReceiver;
import ru.profit.printpdf.utils.FileUtils;
import ru.profit.printpdf.views.MainActivity;
import timber.log.Timber;

public class MainService extends Service {

    @Inject
    FoldersScanner foldersScanner;

    private static final String BARCODE_SCAN_ACTION = "android.intent.ACTION_DECODE_DATA_2";

    private BarcodeReceiver barcodeReceiver = new BarcodeReceiver();

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("MainService onBind() not implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d("MainService", "onCreate");
        ((App) getApplication().getApplicationContext()).getScannerComponent().inject(this);

        registerReceiver(barcodeReceiver, new IntentFilter(BARCODE_SCAN_ACTION));

        startInForeground();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.log(Log.INFO, "Service started");

        FileUtils.createFolders(new String[]{
                Config.LOGS_DIR,
                Config.PDF_DIR,
                Config.PNG_DIR,
                Config.WIFI_PRINT_DIR,
                Config.BARCODE_DIR});

        createFileForBarcode();

        foldersScanner.start(this);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Timber.log(Log.INFO, "Service stopped");
        unregisterReceiver(barcodeReceiver);
        foldersScanner.stop();
    }

    /**
     * It is necessary that the system does not kill the service
     */
    private void startInForeground() {
        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                0);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1337, notification);
    }

    private void createFileForBarcode() { //todo in fileutils
        String dir = Environment.getExternalStorageDirectory() + Config.BARCODE_DIR;

        if (!FileUtils.folderIsEmpty(dir)) {
            return;
        }
        // TODO: 07.03.17 log
        File file = new File(dir + Config.BARCODE_FILE);
    }
}
