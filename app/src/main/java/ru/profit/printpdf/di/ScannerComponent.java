package ru.profit.printpdf.di;

import javax.inject.Singleton;

import dagger.Component;
import ru.profit.printpdf.services.MainService;

@Component(modules = {ScannerModule.class})
@Singleton
public interface ScannerComponent {
    void inject(MainService mainService);
}
