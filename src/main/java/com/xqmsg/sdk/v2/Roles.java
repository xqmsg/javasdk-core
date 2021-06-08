package com.xqmsg.sdk.v2;



/**
 * Created by ikechie on 2/3/20.
 */

public enum Roles {

    Unkown(0),
    Admin(1),
    User(2),
    Customer(3),
    Vendor(4),
    SuperUser(5),
    Device(6),
    Alias(7);


    private final int id;

    Roles(int id) {
        this.id = id;
    }

    public int getCode() {
        return id;
    }

    public static Roles valueOf(Integer code) {
        switch (code) {
            case 1: return Admin;
            case 2: return User;
            case 3: return Customer;
            case 4: return Vendor;
            case 5: return SuperUser;
            case 6: return Device;
            case 7: return Alias;
            default: return Unkown;
        }
    }

}
