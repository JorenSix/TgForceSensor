package be.ugent.ipem.tgforcesensor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.List;

/**
 * Created by joren on 9/16/15.
 */
public class TgForceSensor {

    private static final String TAG = "TgForceSensor" ;
    private BluetoothDevice tgForceDevice;
    private BluetoothGatt bluetoothGatt;
    private final Context context;
    private BluetoothAdapter btAdapter;
    private final TgForceEventHandler eventHandler;
    private TgForceStatus status;


    public TgForceSensor(Context context,TgForceEventHandler  eventHandler ){
        this.context = context;
        this.eventHandler = eventHandler;
        status=TgForceStatus.SEARCHING;
        eventHandler.handleStatusChanged(status);

        BluetoothManager btManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

        btAdapter = btManager.getAdapter();

        Log.i(TAG, "Start LE SCAn");
        btAdapter.startLeScan(leScanCallback);
    }


    public void close(){
        if (bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            Log.i(TAG,"Bluetooth device found : " + device.getName());
            if("TgForce152".equalsIgnoreCase(device.getName())){
                BluetoothManager btManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

                btAdapter = btManager.getAdapter();
                btAdapter.stopLeScan(this);

                tgForceDevice = device;

                Log.i(TAG,"Found TgForce152 , stopping le scan and connecting gatt callback");

                //only connect the first time!
                bluetoothGatt = tgForceDevice.connectGatt(context, false, btleGattCallback);
            }
        }
    };

    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {

        @Override
        public  void  onCharacteristicWrite(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic, int status){
            Log.i(TAG, "onCharacteristicWrite status: " + status + " characteristic " + characteristic.getUuid().toString());
        }

        @Override
        public  void  onCharacteristicRead(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic, int status){
            Log.i(TAG, "onCharacteristicRead status: " + status + " characteristic " + characteristic.getUuid().toString());
            Log.i(TAG, characteristic.getStringValue(0));

            state++;

            byte[] values = characteristic.getValue();
            if(TgForceProtocol.CHAR_BATTERY_LEVEL.is(characteristic)) {
                int integerValue = values[0];//A value from zero to -128 to 127 255
                Log.i(TAG, "BATTERY LEVEL:  " + integerValue + "%");
            }else if(TgForceProtocol.CHAR_DELAY.is(characteristic)) {
                int integerValue = values[0] & 0x000000ff;//A value from zero to -128 to 127 255
                Log.i(TAG, "Delay:  " + integerValue + "");
            }else if(TgForceProtocol.CHAR_GTHRESHOLD.is(characteristic)) {
                int integerValue = values[0] & 0x000000ff;//A value from zero to -128 to 127 255
                Log.i(TAG, "Threshold:  " + integerValue + "");
            }

            if(state==3){
                BluetoothGattService tgForceService = bluetoothGatt.getService(TgForceProtocol.SERVICE_TGFORCE.getUUID());
                BluetoothGattCharacteristic thresholdChar = tgForceService.getCharacteristic(TgForceProtocol.CHAR_GTHRESHOLD.getUUID());
                bluetoothGatt.readCharacteristic(thresholdChar) ;
            }else if(state==4){
                BluetoothGattService tgForceService = bluetoothGatt.getService(TgForceProtocol.SERVICE_TGFORCE.getUUID());
                BluetoothGattCharacteristic delayChar = tgForceService.getCharacteristic(TgForceProtocol.CHAR_DELAY.getUUID());
                bluetoothGatt.readCharacteristic(delayChar);
            }
        }

        @Override
        public  void  onDescriptorRead(BluetoothGatt gatt,
                                           BluetoothGattDescriptor characteristic, int status){
            Log.i(TAG, "onDiscriptorRead status: " + status + " characteristic " + characteristic.getUuid().toString());

            try {
                Log.i(TAG, new String(characteristic.getValue(), "UTF-8"));
            }catch (Exception e){
                Log.w(TAG, e.getMessage());
            }
        }

        int state = 0;
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                      int status) {
            Log.i(TAG, "onDescriptorWrite status: " + status + " descriptor " + descriptor.getUuid());

            state++;

            if(state == 1 ){
                BluetoothGattService tgForceService = bluetoothGatt.getService(TgForceProtocol.SERVICE_TGFORCE.getUUID());
                BluetoothGattCharacteristic cadenceChar = tgForceService.getCharacteristic(TgForceProtocol.CHAR_CADENCE.getUUID());
                enableNotification(cadenceChar,"Cadence");
            }else if(state == 2){
                BluetoothGattService batteryService = bluetoothGatt.getService(TgForceProtocol.SERVICE_BATTERY.getUUID());
                BluetoothGattCharacteristic batteryLevelChar = batteryService.getCharacteristic(TgForceProtocol.CHAR_BATTERY_LEVEL.getUUID());
                enableNotification(batteryLevelChar,"Battery");
                //bluetoothGatt.readCharacteristic(batteryLevelChar) ;
            }else if(state == 3){
                TgForceSensor.this.status=TgForceStatus.INITIALIZED;
                eventHandler.handleStatusChanged(TgForceSensor.this.status);
            }
        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            byte[] values = characteristic.getValue();

            if(TgForceProtocol.CHAR_PPA.is(characteristic)) {
                int integerValue = values[0] & 0x000000ff;//A value from zero to -128 to 127 255
                //this range is converted to g (the impact range is from 1g to 23.9g so 0-255 is either [1-23.9g] or[0-2.39g]
                //conversion with [0-23.9g]
                //conversion factor of 0.09
                double factor = 23.9141/ 254;
                double ppaInG = integerValue * factor;
                if(eventHandler!=null){
                    eventHandler.handleEvent(TgForceEventType.PPA, ppaInG);
                }
                Log.i(TAG, "PPA:  " + ppaInG + "g (int value: " + integerValue + ")");
            }

            if(TgForceProtocol.CHAR_BATTERY_LEVEL.is(characteristic)) {
                int integerValue = values[0] & 0x000000ff;//A value from zero to -128 to 127 255
                Log.i(TAG, "BATTERY LEVEL:  " + integerValue + "%");
                if(eventHandler!=null){
                    eventHandler.handleEvent(TgForceEventType.BATTERY_LEVEL,integerValue);
                }
            }

            if(TgForceProtocol.CHAR_CADENCE.is(characteristic)) {
                int integerValue = values[0] & 0x000000ff;//A value from zero to -128 to 127 255
                if(eventHandler!=null){
                    eventHandler.handleEvent(TgForceEventType.CADENCE,integerValue);
                }
                Log.i(TAG, "Cadence:  " + integerValue + " steps per minute");
            }
        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            Log.i(TAG, "onConnectionStateChange");
            if(newState == BluetoothProfile.STATE_CONNECTED){
                Log.i(TAG, "connected");
                TgForceSensor.this.status=TgForceStatus.CONNECTED;
                eventHandler.handleStatusChanged(TgForceSensor.this.status);

                bluetoothGatt.discoverServices();
            }else if(newState == BluetoothProfile.STATE_DISCONNECTED || newState == BluetoothProfile.STATE_DISCONNECTING){
                //try to reconnect!
                Log.w(TAG, "Disconnecting or disconnected!");
                Log.w(TAG, "Start LE SCAN- Reconnect");
                state = 0;
                btAdapter.startLeScan(leScanCallback);
                bluetoothGatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            Log.i(TAG, "onServicesDiscovered");
            BluetoothGattService tgForceService = bluetoothGatt.getService(TgForceProtocol.SERVICE_TGFORCE.getUUID());
            BluetoothGattCharacteristic ppaChar = tgForceService.getCharacteristic(TgForceProtocol.CHAR_PPA.getUUID());
            enableNotification(ppaChar,"PPA");
        }

        private void enableNotification(BluetoothGattCharacteristic characteristic,String name){
            if (characteristic != null) {
                BluetoothGattDescriptor notification =characteristic.getDescriptor(TgForceProtocol.DESCR_CLIENT_CHAR_CONFIG.getUUID());
                if (notification != null) {
                    bluetoothGatt.setCharacteristicNotification(characteristic, true);
                    notification.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    bluetoothGatt.writeDescriptor(notification);
                    Log.i(TAG,"Requested notifications for: " + name);
                } else {
                    Log.e(TAG, "Unable to start notifications on TgForce " + name + " characteristic!");
                    printServices();
                }
            } else {
                Log.e(TAG, "Unable to find TgForce " + name + " characteristic!");
            }
        }

        private void printServices(){
            List<BluetoothGattService> services = bluetoothGatt.getServices();
            for (BluetoothGattService service : services) {
                Log.i(TAG,"Found service " + service.getUuid());
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                for(BluetoothGattCharacteristic characteristic: characteristics) {
                    Log.i(TAG, "----Found characteristic " + characteristic.getUuid() + "  instanceid " + characteristic.getInstanceId() + " permissions: " +  characteristic.getPermissions() + " write type " + characteristic.getWriteType());
                    for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                        Log.i(TAG,"--------Found descriptor " + descriptor.getUuid().toString() + " permissions: " + descriptor.getPermissions() );
                    }
                }
            }
        }
    };
}
