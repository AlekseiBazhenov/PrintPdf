package ru.profit.printpdf.printers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import java.util.ArrayList;

import ru.profit.printpdf.utils.PdfUtil;
import ru.profit.printpdf.zebraprinter.PrintReceiverActivity;
import ru.profit.printpdf.zebraprinter.ZebraPrintTask;

public class ZebraPrinter implements ProfitPrinter {

    @Override
    public void print(Context context, Uri fileUri) {
        ArrayList<String> files = PdfUtil.getPages(context, fileUri);
        Bundle jobTitles = new Bundle();

        ArrayList<String> titles = new ArrayList<>();
        titles.add("bundle1");
        jobTitles.putStringArrayList(PrintReceiverActivity.KEY_BUNDLE_LIST, titles);

        Bundle jobs = new Bundle();
        jobs.putString(ZebraPrintTask.KEY_JOB_TITLE, "Статус:");
        jobs.putStringArrayList(ZebraPrintTask.PDF_PAGES, files);

        Intent intent = new Intent(context, PrintReceiverActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtras(jobTitles);
        intent.putExtra("bundle1", jobs);
        context.getApplicationContext().startActivity(intent);
    }

    @Override
    public String toString() {
        return "Bluetooth Zebra printer";
    }
}
