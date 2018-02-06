package ru.profit.printpdf.di;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import ru.profit.printpdf.FoldersScanner;
import ru.profit.printpdf.utils.AppUtils;
import ru.profit.printpdf.utils.FileUtils;
import ru.profit.printpdf.utils.PdfUtil;

@Module
public class UtilsModule {

    @Provides
    @Singleton
    public AppUtils provideAppUtils() {
        return new AppUtils();
    }

    @Provides
    @Singleton
    public FileUtils provideFileUtils() {
        return new FileUtils();
    }

    @Provides
    @Singleton
    public PdfUtil providePdfUtils() {
        return new PdfUtil();
    }
}
