package ru.profit.printpdf;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import ru.profit.printpdf.printers.PrinterType;
import ru.profit.printpdf.printers.PrintersFactory;
import ru.profit.printpdf.printers.ProfitPrinter;
import ru.profit.printpdf.utils.AppUtils;
import ru.profit.printpdf.utils.FileUtils;
import timber.log.Timber;

public class FoldersScanner {

    @Inject
    AppUtils appUtils;

    private String pdfDir;
    private String wifiPrintDir;
    private Context context;

    private ScheduledExecutorService service;
    private ScheduledFuture future;

    private boolean enablePrinting = true;

    public FoldersScanner() {
        service = Executors.newSingleThreadScheduledExecutor();
    }


    /**
     * Start folders scanning.
     *
     * @param context the service's context
     */
    public void start(Context context) {
        this.context = context;

        String rootPath = Environment.getExternalStorageDirectory().toString();
//        на всех тсд rootPath должен быть /storage/sdcard0 (прописывается в 1С для выгрузки файлов!!!)
        pdfDir = rootPath + Config.PDF_DIR;
        wifiPrintDir = rootPath + Config.WIFI_PRINT_DIR;

        future = service.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                scanForBluetoothPrint();
                scanForWifiPrint();
            }
        }, 0, 2, TimeUnit.SECONDS);
    }


    /**
     * Stop scanning onDestroy service.
     */
    public void stop() {
        if (future != null && !future.isCancelled()) {
            future.cancel(true);
        }
    }

    private void scanForBluetoothPrint() {
        File[] files = FileUtils.filesInFolder(pdfDir);
        if (files.length == 0) {
            return;
        }
        Uri pdfUri = Uri.fromFile(new File(files[0].toURI()));

        PdfDocument pdfDocument = new PdfDocument();
        pdfDocument.printerType = PrinterType.BLUETOOTH;
        pdfDocument.docUri = pdfUri;

        onFindPdfFile(pdfDocument);

        for (File file : files) {
            boolean isDeleted = file.delete();
        }
    }

    private void scanForWifiPrint() {
        File[] files = FileUtils.filesInFolder(wifiPrintDir);
        if (files.length == 0) {
            return;
        }

        Uri pdfUri = Uri.fromFile(new File(files[0].toURI()));

        // turn on wifi
        AppUtils.changeWiFiState(context, true);

        if (enablePrinting) {
            PdfDocument pdfDocument = new PdfDocument();
            pdfDocument.printerType = PrinterType.WIFI;
            pdfDocument.docUri = pdfUri;
            onFindPdfFile(pdfDocument);

            enablePrinting = false;
        }

        if (AppUtils.hpEprintOnTop(context)) {
            return;
        }

        // do if gone from eprint
        for (File file : files) {
            boolean isDeleted = file.delete();
        }

        // turn off wifi
        AppUtils.changeWiFiState(context, false);
        enablePrinting = true;
    }


    /**
     * Starts when pdf file found in folders.
     *
     * @param pdfDocument the pdf document
     */
    private void onFindPdfFile(PdfDocument pdfDocument) {
        Timber.log(Log.INFO, "File found: " + pdfDocument.docUri);
        try {
            ProfitPrinter printer = PrintersFactory.getPrinter(pdfDocument.printerType);
            Timber.log(Log.INFO, "Document sent to " + printer.toString());
            printer.print(context, pdfDocument.docUri);
        } catch (Exception e) {
            Timber.log(Log.ERROR, e.getMessage());
        }
    }
}