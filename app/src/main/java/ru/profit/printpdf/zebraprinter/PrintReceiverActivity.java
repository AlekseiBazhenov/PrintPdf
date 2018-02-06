package ru.profit.printpdf.zebraprinter;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.profit.printpdf.R;

public class PrintReceiverActivity extends Activity implements BluetoothStateHolder.BluetoothStateListener, PrintTaskListener {

    @BindView(R.id.list_print_jobs)
    ListView listPrintJobs;
    @BindView(R.id.current_status_text)
    TextView statusText;
    @BindView(R.id.button_update_settings)
    Button updateSettings;
    @BindView(R.id.button_cancel_print)
    Button cancelPrint;

    public static final String KEY_BUNDLE_LIST = "zebra:bundle_list";

    private static final int REQUEST_PRINTER = 1;
    private static final String TASK_FRAGMENT_TAG = "print_fragment";

    private ArrayList<String> bundleKeyList;
    private ZebraPrintTask.PrintJob[] jobs;

    private BluetoothStateHolder bluetoothService;

    PrintJobListAdapter adapter;

    boolean listViewTouched = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print_receiver);
        ButterKnife.bind(this);

        ensureFragmentConnected();

        cancelPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TaskHolderFragment printTaskFragment =
                        (TaskHolderFragment) getFragmentManager().findFragmentByTag(TASK_FRAGMENT_TAG);
                if (printTaskFragment == null) {
                    v.setEnabled(false);
                    return;
                }

                printTaskFragment.cancelPrintTask();
                cancelPrint.setText(R.string.canselling);
                cancelPrint.setEnabled(false);

                if (adapter != null) {
                    adapter.setModeCancelled();
                    adapter.notifyDataSetChanged();
                }
            }
        });

        updateSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calloutToSelectPrinter();
            }
        });


        adapter = new PrintJobListAdapter(this, jobs);
        listPrintJobs.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                listViewTouched = true;
                return false;
            }
        });
        listPrintJobs.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        BluetoothStateHolder.attachContextListener(this, this);

        TaskHolderFragment printTaskFragment =
                (TaskHolderFragment) getFragmentManager().findFragmentByTag(TASK_FRAGMENT_TAG);
        printTaskFragment.ensureTaskRunning(bluetoothService);
        updateStatusText();
    }

    @Override
    public void attachStateHolder(BluetoothStateHolder bluetoothStateHolder) {
        this.bluetoothService = bluetoothStateHolder;
    }

    private void ensureFragmentConnected() {
        FragmentManager fragmentManager = this.getFragmentManager();

        TaskHolderFragment printTaskFragment =
                (TaskHolderFragment) fragmentManager.findFragmentByTag(TASK_FRAGMENT_TAG);

        if (printTaskFragment == null) {
            bundleKeyList = getIntent().getStringArrayListExtra(KEY_BUNDLE_LIST);
            if (bundleKeyList == null || bundleKeyList.size() == 0) {
                throw new RuntimeException("No print jobs provided to print activity!");
            }

            jobs = generatePrintJobs();

            printTaskFragment = new TaskHolderFragment();
            printTaskFragment.setJobs(jobs);

            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.add(printTaskFragment, TASK_FRAGMENT_TAG);
            transaction.commit();
        } else {
            //the fragment will attach on its own, rely on that process to trigger a redraw
            this.jobs = printTaskFragment.getPrintJobs();
        }
    }

    private ZebraPrintTask.PrintJob[] generatePrintJobs() {
        Bundle[] printSets = new Bundle[bundleKeyList.size()];
        for (int i = 0; i < printSets.length; ++i) {
            printSets[i] = this.getIntent().getBundleExtra(bundleKeyList.get(i));
        }

        ZebraPrintTask.PrintJob[] jobs = new ZebraPrintTask.PrintJob[printSets.length];
        for (int i = 0; i < printSets.length; ++i) {
            jobs[i] = new ZebraPrintTask.PrintJob(i, printSets[i]);
        }
        return jobs;
    }

    @Override
    protected void onPause() {
        super.onPause();
        bluetoothService.detachStateListener(this);

        if (this.isFinishing()) {
            TaskHolderFragment fragment = this.getTaskFragment();
            if (fragment != null) {
                fragment.signalKill();
            }
        }
    }

    private void calloutToSelectPrinter() {
        Intent i = new Intent(this, PrinterSearchingActivity.class);
        if (bluetoothService == null) {
            i.putExtra(PrinterSearchingActivity.RETURN_WHEN_SELECTED, true);
        } else {
            boolean haveActivePrinter = bluetoothService.getActivePrinter() != null;
            i.putExtra(PrinterSearchingActivity.RETURN_WHEN_SELECTED, !haveActivePrinter);
        }
        this.startActivityForResult(i, REQUEST_PRINTER);
    }


    private void finishAndExit() {
        //TODO: Job results
        this.setResult(Activity.RESULT_OK);
        this.finish();
    }

    private TaskHolderFragment getTaskFragment() {
        return (TaskHolderFragment) this.getFragmentManager().findFragmentByTag(TASK_FRAGMENT_TAG);
    }

    @Override
    public void onBluetoothStateUpdate() {
        updateStatusText();

        TaskHolderFragment printTaskFragment = getTaskFragment();

        if (!bluetoothService.inDiscovery() &&
                bluetoothService.getDiscoveredPrinters().size() > 0 &&
                printTaskFragment != null &&
                printTaskFragment.firePrintConnectionSpringIfLoaded()) {
            this.calloutToSelectPrinter();
        }
    }

    private void updateStatusText() {
        String taskUpdateMessage = getTaskFragment().getCurrentTaskMessage();
        if (bluetoothService.getActivePrinter() != null) {
            if (taskUpdateMessage != null) {
                statusText.setText(taskUpdateMessage);
            } else {
                statusText.setText(R.string.connected);
            }
        } else if (bluetoothService.inDiscovery()) {
            if (bluetoothService.getDefaultPrinterId() != null) {
                statusText.setText(R.string.connecting);
            } else {
                statusText.setText(R.string.searching);
            }
        } else {
            String status = "";
            if (bluetoothService.getDefaultPrinterId() != null) {
                status += getString(R.string.printer_not_found);
            }
            int devices = bluetoothService.getDiscoveredPrinters().size();
            if (devices > 0) {
                status += devices + getString(R.string.printer_found);
            } else {
                status += getString(R.string.no_discovered);
            }
            if (bluetoothService.getDiscoveryErrorMessage() != null) {
                status += getString(R.string.searching_error) + bluetoothService.getDiscoveryErrorMessage();
            }
            statusText.setText(status);
        }
    }

    @Override
    public void taskUpdate(ZebraPrintTask task) {
        if (adapter != null) {
            updateStatusText();
            adapter.notifyDataSetChanged();
            this.cancelPrint.setEnabled(true);

            if (!listViewTouched) {
                listPrintJobs.smoothScrollToPosition(task.getCurrentJobNumber());
            }
        }
    }

    @Override
    public void taskFinished(final boolean taskSuccesful) {
        cancelPrint.setEnabled(true);
        int successfulJobs = getNumberOfSuccessfulJobs();
        if (taskSuccesful) {
            if (successfulJobs == jobs.length) {
                finishAndExit();
            } else {
                cancelPrint.setText(R.string.return_str);
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }
        } else {
            if (successfulJobs == 0) {
                finishAndExit();
            } else {
                cancelPrint.setText(R.string.return_str);
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }
        }
        cancelPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishAndExit();
            }
        });
    }

    private int getNumberOfSuccessfulJobs() {
        int numSuccessful = 0;
        for (ZebraPrintTask.PrintJob job : jobs) {
            if (job.getStatus() == ZebraPrintTask.PrintJob.Status.SUCCESS) {
                numSuccessful++;
            }
        }
        return numSuccessful;
    }
}
