package ru.profit.printpdf.zebraprinter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.zebra.sdk.printer.discovery.DiscoveredPrinterBluetooth;

import java.util.ArrayList;

import ru.profit.printpdf.R;

public class PrinterSearchingActivity extends AppCompatActivity implements BluetoothStateHolder.BluetoothStateListener {

    public static final String RETURN_WHEN_SELECTED = "return_on_select";

    boolean returnWhenSelected;

    BluetoothStateHolder bluetoothService;
    boolean springloadDiscoveryResult = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_printer_searching);

        returnWhenSelected = this.getIntent().getBooleanExtra(RETURN_WHEN_SELECTED, false);

        this.findViewById(R.id.discover_printers).
                setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View v) {
                                           springloadDiscoveryResult = true;
                                           bluetoothService.startBluetoothDiscovery();
                                       }
                                   }

                );

        this.findViewById(R.id.view_printers)
                .setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            launchPrinterSelect(bluetoothService.getDiscoveredPrinters());
                                        }
                                    }

                );

        ((CheckBox) this.findViewById(R.id.cbx_view_details)).setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        findViewById(R.id.connect_advanced_pane).setVisibility(isChecked ? View.VISIBLE : View.INVISIBLE);
                    }
                }
        );

        this.findViewById(R.id.connect_btn_scan_again).
                setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View v) {
                                           springloadDiscoveryResult = true;
                                           bluetoothService.startBluetoothDiscovery();
                                       }
                                   }

                );

    }

    @Override
    protected void onResume() {
        super.onResume();
        springloadDiscoveryResult = returnWhenSelected;
        BluetoothStateHolder.attachContextListener(this, this);

        if (bluetoothService.getActivePrinter() != null && returnWhenSelected) {
            done();
            return;
        }

        if (!bluetoothService.inDiscovery()) {
            updateCurrentPrinterView();
            if (springloadDiscoveryResult) {
                ArrayList<DiscoveredPrinterBluetooth> printers = bluetoothService.getDiscoveredPrinters();
                if (printers.size() > 0) {
                    this.launchPrinterSelect(printers);
                }
            }
        }
    }

    @Override
    public void attachStateHolder(BluetoothStateHolder bluetoothStateHolder) {
        this.bluetoothService = bluetoothStateHolder;
    }


    @Override
    protected void onPause() {
        super.onPause();
        bluetoothService.detachStateListener(this);
    }

    private void updateCurrentPrinterView() {
        if (isFinishing()) {
            return;
        }

        ProgressBar progressBar = (ProgressBar) this.findViewById(R.id.connect_progress_bar);

        TextView advancedTextView = (TextView) this.findViewById(R.id.active_printer);
        TextView humanReadable = (TextView) this.findViewById(R.id.connect_human_text);

        Button scanAgain = (Button) this.findViewById(R.id.connect_btn_scan_again);
        Button discover = (Button) this.findViewById(R.id.discover_printers);

        String advancedText = "";

        if (bluetoothService.getActivePrinter() != null) {
            advancedText = getString(R.string.connected_printer) + bluetoothService.getActivePrinter().friendlyName;
            humanReadable.setText(R.string.printer_connected);
            progressBar.setVisibility(View.GONE);
        } else if (!bluetoothService.inDiscovery()) {
            if (bluetoothService.getDiscoveredPrinters().size() == 0) {
                advancedText = getString(R.string.no_printers_found);
                humanReadable.setText(R.string.no_printers_found);
            } else {
                advancedText = getString(R.string.discovery_finished) + bluetoothService.getDiscoveredPrinters().size() + getString(R.string.printers_found);
                humanReadable.setText(R.string.choose_printer_from_list);
            }
        } else if (bluetoothService.getDefaultPrinterId() != null) {
            advancedText = getString(R.string.searching_default_printer) + bluetoothService.getDefaultPrinterId();
            humanReadable.setText(R.string.reconnecting_to_printer);
        } else {
            advancedText = getString(R.string.searching_for_printer);
            humanReadable.setText(R.string.searching_for_printer);
        }

        if (bluetoothService.getDiscoveryErrorMessage() != null) {
            advancedText += "\n\n" + getString(R.string.discovery_error) + "\n" + bluetoothService.getDiscoveryErrorMessage();
        }

        advancedTextView.setText(advancedText);


        if (!bluetoothService.inDiscovery()) {
            progressBar.setVisibility(View.GONE);
            scanAgain.setVisibility(View.VISIBLE);
            discover.setEnabled(true);
        } else {
            progressBar.setVisibility(View.VISIBLE);
            scanAgain.setVisibility(View.GONE);
            discover.setEnabled(false);
        }
    }

    private void launchPrinterSelect(final ArrayList<DiscoveredPrinterBluetooth> printers) {
        springloadDiscoveryResult = false;
        if (isFinishing()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setAdapter(new ArrayAdapter<DiscoveredPrinterBluetooth>(this, -1, printers) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                DiscoveredPrinterBluetooth printer = this.getItem(position);
                if (convertView == null) {
                    convertView = View.inflate(PrinterSearchingActivity.this, R.layout.component_printer_display, null);
                }

                ((TextView) convertView.findViewById(R.id.printer_name)).setText(printer.friendlyName);
                ((TextView) convertView.findViewById(R.id.printer_address)).setText(printer.address);
                return convertView;
            }
        }, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                bluetoothService.setActivePrinter(printers.get(which));
            }
        });
        builder.setTitle(R.string.choose_printer);
        builder.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        if (item.getItemId() == R.id.clear_active_printer) {
            bluetoothService.wipeActivePrinter();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void done() {
        this.setResult(Activity.RESULT_OK);
        this.finish();
    }

    @Override
    public void onBluetoothStateUpdate() {
        if (isFinishing()) {
            return;
        }

        if (bluetoothService.getActivePrinter() != null && returnWhenSelected) {
            done();
        } else if (springloadDiscoveryResult &&
                !bluetoothService.inDiscovery() &&
                bluetoothService.getActivePrinter() == null &&
                !bluetoothService.getDiscoveredPrinters().isEmpty()) {
            launchPrinterSelect(bluetoothService.getDiscoveredPrinters());
        }
        updateCurrentPrinterView();
    }
}
