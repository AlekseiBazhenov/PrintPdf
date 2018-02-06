package ru.profit.printpdf;

import android.app.Application;

import ru.profit.printpdf.di.DaggerScannerComponent;
import ru.profit.printpdf.di.DaggerUtilsComponent;
import ru.profit.printpdf.di.ScannerComponent;
import ru.profit.printpdf.di.ScannerModule;
import ru.profit.printpdf.di.UtilsComponent;
import ru.profit.printpdf.di.UtilsModule;


public class App extends Application {

    private ScannerComponent scannerComponent;
    private UtilsComponent utilsComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        scannerComponent = DaggerScannerComponent.builder()
                .scannerModule(new ScannerModule())
                .build();

        utilsComponent = DaggerUtilsComponent.builder()
                .utilsModule(new UtilsModule())
                .build();
    }

    public ScannerComponent getScannerComponent() {
        return scannerComponent;
    }

    public UtilsComponent getUtilsComponent() {
        return utilsComponent;
    }
}
