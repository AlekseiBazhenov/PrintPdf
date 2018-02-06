package ru.profit.printpdf.printers;

import android.content.Context;
import android.net.Uri;

public interface ProfitPrinter {

    void print(Context context, Uri fileUri);
}
