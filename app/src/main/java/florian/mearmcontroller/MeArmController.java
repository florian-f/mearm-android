/*
 * Copyright (C) 2012 Mathias Jeppsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package florian.mearmcontroller;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class MeArmController extends Activity {

    private static final int ARDUINO_USB_VENDOR_ID = 0x2341;
    private static final int ARDUINO_UNO_USB_PRODUCT_ID = 0x01;
    private static final int ARDUINO_MEGA_2560_USB_PRODUCT_ID = 0x10;
    private static final int ARDUINO_MEGA_2560_R3_USB_PRODUCT_ID = 0x42;
    private static final int ARDUINO_UNO_R3_USB_PRODUCT_ID = 0x43;
    private static final int ARDUINO_MEGA_2560_ADK_R3_USB_PRODUCT_ID = 0x44;
    private static final int ARDUINO_MEGA_2560_ADK_USB_PRODUCT_ID = 0x3F;

    private final static String TAG = "MeArmController";

    private static final String SERVO_0 = "0:";
    private static final String SERVO_1 = "1:";
    private static final String SERVO_2 = "2:";
    private static final String SERVO_3 = "3:";

    private final static boolean DEBUG = false;
    
    private Boolean mIsReceiving;
    private ArrayList<ByteArray> mTransferedDataList = new ArrayList<ByteArray>();

    private SeekBar mSeekBar0;
    private SeekBar mSeekBar1;
    private SeekBar mSeekBar2;
    private SeekBar mSeekBar3;
    private TextView mTextView0;
    private TextView mTextView1;
    private TextView mTextView2;
    private TextView mTextView3;
    private int mPos0 = 0;
    private int mPos1 = 0;
    private int mPos2 = 0;
    private int mPos3 = 0;

    private void findDevice() {
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        UsbDevice usbDevice = null;
        HashMap<String, UsbDevice> usbDeviceList = usbManager.getDeviceList();
        if (DEBUG) Log.d(TAG, "length: " + usbDeviceList.size());
        Iterator<UsbDevice> deviceIterator = usbDeviceList.values().iterator();
        if (deviceIterator.hasNext()) {
            UsbDevice tempUsbDevice = deviceIterator.next();

            // Print device information. If you think your device should be able
            // to communicate with this app, add it to accepted products below.
            if (DEBUG) Log.d(TAG, "VendorId: " + tempUsbDevice.getVendorId());
            if (DEBUG) Log.d(TAG, "ProductId: " + tempUsbDevice.getProductId());
            if (DEBUG) Log.d(TAG, "DeviceName: " + tempUsbDevice.getDeviceName());
            if (DEBUG) Log.d(TAG, "DeviceId: " + tempUsbDevice.getDeviceId());
            if (DEBUG) Log.d(TAG, "DeviceClass: " + tempUsbDevice.getDeviceClass());
            if (DEBUG) Log.d(TAG, "DeviceSubclass: " + tempUsbDevice.getDeviceSubclass());
            if (DEBUG) Log.d(TAG, "InterfaceCount: " + tempUsbDevice.getInterfaceCount());
            if (DEBUG) Log.d(TAG, "DeviceProtocol: " + tempUsbDevice.getDeviceProtocol());

            if (tempUsbDevice.getVendorId() == ARDUINO_USB_VENDOR_ID) {
                if (DEBUG) Log.i(TAG, "Arduino device found!");

                switch (tempUsbDevice.getProductId()) {
                case ARDUINO_UNO_USB_PRODUCT_ID:
                    Toast.makeText(getBaseContext(), "Arduino Uno " + getString(R.string.found), Toast.LENGTH_SHORT).show();
                    usbDevice = tempUsbDevice;
                    break;
                case ARDUINO_MEGA_2560_USB_PRODUCT_ID:
                    Toast.makeText(getBaseContext(), "Arduino Mega 2560 " + getString(R.string.found), Toast.LENGTH_SHORT).show();
                    usbDevice = tempUsbDevice;
                    break;
                case ARDUINO_MEGA_2560_R3_USB_PRODUCT_ID:
                    Toast.makeText(getBaseContext(), "Arduino Mega 2560 R3 " + getString(R.string.found), Toast.LENGTH_SHORT).show();
                    usbDevice = tempUsbDevice;
                    break;
                case ARDUINO_UNO_R3_USB_PRODUCT_ID:
                    Toast.makeText(getBaseContext(), "Arduino Uno R3 " + getString(R.string.found), Toast.LENGTH_SHORT).show();
                    usbDevice = tempUsbDevice;
                    break;
                case ARDUINO_MEGA_2560_ADK_R3_USB_PRODUCT_ID:
                    Toast.makeText(getBaseContext(), "Arduino Mega 2560 ADK R3 " + getString(R.string.found), Toast.LENGTH_SHORT).show();
                    usbDevice = tempUsbDevice;
                    break;
                case ARDUINO_MEGA_2560_ADK_USB_PRODUCT_ID:
                    Toast.makeText(getBaseContext(), "Arduino Mega 2560 ADK " + getString(R.string.found), Toast.LENGTH_SHORT).show();
                    usbDevice = tempUsbDevice;
                    break;
                }
            }
        }

        if (usbDevice == null) {
            if (DEBUG) Log.i(TAG, "No device found!");
            Toast.makeText(getBaseContext(), getString(R.string.no_device_found), Toast.LENGTH_LONG).show();
        } else {
            if (DEBUG) Log.i(TAG, "Device found!");
            Intent startIntent = new Intent(getApplicationContext(), ArduinoCommunicatorService.class);
            PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, startIntent, 0);
            usbManager.requestPermission(usbDevice, pendingIntent);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG) Log.d(TAG, "onCreate()");

        setContentView(R.layout.activity_me_arm_controller);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ArduinoCommunicatorService.DATA_RECEIVED_INTENT);
        filter.addAction(ArduinoCommunicatorService.DATA_SENT_INTERNAL_INTENT);
        registerReceiver(mReceiver, filter);

        mTextView0 = (TextView) findViewById(R.id.textView0);
        mTextView1 = (TextView) findViewById(R.id.textView1);
        mTextView2 = (TextView) findViewById(R.id.textView2);
        mTextView3 = (TextView) findViewById(R.id.textView3);

        mSeekBar0 = (SeekBar) findViewById(R.id.seekBar0);
        mSeekBar1 = (SeekBar) findViewById(R.id.seekBar1);
        mSeekBar2 = (SeekBar) findViewById(R.id.seekBar2);
        mSeekBar3 = (SeekBar) findViewById(R.id.seekBar3);

        mSeekBar0.setMax(180);
        mSeekBar1.setMax(180);
        mSeekBar2.setMax(180);
        mSeekBar3.setMax(180);


        SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    switch(seekBar.getId()) {
                        case R.id.seekBar0:
                            mTextView0.setText("" + progress);
                            mPos0 = progress;
                            break;
                        case R.id.seekBar1:
                            mTextView1.setText("" + progress);
                            mPos1 = progress;
                            break;
                        case R.id.seekBar2:
                            mTextView2.setText("" + progress);
                            mPos2 = progress;
                            break;
                        case R.id.seekBar3:
                            mTextView3.setText("" + progress);
                            mPos3 = progress;
                            break;
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                String text = "";
                switch (seekBar.getId()) {
                    case R.id.seekBar0:
                        text = SERVO_0 + mPos0+";";
                        break;
                    case R.id.seekBar1:
                        text = SERVO_1 + mPos1+";";
                        break;
                    case R.id.seekBar2:
                        text = SERVO_2 + mPos2+";";
                        break;
                    case R.id.seekBar3:
                        text = SERVO_3 + mPos3+";";
                        break;
                }
                if (!"".equals(text)) {
                    byte[] data = text.getBytes();
                    Intent intent = new Intent(ArduinoCommunicatorService.SEND_DATA_INTENT);
                    intent.putExtra(ArduinoCommunicatorService.DATA_EXTRA, data);
                    sendBroadcast(intent);
                }
            }
        };

        mSeekBar0.setOnSeekBarChangeListener(onSeekBarChangeListener);
        mSeekBar1.setOnSeekBarChangeListener(onSeekBarChangeListener);
        mSeekBar2.setOnSeekBarChangeListener(onSeekBarChangeListener);
        mSeekBar3.setOnSeekBarChangeListener(onSeekBarChangeListener);

        findDevice();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (DEBUG) Log.d(TAG, "onNewIntent() " + intent);
        super.onNewIntent(intent);

        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.contains(intent.getAction())) {
            if (DEBUG) Log.d(TAG, "onNewIntent() " + intent);
            findDevice();
        }
    }

    @Override
    protected void onDestroy() {
        if (DEBUG) Log.d(TAG, "onDestroy()");
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.me_arm_controller, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_about:
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/florian-f/mearm-android")));
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {

        private void handleTransferedData(Intent intent, boolean receiving) {
            if (mIsReceiving == null || mIsReceiving != receiving) {
                mIsReceiving = receiving;
                mTransferedDataList.add(new ByteArray());
            }

            final byte[] newTransferedData = intent.getByteArrayExtra(ArduinoCommunicatorService.DATA_EXTRA);
            if (DEBUG) Log.i(TAG, "data: " + newTransferedData.length + " \"" + new String(newTransferedData) + "\"");

            ByteArray transferedData = mTransferedDataList.get(mTransferedDataList.size() - 1);
            transferedData.add(newTransferedData);
            mTransferedDataList.set(mTransferedDataList.size() - 1, transferedData);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (DEBUG) Log.d(TAG, "onReceive() " + action);

            if (ArduinoCommunicatorService.DATA_RECEIVED_INTENT.equals(action)) {
                handleTransferedData(intent, true);
            } else if (ArduinoCommunicatorService.DATA_SENT_INTERNAL_INTENT.equals(action)) {
                handleTransferedData(intent, false);
            }
        }
    };
}
