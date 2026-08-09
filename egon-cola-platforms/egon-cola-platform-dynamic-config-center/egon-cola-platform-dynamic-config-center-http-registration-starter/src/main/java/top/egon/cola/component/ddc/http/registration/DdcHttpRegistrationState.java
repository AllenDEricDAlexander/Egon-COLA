package top.egon.cola.component.ddc.http.registration;

public enum DdcHttpRegistrationState {

    NEW,
    WAITING_SERVER,
    REGISTERING,
    REGISTERED,
    RECOVERING,
    FAILED,
    STOPPED
}
