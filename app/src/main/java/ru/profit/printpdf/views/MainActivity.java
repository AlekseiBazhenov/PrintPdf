package ru.profit.printpdf.views;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import ru.profit.printpdf.App;
import ru.profit.printpdf.R;
import ru.profit.printpdf.logging.FileLoggingTree;
import ru.profit.printpdf.services.MainService;
import ru.profit.printpdf.utils.AppUtils;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private final List<Integer> blockedKeys = new ArrayList<>(Arrays.asList(
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP)
    );

    @Inject
    AppUtils appUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ((App) getApplication().getApplicationContext()).getUtilsComponent().inject(this);

        Timber.plant(new FileLoggingTree());

        if (!appUtils.serviceIsRunning(this, MainService.class)) {
            Intent intent = new Intent(MainActivity.this, MainService.class);
            startService(intent);
        }
        finish();
    }
}
