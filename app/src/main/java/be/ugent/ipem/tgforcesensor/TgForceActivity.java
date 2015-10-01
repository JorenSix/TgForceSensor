package be.ugent.ipem.tgforcesensor;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class TgForceActivity extends Activity implements TgForceEventHandler {

    private TextView textViewPPA;
    private TextView textViewCadence;
    private TextView textViewBattery;
    private TextView textViewStatus;

    private TgForceSensor sensor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tg_force);

        textViewPPA = (TextView) findViewById(R.id.textViewPPA);
        textViewCadence = (TextView) findViewById(R.id.textViewCadence);
        textViewBattery = (TextView) findViewById(R.id.textViewBattery);
        textViewStatus = (TextView) findViewById(R.id.textViewStatus);

        //if BT is not enabled, request to enable it
        requestBluetoothAccess();

        sensor = new TgForceSensor(this.getApplicationContext(),this);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_tg_force, menu);
        return true;
    }

    @Override
    public void onDestroy(){
        sensor.close();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private static int REQUEST_ENABLE_BT = 56546;
    private void requestBluetoothAccess(){
        BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter btAdapter = btManager.getAdapter();
        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }


    @Override
    public void handleEvent(final TgForceEventType type, final double data) {
        if(textViewBattery == null || textViewPPA == null ||  textViewCadence == null){
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (type) {
                    case BATTERY_LEVEL:
                        textViewBattery.setText(String.format("Battery: %d %%", (int) data));
                        break;
                    case PPA:
                        textViewPPA.setText(String.format("PPA: %.02f g", data));
                        break;
                    case CADENCE:
                        textViewCadence.setText(String.format("Cadence: %d steps per minute", (int) data));
                        break;
                }
            }
        });
    }

    @Override
    public void handleStatusChanged(final TgForceStatus newStatus) {
        if(textViewStatus == null){
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (newStatus) {
                    case SEARCHING:
                        textViewStatus.setText("Searching for TgForce Sensor");
                        break;
                    case CONNECTED:
                        textViewStatus.setText("Connected to TgForce Sensor");
                        break;
                    case INITIALIZED:
                        textViewStatus.setText("TgForce Sensor initialized");
                        break;
                }
            }
        });
    }
}
