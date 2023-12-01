package com.antgroup.oss.gitprepushhook.util;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public final class NetworkUtil {
    public static String getMacAddress(){
        try {
            NetworkInterface en0 = NetworkInterface.getByName("en0");
            if (en0 != null) {
                return getMacAddress(en0.getHardwareAddress());
            } else {
                Enumeration<NetworkInterface> networkInterfaceEnumeration = NetworkInterface.getNetworkInterfaces();
                if (networkInterfaceEnumeration.hasMoreElements()) {
                    NetworkInterface first = networkInterfaceEnumeration.nextElement();
                    return getMacAddress(first.getHardwareAddress());
                } else {
                    return "";
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "";
    }
    private static String getMacAddress(byte[] hardwareAddress) {
        if (hardwareAddress == null) {
            return "";
        }
        String[] hexadecimalFormat = new String[hardwareAddress.length];
        for (int i = 0; i < hardwareAddress.length; i++) {
            hexadecimalFormat[i] = String.format("%02X", hardwareAddress[i]);
        }
        return String.join("-", hexadecimalFormat);
    }
}
