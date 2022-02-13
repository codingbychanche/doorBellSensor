package com.berthold.doorbellsensor.main;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.berthold.doorbellsensor.R;
import com.example.bluetoothconnector.*;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Set;

/**
 * <p><h1></h1>Detects when the doorbell has been rang....</h1></p>
 *
 * <p>Distinctly this class contains all interface receivers which evaluate/ display the result of all bluetooth
 * connection/ data receiving results.<p>
 *
 * <p>When an connection was established successfully, {@link JobServiceAlarmListenerForBTIncomming} is invoked
 * which distictivly evaluates if an alarm occurred and notifies the {@link FragmentDoorBellWatcher} and {@link FragmentRangHistory} fragments.</p>
 *
 * <p>{@link FragmentDoorBellWatcher}
 * receives and displays all data send by the connected device, evaluates if an
 * alarm has taken place and counts the total number of alarms occurred <br>
 *
 * <u>Alarms are received via the broadcast receiver ({sender: {@link JobServiceAlarmListenerForBTIncomming}=> <b>com.berthold.servicejobintentservice.ALARM_INTENT</b>)
 * Data</ul>
 * <u> It also receives broadcasts notifying that the {@link JobServiceAlarmListenerForBTIncomming}=> <b>com.berthold.servicejobintentservice.ALARM_WATCHER_ALIVE</b>
 * is still alive</u>
 * <u>Data is received by the interface methods implemented from {@link BTConnectedInterface}</u>
 * </p>
 *
 * <p>{@link FragmentRangHistory}
 * receives only alarm events send by the {@link JobServiceAlarmListenerForBTIncomming} service
 * (<b>com.berthold.servicejobintentservice.ALARM_INTENT</b>). It then adds a new entry to the rang history- list and displays it....</p>
 *
 * <p>View Model</p>
 * {@link MainActivity} and all fragments share one view model => {@link MainViewModel}
 *
 */
public class MainActivity extends AppCompatActivity implements BTConnectedInterface, FragmentSelectDevice.getDataFromFragment {

    // Debug
    private String tag;

    // View model
    private MainViewModel mainViewModel;

    // Shared
    SharedPreferences sharedPreferences;

    // Bluetooth
    private BluetoothAdapter blueToothAdapter;
    private Set<BluetoothDevice> btBondedDevices;
    private BluetoothDevice bluetoothDeviceCurentlyConnectedTo;
    private ConnectThread connectThread;
    private String addressOfCurrentDevice;

    // Connected thread
    private ConnectedThreadReadWriteData connectedThreadReadWriteData;
    private final static int BROKEN_HEART = 128148;
    private final static int ANTENNA = 128225;

    // device specific commands
    private final static String COMMAND_RESET_DOORBELL_COUNTER = "rsct";
    private final static String COMMAND_SEND_MESSAGE = "rmsg";
    private final static String COMMAND_LOCK = "lock";
    private final static String COMMAND_UNLOCK = "ulck";
    private final static String COMMAND_NEXT_SCREEN="incs";
    private final static String COMMAND_PREV_SCREEN="decs";

    // UI
    private FloatingActionButton reconnect, connectToAnotherDevice;
    private TextView connectedDeviceNameAndAddreeView, connectionHistoryView;
    private ImageButton resetDoorBellCounterView,nextScreenView,prevScreenView;
    private Switch lockDeviceView;
    private Spinner sendMessageSelectView;

    // Animations
    Animation fadeInAnim, fadeOutAnim;

