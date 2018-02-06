package ru.profit.printpdf.zebraprinter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.graphics.internal.ZebraImageAndroid;
import com.zebra.sdk.printer.PrinterStatus;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;
import com.zebra.sdk.printer.discovery.DiscoveredPrinterBluetooth;

import java.io.File;
import java.util.ArrayList;

import ru.profit.printpdf.Config;

public class ZebraPrintTask extends AsyncTask<ZebraPrintTask.PrintJob, String, Boolean> {

    private PrintJob[] jobs;
    private int currentJob;

    public static final String PDF_PAGES = "zebra:pdf_pages";
    public static final String KEY_JOB_TITLE = "zebra:print_job_title";

    private boolean isWaitingForPrinter = false;

    private BluetoothStateHolder bluetoothService;

    private Connection cachedConnection;

    public ZebraPrintTask(BluetoothStateHolder bluetoothService) {
        this.bluetoothService = bluetoothService;
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        listener.taskFinished(false);
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        super.onPostExecute(aBoolean);
        boolean allSuccess = true;
        for (PrintJob job : jobs) {
            if (job.getStatus() != PrintJob.Status.SUCCESS) {
                allSuccess = false;
                return;
            }
        }
        listener.taskFinished(allSuccess);
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        listener.taskUpdate(this);
    }

