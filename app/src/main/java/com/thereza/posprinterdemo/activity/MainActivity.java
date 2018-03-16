package com.thereza.posprinterdemo.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mocoo.hang.rtprinter.driver.Contants;
import com.mocoo.hang.rtprinter.driver.HsBluetoothPrintDriver;
import com.thereza.posprinterdemo.R;
import com.thereza.posprinterdemo.utility.PrintReceipt;
import com.thereza.posprinterdemo.utility.StaticValue;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static BluetoothDevice device;
    private static Context CONTEXT;
    private AlertDialog.Builder alertDlgBuilder;
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private BluetoothAdapter mBluetoothAdapter = null;
    public static HsBluetoothPrintDriver BLUETOOTH_PRINTER = null;

    private static Button mBtnConnetBluetoothDevice = null;
    private static Button mBtnPrint = null;
    private static TextView tvPrinterStatus = null;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "+++ ON CREATE +++");

        setContentView(R.layout.activity_main);

        CONTEXT = getApplicationContext();
        alertDlgBuilder = new AlertDialog.Builder(MainActivity.this);

        // Get device's Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not available in your device
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;

        }
        //Initialize widgets
        InitUIControl();
    }

    private void InitUIControl() {
        tvPrinterStatus = (TextView) findViewById(R.id.txtPrinerStatus);
        mBtnConnetBluetoothDevice = (Button) findViewById(R.id.btn_connect_bluetooth_device);
        mBtnConnetBluetoothDevice.setOnClickListener(mBtnConnetBluetoothDeviceOnClickListener);
        mBtnPrint = (Button) findViewById(R.id.btn_print);
        mBtnPrint.setOnClickListener(mBtnPrintOnClickListener);

    }

    @Override
    public void onStart() {
        super.onStart();
        Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that to be enabled.
        // initializeBluetoothDevice() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else {
            if (BLUETOOTH_PRINTER == null) {
                initializeBluetoothDevice();
            } else {
                if (BLUETOOTH_PRINTER.IsNoConnection()) {

                } else {
                    tvPrinterStatus.setText(R.string.title_connected_to);
                    tvPrinterStatus.append(device.getName());

                }
            }

        }
    }

    private void initializeBluetoothDevice() {

        // Initialize HsBluetoothPrintDriver class to perform bluetooth connections
        BLUETOOTH_PRINTER = HsBluetoothPrintDriver.getInstance();//
        BLUETOOTH_PRINTER.setHandler(new BluetoothHandler(MainActivity.this));
    }

    /**
     * The Handler that gets information back from Bluetooth Devices
     */
    static class BluetoothHandler extends Handler {
        private final WeakReference<MainActivity> myWeakReference;

        //Creating weak reference of BluetoothPrinterActivity class to avoid any leak
        BluetoothHandler(MainActivity weakReference) {
            myWeakReference = new WeakReference<MainActivity>(weakReference);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity mainActivity = myWeakReference.get();
            if (mainActivity != null) {
                super.handleMessage(msg);
                Bundle data = msg.getData();
                switch (data.getInt("flag")) {
                    case Contants.FLAG_STATE_CHANGE:
                        int state = data.getInt("state");
                        Log.i(TAG, "MESSAGE_STATE_CHANGE: " + state);
                        switch (state) {
                            case HsBluetoothPrintDriver.CONNECTED_BY_BLUETOOTH:
                                tvPrinterStatus.setText(R.string.title_connected_to);
                                tvPrinterStatus.append(device.getName());
                                StaticValue.isPrinterConnected = true;
                                Toast.makeText(CONTEXT, "Connection successful.", Toast.LENGTH_SHORT).show();

                                break;
                            case HsBluetoothPrintDriver.FLAG_SUCCESS_CONNECT:
                                tvPrinterStatus.setText(R.string.title_connecting);
                                break;

                            case HsBluetoothPrintDriver.UNCONNECTED:
                                tvPrinterStatus.setText(R.string.no_printer_connected);
                                break;
                        }
                        break;
                    case Contants.FLAG_SUCCESS_CONNECT:
                        tvPrinterStatus.setText(R.string.title_connecting);
                        break;
                    case Contants.FLAG_FAIL_CONNECT:
                        Toast.makeText(CONTEXT, "Connection failed.", Toast.LENGTH_SHORT).show();

                        break;
                    default:
                        break;

                }
            }
        }

        ;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BLuetoothDevice object
                    device = mBluetoothAdapter.getRemoteDevice(address);
                    // Attempt to connect to the device
                    BLUETOOTH_PRINTER.start();
                    BLUETOOTH_PRINTER.connect(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    initializeBluetoothDevice();
                } else {
                    // User did not enable Bluetooth or an error occured
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }


    View.OnClickListener mBtnQuitOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Stop the Bluetooth chat services
            if (!PrintReceipt.printBillFromOrder(getApplicationContext())) {
                Toast.makeText(MainActivity.this, "No printer is connected!!", Toast.LENGTH_LONG).show();
            }

        }

        ;
    };

    View.OnClickListener mBtnPrintOnClickListener = new View.OnClickListener() {
        public void onClick(View arg0) {
            PrintReceipt.printBillFromOrder(MainActivity.this);
        }
    };

    View.OnClickListener mBtnConnetBluetoothDeviceOnClickListener = new View.OnClickListener() {
        Intent serverIntent = null;

        public void onClick(View arg0) {

            //If bluetooth is disabled then ask user to enable it again
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            } else {//If the connection is lost with last connected bluetooth printer
                if (BLUETOOTH_PRINTER.IsNoConnection()) {
                    serverIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                } else { //If an existing connection is still alive then ask user to kill it and re-connect again
                    alertDlgBuilder.setTitle(getResources().getString(R.string.alert_title));
                    alertDlgBuilder.setMessage(getResources().getString(R.string.alert_message));
                    alertDlgBuilder.setNegativeButton(getResources().getString(R.string.alert_btn_negative), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            }
                    );
                    alertDlgBuilder.setPositiveButton(getResources().getString(R.string.alert_btn_positive), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    BLUETOOTH_PRINTER.stop();
                                    serverIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                                }
                            }
                    );
                    alertDlgBuilder.show();

                }
            }

        }

        ;

    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (BLUETOOTH_PRINTER.IsNoConnection())
            BLUETOOTH_PRINTER.stop();
    }
}