    /**
     * Let there be light......
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // Debug
        tag = getClass().getSimpleName();
        Long time = System.currentTimeMillis();

        // Anim
        fadeInAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.menu_fade_in);
        fadeOutAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.menu_fade_out);

        // UI
        connectedDeviceNameAndAddreeView = findViewById(R.id.connection_status);
        connectionHistoryView = findViewById(R.id.connection_history);

        reconnect = (FloatingActionButton) findViewById(R.id.reconnect);
        reconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectToDevice(bluetoothDeviceCurentlyConnectedTo);
            }
        });

        connectToAnotherDevice = (FloatingActionButton) findViewById(R.id.select_device);
        connectToAnotherDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDeviceList();
            }
        });

        /////////////////////////////////////////////////// Sends commands to the connected device ///////////////////////////////////////////////////////////
        //
        // Sends messages to the connected device...
        //
        sendMessageSelectView = findViewById(R.id.message_select_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.messages_to_send, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sendMessageSelectView.setAdapter(adapter);
        sendMessageSelectView.setSelection(0);

        sendMessageSelectView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                long currentTime = System.currentTimeMillis();

                if (connectedThreadReadWriteData != null) {
                    connectedThreadReadWriteData.send(COMMAND_SEND_MESSAGE);
                    connectedThreadReadWriteData.send(mainViewModel.messageLogic(i));

                    connectionHistoryView.append(currentTime + ">>Message:" + mainViewModel.messageLogic(i) + "\n");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        //
        // Reset doorbell counter
        //
        resetDoorBellCounterView = findViewById(R.id.reset_doorbell_counter_on_dev);
        resetDoorBellCounterView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (connectedThreadReadWriteData != null) {
                    connectedThreadReadWriteData.send(COMMAND_RESET_DOORBELL_COUNTER);
                    long currentTime = System.currentTimeMillis();
                    connectionHistoryView.append(currentTime + ">>Doorbell counter was reset\n");
                }
            }
        });

        //
        // Lock device
        //
        lockDeviceView = findViewById(R.id.lock_device);
        lockDeviceView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isLocked) {
                if (isLocked) {
                    if (connectedThreadReadWriteData != null) {
                        connectedThreadReadWriteData.send(COMMAND_LOCK);
                        long currentTime = System.currentTimeMillis();
                        connectionHistoryView.append(currentTime + ">>Keys on device are now locked.....\n");
                    }
                } else {
                    if (connectedThreadReadWriteData != null) {
                        connectedThreadReadWriteData.send(COMMAND_UNLOCK);
                        long currentTime = System.currentTimeMillis();
                        connectionHistoryView.append(currentTime + ">>Keys on device are now unlocked.....\n");
                    }
                }
            }
        });

        //
        // Advance one screen displayed on the connected device
        //
        nextScreenView=findViewById(R.id.next_screen);
        nextScreenView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectedThreadReadWriteData.send(COMMAND_NEXT_SCREEN);
                long currentTime = System.currentTimeMillis();
                connectionHistoryView.append(currentTime+">>Next screen");
            }
        });

        //
        // Go one screen back on the connected device.
        //
        prevScreenView=findViewById(R.id.prev_screen);
        prevScreenView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectedThreadReadWriteData.send(COMMAND_PREV_SCREEN);
                long currentTime = System.currentTimeMillis();
                connectionHistoryView.append(currentTime+">>Last screen");
            }
        });

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // View model and it's observers
        //
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        //
        // BT- Status
        //
        // Receives status messages while the connection is being established or
        // destroyed.
        //
        final Observer<String> btStatusObserver = new Observer<String>() {
            @Override
            public void onChanged(@Nullable final String status) {
                connectionHistoryView.append(status + "\n");
            }
        };
        mainViewModel.getBtStatusMessage().observe(this, btStatusObserver);

        //
        // BT- error messages
        //
        // Yes, all error messages arrive here.
        //
        final Observer<String> btErrorObserver = new Observer<String>() {
            @Override
            public void onChanged(@Nullable final String error) {
                //connectToAnotherDevice.startAnimation(fadeInAnim);
                connectToAnotherDevice.show();
                // reconnect.startAnimation(fadeInAnim);
                 // reconnect.setEnabled(true);
                reconnect.show();
                long currentTime = System.currentTimeMillis();
                connectionHistoryView.append(currentTime + ">>" + error + "\n");
                //mainViewModel.btErrorMessage.postValue(error);
            }
        };
        mainViewModel.getBtErrorMessage().observe(this, btErrorObserver);

        //
        // BT- success ,message. Called every time a connection could be established
        //
        //
        final Observer<String> btSuccessObserver = new Observer<String>() {
            @Override
            public void onChanged(@Nullable final String success) {
                long currentTime = System.currentTimeMillis();
                connectionHistoryView.append(currentTime + ">>" + success + "\n");
            }
        };
        mainViewModel.getbtSucessMessage().observe(this, btSuccessObserver);

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //
        // Establish connection
        //
        // Check if bluetooth is enabled
        BluetoothManager bm = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bm.getAdapter();

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(new BTEventReceiver(), filter);

        if (!bluetoothAdapter.isEnabled()) {
            Intent bluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(bluetoothIntent, 1);
        }
        connectToDeviceLogic();

        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Show all associated fragments....
        //
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_doorbell_watcher, FragmentDoorBellWatcher.newInstance())
                    .commitNow();

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_dorbell_rag_list, FragmentRangHistory.newInstance())
                    .commitNow();
        }
    }

    /**
     * Callback when a connection was established.
     * <p>
     * Receives an instance of {@link ConnectedThreadReadWriteData}over which
     * data can be send to the connected device or data can be received.
     * <p>
     *
     *
     */
    @Override
    public void sucess(ConnectedThreadReadWriteData connectedThreadReadWriteData) {

        this.connectedThreadReadWriteData = connectedThreadReadWriteData;

        connectToAnotherDevice.startAnimation(fadeOutAnim);
        //connectToAnotherDevice.setEnabled(false);
        reconnect.startAnimation(fadeOutAnim);
        //reconnect.setEnabled(false);

        currentStateSaveToSharedPref(addressOfCurrentDevice);

        //
        // Send initial message to device
        //
        connectedThreadReadWriteData.send(COMMAND_SEND_MESSAGE);
        connectedThreadReadWriteData.send(mainViewModel.messageLogic(mainViewModel.MESSAGE_HOME));

        //
        // Create Job Service
        //
        Intent jobIntent=new Intent (getApplicationContext(), JobServiceAlarmListenerForBTIncomming.class);
        JobServiceAlarmListenerForBTIncomming.doWork(getApplicationContext(),connectedThreadReadWriteData.getInputStream(),jobIntent);

        mainViewModel.btSucessMessage.postValue("Connected...");
    }

