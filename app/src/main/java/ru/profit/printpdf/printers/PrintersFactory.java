package ru.profit.printpdf.printers;

public class PrintersFactory {

    public static ProfitPrinter getPrinter(PrinterType jobType) throws Exception {
        ProfitPrinter printer;

        switch (jobType) {
            case BLUETOOTH:
                printer = new ZebraPrinter();
                break;
            case WIFI:
                printer = new HPPrinter();
                break;
            default:
                throw new Exception("PrintersFactory - Unknown printer printerType");
        }
        return printer;
    }
}
