package com.jackz314.adbtool

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import java.math.BigInteger
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.ByteOrder


private val TAG = "RootUtils"

@Suppress("DEPRECATION")//have to use deprecated methods
fun isWiFiConnected(): Boolean {
    /*val wifiMgr =  MyApplication.context!!.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
    if (wifiMgr == null) {
        Log.e(TAG, "WiFi Manager is null")
        return false
    }
    Log.i(TAG, "WIFI MANAGER: " + wifiMgr.isWifiEnabled + wifiMgr.connectionInfo.toString())
    return if (wifiMgr.isWifiEnabled) { // Wi-Fi adapter is ON
        val wifiInfo = wifiMgr.connectionInfo
        wifiInfo.networkId != -1 // -1 means not connected, otherwise connected
    } else {
        false // Wi-Fi adapter is OFF
    }*///backup method
    val connMgr = MyApplication.context!!.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
    connMgr?: return false
    /*if (connMgr == null) {
        Log.e(TAG, "Connectivity Manager is null")
        return false
    }*/
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network: Network = connMgr.activeNetwork ?: return false
        val capabilities = connMgr.getNetworkCapabilities(network)
        Log.i(TAG, "CONN MANAGER: $capabilities")
        return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    } else {
        val networkInfo = connMgr.activeNetworkInfo ?: return false
        return networkInfo.isConnected && networkInfo.type == ConnectivityManager.TYPE_WIFI
    }
}

fun getIPAddr(): String{
    val wm = MyApplication.context!!.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    var ip: Int = wm.connectionInfo.ipAddress
    ip = if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) Integer.reverseBytes(ip) else ip
    val ipBytes: ByteArray = BigInteger.valueOf(ip.toLong()).toByteArray()
    try {
        val ipObj: InetAddress = InetAddress.getByAddress(ipBytes)
        return ipObj.hostAddress
    } catch (e: UnknownHostException) {
        Log.e(TAG, "Error getting WiFi IP address ", e)
    }

    /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val connMgr = MyApplication.context!!.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        connMgr?: return "NULL"
        val network: Network = connMgr.activeNetwork ?: return "NULL"
        val properties = connMgr.getLinkProperties(network) ?: return "NULL"
        Log.i(TAG,"Properties: $properties")
        for (linkAddress in properties.linkAddresses){
            val address = linkAddress.address
            if(address.isSiteLocalAddress){
                Log.d(TAG, "Got local IP: $address")
                return linkAddress.address.hostAddress
            }
        }
    } else {
        val wm = MyApplication.context!!.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        var ip: Int = wm.connectionInfo.ipAddress
        ip = if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) Integer.reverseBytes(ip) else ip
        val ipBytes: ByteArray = BigInteger.valueOf(ip.toLong()).toByteArray()
        try {
            val ipObj: InetAddress = InetAddress.getByAddress(ipBytes)
            return ipObj.hostAddress
        } catch (e: UnknownHostException) {
            Log.e(TAG, "Error getting WiFi IP address ", e)
        }
    }
*/
    return "NULL"
}

//get adb over wifi port config, -1 if not open or error
fun adbWifiPort(): Int{
    val portOutput = runCmdOut("getprop service.adb.tcp.port", 1500)
    if (portOutput == null || portOutput == "" || portOutput == "-1") return -1
    else try {
        return portOutput.toInt()
    } catch(e:  NumberFormatException) {
        Log.e(TAG, "Port Number error: " + e.message)
        e.printStackTrace()
        return -1
    }
}