    /**
     * Publishes received data to all observers...
     *
     * @param d {@link DecodedSensorData} instance which contains all data received from the
     *          connected device.
     */
    @Override
    public void receiveDataFromBTDevice(DecodedSensorData d) {
        mainViewModel.btReceivedData.postValue(d);
    }

    /**
     * Callback receive status message from connected device.
     */
    @Override
    public void receiveStatusMessage(String status) {
        Log.v(tag, status);
        mainViewModel.btStatusMessage.postValue(status);
    }

    /**
     * Receive error message
     */
    @Override
    public void receiveErrorMessage(String error) {
        Log.v(tag, error);
        mainViewModel.btErrorMessage.postValue(error);

    }

    /////////////////////////////////////////////////////// All things Bluetooth ///////////////////////////////////////////////////////////////////////////
    // Would be nice if I could move this code to the view model. For he time beeing using the view model
    // helped me to avoid working with handlers to access UI components
    //
    /**
     * When bt is turned on or it was turned on, this checks if a
     * device address was stored in shared prefs and if so, get the
     * associated device and try's to connect it. If not, show a
     * list of bonded dev's allowing the user to select one.....
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void connectToDeviceLogic() {

        reconnect.startAnimation(fadeOutAnim);
        connectToAnotherDevice.startAnimation(fadeOutAnim);
        addressOfCurrentDevice = currentStateRestoreFromSharedPref();

        // Device already selected and saved after leaving previous session?
        // If so, try to reconnect... If not, show list of devices
        // to connect to.
        addressOfCurrentDevice = currentStateRestoreFromSharedPref();
        if (addressOfCurrentDevice.equals("NO_DEVICE")) {
            showDeviceList();
        } else {
            bluetoothDeviceCurentlyConnectedTo = getBlueToothDeviceByAdress(addressOfCurrentDevice);
            if (bluetoothDeviceCurentlyConnectedTo != null)
                connectToDevice(bluetoothDeviceCurentlyConnectedTo);
        }
    }

    /**
     * Shows a list of bonded devices to choose from and
     * to connect to.
     */
    private void showDeviceList() {
        FragmentManager fm = getSupportFragmentManager();
        FragmentSelectDevice fragmentSelectDevice = FragmentSelectDevice.newInstance("Titel");
        fragmentSelectDevice.show(fm, "fragment_select_device");
    }

