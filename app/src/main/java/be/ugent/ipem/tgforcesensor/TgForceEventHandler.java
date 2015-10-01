package be.ugent.ipem.tgforcesensor;

/**
 * Created by joren on 10/1/15.
 */
public interface TgForceEventHandler {
    void handleEvent(TgForceEventType type, double data);
    void handleStatusChanged(TgForceStatus newStatus);
}
