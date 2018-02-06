package ru.profit.printpdf.zebraprinter;

public interface PrintTaskListener {
    void taskUpdate(ZebraPrintTask task);

    void taskFinished(boolean taskSuccesful);
}