    /**
     * Callback for {@link FragmentSelectDevice}
     */
    @Override
    public void getDialogInput(String buttonPressed, String deviceName, BluetoothDevice bluetoothDeviceToConnectTo) {
        Log.v(tag, "Button pressed" + buttonPressed + " Device:" + deviceName);

        if (buttonPressed.equals("CANCEL")) {
            Log.v(tag, " No device selected.....");
        } else {
            Log.v(tag, " Device selected:" + deviceName + " Connecting.....");
            connectToDevice(bluetoothDeviceToConnectTo);
            addressOfCurrentDevice = bluetoothDeviceToConnectTo.getAddress();
        }
    }

    /**
     * Get Bluetooth device by it's address
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private BluetoothDevice getBlueToothDeviceByAdress(String adressOfBluetoothDevice) {
        BluetoothManager bm = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bm.getAdapter();
        btBondedDevices = bluetoothAdapter.getBondedDevices();

        if (btBondedDevices.size() > 0) {
            for (BluetoothDevice dev : btBondedDevices) {
                if (dev.getAddress().equals(adressOfBluetoothDevice)) {
                    bluetoothDeviceCurentlyConnectedTo = dev;
                    connectedDeviceNameAndAddreeView.setText(bluetoothDeviceCurentlyConnectedTo.getName() + " // " + bluetoothDeviceCurentlyConnectedTo.getAddress());
                    Log.v(tag, "MYDEBUG>Bonded hc05 adress is:" + bluetoothDeviceCurentlyConnectedTo.getAddress().toString() + " Name:" + bluetoothDeviceCurentlyConnectedTo.getName().toString());
                }
            }
        } else {
            mainViewModel.btErrorMessage.postValue("The selected device could not be found....");
            showDeviceList();
        }
        return bluetoothDeviceCurentlyConnectedTo;
    }

    /**
     * Connect to device
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void connectToDevice(BluetoothDevice bluetoothDevice) {

        connectToAnotherDevice.startAnimation(fadeOutAnim);
        reconnect.startAnimation(fadeOutAnim);

        if (bluetoothDevice != null) {
            String name = bluetoothDevice.getName();
            String address = bluetoothDevice.getAddress();

            String html = "&#" + ANTENNA + " " + name + " " + address;
            Spanned htmlText = Html.fromHtml(html);

            mainViewModel.btStatusMessage.postValue(name + " // " + address);

            BluetoothManager bm = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
            blueToothAdapter = bm.getAdapter();

            reconnect.startAnimation(fadeOutAnim);
            connectToAnotherDevice.startAnimation(fadeOutAnim);

            // todo Test
            Context c = getApplicationContext();
            connectThread = new ConnectThread(bluetoothDevice, c, this);
            connectThread.start();
        } else {
            Log.v(tag, "Error: No devices found");
            connectToAnotherDevice.startAnimation(fadeInAnim);
            reconnect.startAnimation(fadeInAnim);

            connectedDeviceNameAndAddreeView.setText("No devices found");
        }
    }
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Save current state to sharedPreferences.
     */
    private void currentStateSaveToSharedPref(String adressOfCurrentDevice) {
        sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        // ToDo
        editor.putString("adressOfCurrentDevice", adressOfCurrentDevice);
        editor.commit();
    }

    /**
     * Restore from shared pref's..
     */
    private String currentStateRestoreFromSharedPref() {
        sharedPreferences = getPreferences(MODE_PRIVATE);
        String adressOfCurrentDevice = sharedPreferences.getString("adressOfCurrentDevice", "NO_DEVICE");
        return adressOfCurrentDevice;
    }
}