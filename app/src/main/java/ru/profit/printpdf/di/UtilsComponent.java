package ru.profit.printpdf.di;

import javax.inject.Singleton;

import dagger.Component;
import ru.profit.printpdf.services.MainService;
import ru.profit.printpdf.views.MainActivity;

@Component(modules = {UtilsModule.class})
@Singleton
public interface UtilsComponent {
    void inject(MainActivity activity);
}
