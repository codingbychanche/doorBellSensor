package com.berthold.doorbellsensor.main;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.berthold.doorbellsensor.R;
import com.example.bluetoothconnector.*;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Set;

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
    private final static String COMMAND_RESET_DOORBELL_COUNTER="rscntr";

    // UI
    private FloatingActionButton reconnect, connectToAnotherDevice;
    private TextView connectionStatusView;
    private Button resetDoorBellCounterView;

    // Animations
    Animation fadeInAnim, fadeOutAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // Debug
        tag = getClass().getSimpleName();

        // UI
        reconnect = (FloatingActionButton) findViewById(R.id.reconnect);
        connectToAnotherDevice = (FloatingActionButton) findViewById(R.id.select_device);
        connectionStatusView = findViewById(R.id.connection_status);
        connectToAnotherDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDeviceList();
            }
        });
        resetDoorBellCounterView=findViewById(R.id.reset_doorbell_counter_on_dev);
        resetDoorBellCounterView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(connectedThreadReadWriteData!=null)
                    connectedThreadReadWriteData.send(COMMAND_RESET_DOORBELL_COUNTER);
            }
        });

        // Anim
        fadeInAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.menu_fade_in);
        fadeOutAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.menu_fade_out);

        reconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectToDevice(bluetoothDeviceCurentlyConnectedTo);
            }
        });

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // View model and it's observers
        //
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // BT- Status
        final Observer<String> btStatusObserver = new Observer<String>() {
            @Override
            public void onChanged(@Nullable final String status) {
                connectionStatusView.setText(status);
            }
        };
        mainViewModel.getBtStatusMessage().observe(this, btStatusObserver);

        // BT- receive data
        final Observer<String> btReceiveDataObserver = new Observer<String>() {
            @Override
            public void onChanged(@Nullable final String received) {
                connectionStatusView.setText(received);
            }
        };
        mainViewModel.getbtReceivedData().observe(this, btReceiveDataObserver);

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //
        // Establish connection
        //
        // Check if bluetooth is enabeled
        BluetoothManager bm = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bm.getAdapter();

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(new BTEventReceiver(), filter);

        if (!bluetoothAdapter.isEnabled()) {
            Intent bluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(bluetoothIntent, 1);
        }
        connectToDeviceLogic();

        //
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
     * Receives an instance of the {@link  } over which
     * data can be send to the connected device.
     */
    @Override
    public void sucess(ConnectedThreadReadWriteData connectedThreadReadWriteData) {

        Log.v(tag, "Connection success!!!!!");
        mainViewModel.getbtSucessMessage().postValue("Connected");
        this.connectedThreadReadWriteData = connectedThreadReadWriteData;
        connectToAnotherDevice.startAnimation(fadeOutAnim);
        reconnect.startAnimation(fadeOutAnim);
        //connectedThreadReadWriteData.send("Sending test, test....");

        currentStateSaveToSharedPref(addressOfCurrentDevice);
    }

    /**
     * Callback receive status message from connected device.
     */
    @Override
    public void receiveStatusMessage(String status) {
        Log.v(tag, status);
        //mainViewModel.btStatusMessage.postValue(status);
    }

    /**
     * Receive error message
     */
    @Override
    public void receiveErrorMessage(String error) {
        connectToAnotherDevice.startAnimation(fadeInAnim);
        reconnect.startAnimation(fadeInAnim);
        Log.v(tag, error);
        //connectionStatusView.setText(error);
    }

    /**
     * Callback receiving data from connected device.
     */
    @Override
    public void receiveDataFromBTDevice(String received) {
        Log.v(tag, received);
        mainViewModel.btReceivedData.postValue(received);
    }
    /////////////////////////////////////////////////////// All things Bluetooth ///////////////////////////////////////////////////////////////////////////

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
            // connectionStatus.append(getResources().getString(R.string.found_bomded_devices_looking_for_hc05) + ":" + adressOfBluetoothDevice);

            for (BluetoothDevice dev : btBondedDevices) {
                if (dev.getAddress().equals(adressOfBluetoothDevice)) {
                    bluetoothDeviceCurentlyConnectedTo = dev;
                    //connectionStatus.append(getResources().getString(R.string.found_hc05) + " " + bluetoothDeviceCurentlyConnectedTo.getName() + " " + bluetoothDeviceCurentlyConnectedTo.getAddress());
                    Log.v(tag, "MYDEBUG>Bonded hc05 adress is:" + bluetoothDeviceCurentlyConnectedTo.getAddress().toString() + " Name:" + bluetoothDeviceCurentlyConnectedTo.getName().toString());
                }
            }
        } else {
            //connectionStatus.append(getResources().getString(R.string.error_no_dev_with_this_address_found));
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
            connectionStatusView.setText(htmlText);

            //connectionStatus.setText(getResources().getString(R.string.info_connecting) + " Name:" + " " + name + "    Adress:" + address);

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

            connectionStatusView.setText("No devices found");
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