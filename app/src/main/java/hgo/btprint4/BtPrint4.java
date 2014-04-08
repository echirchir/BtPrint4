package hgo.btprint4;

import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class BtPrint4 extends Activity {
    btPrintFile btPrintService = null;
    // Layout Views
//    private TextView mTitle;
    private EditText mRemoteDevice;
    Button mConnectButton;
    // Debugging
    private static final String TAG = "btprint";
    private static final boolean D = true;

    ScrollView mScrollView;
    TextView mLog = null;
    Button mBtnExit = null;
    Button mBtnScan=null;

    Button mBtnSelectFile;
    TextView mTxtFilename;
    Button mBtnPrint;
    PrintFileXML printFileXML=null;
    ArrayList<PrintFileDetails> printFileDetailses;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Intent request codes for files list
    private static final int REQUEST_SELECT_FILE = 3;

    BluetoothAdapter mBluetoothAdapter = null;

    View _view;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //show
        super.onCreate(savedInstanceState);
        setContentView(R.layout.btprint_main);

        // CRASHES!
//        // Set up the window layout
//        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
//        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
//        setContentView(R.layout.btprint_main);
//
//        // Set up the custom title
//        mTitle = (TextView) findViewById(R.id.title_left_text);
//        mTitle.setText(R.string.app_name);
//        mTitle = (TextView) findViewById(R.id.title_right_text);

        mScrollView=(ScrollView)findViewById(R.id.ScrollView01);
        mLog = (TextView) findViewById(R.id.log);
        mRemoteDevice=(EditText)findViewById(R.id.remote_device);

        //connect button
        mConnectButton=(Button)findViewById(R.id.buttonConnect);
        mConnectButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                connectToDevice();
            }
        });

        addLog("btprint2 started");

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        //exit button
        mBtnExit = (Button) findViewById(R.id.button1);
        mBtnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                finish();
                return;
            }
        });

        //scan button
        mBtnScan=(Button)findViewById(R.id.button_scan);
        mBtnScan.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startDiscovery();
            }
        });

        mBtnSelectFile=(Button)findViewById(R.id.btnSelectFile);
        mBtnSelectFile.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startFileList();
            }
        });

        mTxtFilename=(TextView)findViewById(R.id.txtFileName);
        mTxtFilename.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startFileList();
            }
        });

        mBtnPrint=(Button)findViewById(R.id.btnPrintFile);
        mBtnPrint.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                printFile();
            }
        });
        //setupComm();
        //list files
        AssetFiles assetFiles=new AssetFiles(this);

        //read file descriptions
        readPrintFileDescriptions();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (D) Log.e(TAG, "++ ON START ++");

        if (mBluetoothAdapter != null) {
            // If BT is not on, request that it be enabled.
            // setupChat() will then be called during onActivityResult
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                // Otherwise, setup the comm session
            } else {
                if(btPrintService==null)
                    setupComm();
                addLog("starting print service...");//if (mChatService == null) setupChat();
            }
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (D) Log.e(TAG, "+ ON RESUME +");
        addLog("onResume");
        /*
		// Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
              // Start the Bluetooth chat services
              mChatService.start();
            }
        }
        */
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if (D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if (D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (btPrintService != null) btPrintService.stop();
        if (D) Log.e(TAG, "--- ON DESTROY ---");
    }

    void readPrintFileDescriptions(){
        //TODO add code
        InputStream inputStream=null;
        try {
            inputStream = this.getAssets().open("demofiles.xml");
            printFileXML=new PrintFileXML(inputStream);
            //now assign the array of known print files and there details
            printFileDetailses=printFileXML.printFileDetails;
        } catch (IOException e) {
            Log.e(TAG, "Exception in readPrintFileDescriptions: " + e.getMessage());
        }
        if(inputStream!=null) {
            try {
                inputStream.close();
            } catch (IOException e) {
            }
        }
    }

    void printFile(){
        if(mTxtFilename.length()>0){
            //TODO: add code
        }
    }

    private void ensureDiscoverable() {
        if (D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    public void addLog(String s) {
        Log.d(TAG, s);
        mLog.append(s + "\r\n");
        mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
        //mLog.invalidate();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    boolean bDiscoveryStarted=false;
    void startDiscovery(){
        if(bDiscoveryStarted)
            return;
        bDiscoveryStarted=true;
        // Launch the DeviceListActivity to see devices and do scan
        Intent serverIntent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }

    boolean bFileListStared=false;
    void startFileList(){
        if(bFileListStared)
            return;
        bFileListStared=true;
        Intent fileListerIntent = new Intent(this, FileListActivity.class);
        startActivityForResult(fileListerIntent, REQUEST_SELECT_FILE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mnuScan:
                startDiscovery();
                return true;
            case R.id.mnuDiscoverable:
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            case R.id.mnuFilelist:
                startFileList();
                return true;
        }
        return false;
    }

    void printESCP(){
        if(btPrintService!=null){
            if(btPrintService.getState()==btPrintFile.STATE_CONNECTED){
                String message=btPrintService.printESCP();
                byte[] buf=message.getBytes();
                btPrintService.write(buf);
                addLog("ESCP printed");
            }
        }
    }
    private void setupComm() {
        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.id.remote_device);
        Log.d(TAG, "setupComm()");
        btPrintService=new btPrintFile(this,mHandler);
        if(btPrintService==null)
            Log.e(TAG, "btPrintService init() failed");
/*
        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mConversationView = (ListView) findViewById(R.id.in);
        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton = (Button) findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                TextView view = (TextView) findViewById(R.id.edit_text_out);
                String message = view.getText().toString();
                sendMessage(message);
            }
        });

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
        */
    }

    // The Handler that gets information back from the btPrintService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case msgTypes.MESSAGE_STATE_CHANGE:
                    if (D) Log.i(TAG, "handleMessage: MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case btPrintFile.STATE_CONNECTED:
                            addLog("connected to: " + mConnectedDeviceName);
                            mConversationArrayAdapter.clear();
                            Log.i(TAG,"handleMessage: STATE_CONNECTED: "+mConnectedDeviceName);
                            break;
                        case btPrintFile.STATE_CONNECTING:
                            addLog("connecting...");
                            Log.i(TAG,"handleMessage: STATE_CONNECTING: "+mConnectedDeviceName);
                            break;
                        case btPrintFile.STATE_LISTEN:
                            addLog("connection ready");
                            Log.i(TAG,"handleMessage: STATE_LISTEN");

                            break;
                        case btPrintFile.STATE_NONE:
                            addLog("not connected");
                            Log.i(TAG,"handleMessage: STATE_NONE: not connected");
                            break;
                    }
                    break;
                case msgTypes.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case msgTypes.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    break;
                case msgTypes.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(msgTypes.DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "handleMessage: CONNECTED TO: " + msg.getData().getString(msgTypes.DEVICE_NAME));
                    printESCP();
                    break;
                case msgTypes.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(msgTypes.TOAST),
                            Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "handleMessage: TOAST: " + msg.getData().getString(msgTypes.TOAST));
                    addLog(msg.getData().getString(msgTypes.TOAST));
                    break;
                case msgTypes.MESSAGE_INFO:
                    addLog(msg.getData().getString(msgTypes.INFO));
                    //mLog.append(msg.getData().getString(msgTypes.INFO));
                    //mLog.refreshDrawableState();
                    String s=msg.getData().getString(msgTypes.INFO);
                    if(s.length()==0)
                        s=String.format("int: %i" + msg.getData().getInt(msgTypes.INFO));
                    Log.i(TAG,"handleMessage: INFO: "+  s);
                    break;
            }
        }
    };

    void connectToDevice(){
        String remote=mRemoteDevice.getText().toString();
        if(remote.length()==0)
            return;
        BluetoothDevice device=mBluetoothAdapter.getRemoteDevice(remote);
        if(device!=null){
            addLog("connecting to "+remote);
            btPrintService.connect(device);
        }
        else{
            addLog("unknown remote device!");
        }
    }

    void connectToDevice(BluetoothDevice _device){
        if(_device!=null){
            addLog("connecting to "+ _device.getAddress());
            btPrintService.connect(_device);
        }
        else{
            addLog("unknown remote device!");
        }
    }


    //handles the scan devices and file list activity (dialog)
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
            case REQUEST_SELECT_FILE:
                addLog("onActivityResult: requestCode==REQUEST_SELECT_FILE");
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    addLog("resultCode==OK");
                    // Get the device MAC address
                    String file = data.getExtras().getString(FileListActivity.EXTRA_FILE_NAME);
                    addLog("onActivityResult: got file="+file);
                    if(printFileXML!=null) {
                        PrintFileDetails details = printFileXML.getPrintFileDetails(file);
                        addLog("printfile type is " + details.printLanguage +
                                "description: " + details.description);
                    }

                    mTxtFilename.setText(file);
                    //mRemoteDevice.setText(device.getAddress());
                    // Attempt to connect to the device
                }
                bFileListStared=false;
                break;
            case REQUEST_CONNECT_DEVICE:
                addLog("onActivityResult: requestCode==REQUEST_CONNECT_DEVICE");
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    addLog("resultCode==OK");
                    // Get the device MAC address
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    addLog("onActivityResult: got device="+address);
                    // Get the BLuetoothDevice object
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    mRemoteDevice.setText(device.getAddress());
                    // Attempt to connect to the device
                    addLog("onActivityResult: connecting service...");
                    //btPrintService.connect(device);
                    connectToDevice(device);
                }
                bDiscoveryStarted=false;
                break;
            case REQUEST_ENABLE_BT:
                addLog("requestCode==REQUEST_ENABLE_BT");
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Log.i(TAG, "onActivityResult: resultCode==OK");
                    // Bluetooth is now enabled, so set up a chat session
                    Log.i(TAG,"onActivityResult: starting setupComm()...");
                    setupComm();
                } else {
                    // User did not enable Bluetooth or an error occured
                    Log.d(TAG, "onActivityResult: BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }
}