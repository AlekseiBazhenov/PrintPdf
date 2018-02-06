package ru.profit.printpdf.di;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import ru.profit.printpdf.FoldersScanner;

@Module
public class ScannerModule {

    @Provides
    @Singleton
    public FoldersScanner provideScanner() {
        return new FoldersScanner();
    }
}
