package ru.profit.printpdf.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Environment;

import org.vudroid.core.DecodeServiceBase;
import org.vudroid.core.codec.CodecPage;
import org.vudroid.pdfdroid.codec.PdfContext;

import java.util.ArrayList;

import ru.profit.printpdf.Config;
import ru.profit.printpdf.bitmap.BitmapConverter;

public class PdfUtil {

    public static ArrayList<String> getPages(Context context, Uri docUri) {
        DecodeServiceBase decodeService = new DecodeServiceBase(new PdfContext());
        decodeService.setContentResolver(context.getContentResolver());
        decodeService.open(docUri);

        int pageCount = decodeService.getPageCount();
        ArrayList<String> pngImages = new ArrayList<>();
        for (int i = 0; i < pageCount; i++) {
            CodecPage page = decodeService.getPage(i);
            RectF rectF = new RectF(0, 0, 1, 1);

            double scaleBy = Math.min(1000 / (double) page.getWidth(),
                    1000 / (double) page.getHeight());
            int with = (int) (page.getWidth() * scaleBy);
            int height = (int) (page.getHeight() * scaleBy);

            Bitmap bitmap = page.renderBitmap(with, height, rectF);

            String rootPath = Environment.getExternalStorageDirectory().toString();
            String pngDir = rootPath + Config.PNG_DIR;

            String fileName = System.currentTimeMillis() + ".png";
            pngImages.add(pngDir + "/" + fileName);

            BitmapConverter convertor = new BitmapConverter();
            convertor.convertBitmap(bitmap, pngDir + "/" + fileName);
        }
        return pngImages;
    }
}
