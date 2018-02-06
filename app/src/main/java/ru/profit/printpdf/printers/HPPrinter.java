package ru.profit.printpdf.printers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class HPPrinter implements ProfitPrinter {

    private static final String EPRINT_HP_INTENT = "org.androidprinting.intent.action.PRINT";
    private static final String EPRINT_HP_FORMAT = "application/pdf";

    @Override
    public void print(Context context, Uri fileUri) {
        Intent intent = new Intent(EPRINT_HP_INTENT);
        intent.setDataAndType(fileUri, EPRINT_HP_FORMAT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    public String toString() {
        return "WiFi HP printer";
    }
}
