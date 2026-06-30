package com.qlikverify.constants;

public enum APIErrorCodes {

    ez_devices_1000("EV.DEVICES:1000"),
    ez_devices_1001("EV.DEVICES:1001"),
    ez_devices_1002("EV.DEVICES:1002"),
    ez_devices_1003("EV.DEVICES:1003"),
    ez_devices_1004("EV.DEVICES:1004"),
    ez_devices_1005("EV.DEVICES:1005"),
    ez_devices_1006("EV.DEVICES:1006"),
    ez_devices_1007("EV.DEVICES:1007"),
    ez_devices_1008("EV.DEVICES:1008"),

    ez_transaction_1000("EV.TRANSACTION:1000");

    public final String value;

    APIErrorCodes(String value) {
        this.value = value;
    }

}