    @Override
    protected Boolean doInBackground(PrintJob... printJobs) {
        this.jobs = printJobs;

        currentJob = 0;

        try {
            while (currentJob < jobs.length) {
                if (this.isCancelled()) {
                    return false;
                }

                PrintJob current = jobs[currentJob];
                if (current.getStatus() == PrintJob.Status.ERROR) {
                    currentJob++;
                    continue;
                }

                if (!isPrinterAvailable() || isWaitingForPrinter) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {

                    }
                    if (!isPrinterAvailable()) {
                        continue;
                    }
                }

                try {
                    advanceJob(current);
                    this.publishProgress(null);
                    if (current.getStatus() == PrintJob.Status.SUCCESS) {
                        currentJob++;
                    }
                } catch (ConnectionException connectionIssue) {
                    connectionIssue.printStackTrace();
                    closeAndClearCachedConnectionHandle();
                    //TODO: Maybe try to re-open the connection handle once before completely
                    //expiring the attached printer?
                    signalPrinterUnavailable();
                } catch (Exception e) {
                    current.setJobToError(e.getMessage());
                    this.publishProgress(null);
                    currentJob++;
                }
            }
            return true;
        } finally {
            closeAndClearCachedConnectionHandle();
        }
    }

    private void closeAndClearCachedConnectionHandle() {
        if (cachedConnection != null) {
            if (cachedConnection.isConnected()) {
                try {
                    cachedConnection.close();
                } catch (ConnectionException e) {
                    e.printStackTrace();
                }
            }
        }
        cachedConnection = null;
    }

    private void signalPrinterUnavailable() {
        bluetoothService.SignalActivePrinterUnavailable();
    }

    private boolean isPrinterAvailable() {
        return bluetoothService.getActivePrinter() != null;
    }

    private void advanceJob(PrintJob job) throws ConnectionException {
        switch (job.getStatus()) {
            case PENDING:
                job.prepare();
                this.publishProgress(null);
            case PRINTING:
                attemptJobPrint(job);
                return;
        }
    }

    private void attemptJobPrint(PrintJob job) throws ConnectionException {
        Connection connection = getPrinterConnection();
        try {
            ZebraPrinter activePrinter = ZebraPrinterFactory.getInstance(connection);
            PrinterStatus status = activePrinter.getCurrentStatus();

            if (!status.isReadyToPrint) {
                isWaitingForPrinter = true;
                return;
            } else {
                isWaitingForPrinter = false;
            }

            Log.v("PrintPdf ZebraPrintTask", "Starting Print");
            printFiles(connection, activePrinter, job.getImages());
            Log.v("PrintPdf ZebraPrintTask", "Print submitted");

            job.setPrintSuccessful();
        } catch (ZebraPrinterLanguageUnknownException e) {
            throw wrap("Unrecognized language for template: ", e);
        }
    }

    private Connection getPrinterConnection() throws ConnectionException {
        DiscoveredPrinterBluetooth activePrinter = bluetoothService.getActivePrinter();
        if (activePrinter == null) {
            throw new ConnectionException("No Active Printer");
        }

        if (cachedConnection != null) {
            if (cachedConnection.isConnected()) {
                return cachedConnection;
            }

            cachedConnection = null;
        }
        cachedConnection = activePrinter.getConnection();
        cachedConnection.open();
        return cachedConnection;
    }

    private void printFiles(Connection connection,
                            ZebraPrinter activePrinter,
                            ArrayList<String> files) throws ConnectionException {
        try {
            for (String path : files) {
                Log.d("PrintPdf ZebraPrintTask", "printFiles - " + path);
                Bitmap bitmap = BitmapFactory.decodeFile(path);
//                activePrinter.printImage(new ZebraImageAndroid(bitmap), 0, 0, 1000, 1300, false);
//                activePrinter.printImage(new ZebraImageAndroid(bitmap), 0, 0, 800, 450, false);
                activePrinter.printImage(new ZebraImageAndroid(bitmap), 0, 0, 850, 650, false);
            }
        } catch (ConnectionException e) {
            e.printStackTrace();
        } finally {
            connection.close();

            // clear png directory
            String rootPath = Environment.getExternalStorageDirectory().toString();
            String pngDir = rootPath + Config.PNG_DIR;
            File directory = new File(pngDir);
            File[] images = directory.listFiles();
            for (File file : images) {
                boolean success = file.delete();
            }
        }
    }

    private static RuntimeException wrap(String message, Exception e) {
        e.printStackTrace();

        RuntimeException r = new RuntimeException(message + " | " + e.getMessage());
        r.initCause(e);
        return r;
    }

    PrintTaskListener listener;

    public void attachListener(PrintTaskListener listener) {
        this.listener = listener;
    }

    public void detachListener(PrintTaskListener listener) {
        this.listener = listener;
    }

    public boolean isWaiting() {
        return isWaitingForPrinter;
    }

    public int getCurrentJobNumber() {
        return currentJob;
    }

    public static class PrintJob {

        public String getErrorMessage() {
            return errorMessage;
        }

        public enum Status {
            PENDING("Ожидание"),
            PRINTING("Печать"),
            SUCCESS("Завершено"),
            ERROR("Ошибка");

            private final String value;

            Status(String value) {
                this.value = value;
            }

            public String getValue() {
                return value;
            }
        }

        ArrayList<String> images;

        int id;

        private Status status;
        private Bundle parameters;

        private String errorMessage;

        public PrintJob(int id, Bundle parameters) {
            this.id = id;
            this.parameters = parameters;
            this.status = Status.PENDING;
        }

        public int getJobId() {
            return id;
        }

        public String getDisplayName() {
            return parameters.getString(KEY_JOB_TITLE, "Print Job: " + id);
        }

        public Status getStatus() {
            return status;
        }

        public void setJobToError(String erroMessage) {
            this.status = Status.ERROR;
            this.errorMessage = erroMessage;
        }

        public void prepare() {
            images = parameters.getStringArrayList(PDF_PAGES);
            this.status = Status.PRINTING;
        }

        public void setPrintSuccessful() {
            this.status = Status.SUCCESS;
        }

        public ArrayList<String> getImages() {
            return images;
        }

        /**
         * @return True if this job's status will not change further. False if the job status may
         * still change.
         */
        public boolean isFinished() {
            switch (this.getStatus()) {
                case PENDING:
                    return false;
                case PRINTING:
                    return false;
                case SUCCESS:
                    return true;
                case ERROR:
                    return true;
            }
            return true;
        }
    }
}
