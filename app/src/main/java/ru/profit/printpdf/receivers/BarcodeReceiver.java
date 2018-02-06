package ru.profit.printpdf.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

import ru.profit.printpdf.Config;

public class BarcodeReceiver extends BroadcastReceiver {

    // in the settings of handheld data terminal
    private static final String INTENT = "android.intent.ACTION_DECODE_DATA_2";
    private static final String EXTRAS_BARCODE = "barcode_string";

    private static long previousBarcodeTime;

    @Override
    public void onReceive(Context context, Intent intent) {
        String barcode = intent.getStringExtra(EXTRAS_BARCODE);
        Log.d("BarcodeReceiver", "onReceive code " + barcode);
        long mills = Calendar.getInstance().getTimeInMillis();
        long diff = mills - previousBarcodeTime;
        Log.d("BarcodeReceiver", "onReceive diff " + diff);
        previousBarcodeTime = mills;
        if (diff > 1000) { // hack. Дважды приходит событие при сканировании штрих-кода. Проверяем разницу во времени между ними
            writeInFile(barcode);
        }
    }

    private void writeInFile(String barcode) {
        String dir = Environment.getExternalStorageDirectory() + Config.BARCODE_DIR;

        try {
            FileWriter writer = new FileWriter(dir + Config.BARCODE_FILE, true);
            BufferedWriter bufferWriter = new BufferedWriter(writer);
            bufferWriter.write(barcode + ";");
            bufferWriter.close();
        } catch (IOException e) {
            Log.e("BarcodeReceiver", e.getMessage());
        }
    }
}
