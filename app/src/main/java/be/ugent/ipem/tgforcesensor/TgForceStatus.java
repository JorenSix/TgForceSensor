package be.ugent.ipem.tgforcesensor;

/**
 * Created by joren on 10/1/15.
 */
public enum TgForceStatus {
    SEARCHING,//when searching for a connection with the sensor
    CONNECTED,//Connected with the sensor but not initialized (notifications are not enabled)
    INITIALIZED,////Connected with  adn notifications are enabled.
}
