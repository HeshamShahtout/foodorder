package com.example.eatit.Common;

import com.example.eatit.Model.User;

public class Common {
    public static User currentUser;

    public static  String convertCodeToStatus(String code)
    {
        switch (code) {
            case "0":
                return "Placed";
            case "1":
                return "On my way";
            default:
                return "Shipped";
        }
    }
}
