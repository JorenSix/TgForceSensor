package be.ugent.ipem.tgforcesensor;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.UUID;

/**
 * Created by joren on 9/21/15.
 */
public enum TgForceProtocol {

    SERVICE_TGFORCE(UUID.fromString("f6531000-9ce4-450d-b276-3b5a88fa2e73")),

    CHAR_PPA(UUID.fromString("f6531010-9ce4-450d-b276-3b5a88fa2e73")),
    CHAR_CADENCE(UUID.fromString("f6531014-9ce4-450d-b276-3b5a88fa2e73")),
    CHAR_DELAY(UUID.fromString("f6531013-9ce4-450d-b276-3b5a88fa2e73")),
    CHAR_GTHRESHOLD(UUID.fromString("f6531011-9ce4-450d-b276-3b5a88fa2e73")),


    /**
     * Notification/indication configuration, see here:
     * https://developer.bluetooth.org/gatt/descriptors/Pages/DescriptorViewer.aspx?u=org.bluetooth.descriptor.gatt.client_characteristic_configuration.xml
     */
    DESCR_CLIENT_CHAR_CONFIG(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")),


    SERVICE_BATTERY(UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")),

    CHAR_BATTERY_LEVEL(UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb"));

    private final UUID uuid ;
    TgForceProtocol(UUID uuid){
        this.uuid = uuid;
    }

    public UUID getUUID(){
        return uuid;
    }



    public boolean is(BluetoothGattCharacteristic characteristic){
        return characteristic.getUuid().equals(uuid);
    }
}
