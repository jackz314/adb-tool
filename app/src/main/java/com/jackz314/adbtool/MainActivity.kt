package com.jackz314.adbtool

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity() {

    private lateinit var openConnBtn: Button
    private lateinit var indicatorText: TextView
    private val TAG = "MainActivity"
    private var portNum = 5555
    private var hasWifi = false
    private var adbPort = -1
    private lateinit var networkCallback: NetworkCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        openConnBtn = findViewById(R.id.open_conn_btn)
        indicatorText = findViewById(R.id.indicator_text)

        hasWifi = isWiFiConnected()
        if (hasWifi) {
            setPortUI(adbWifiPort())
        }else{
            adbPort = adbWifiPort()
            indicatorText.text = if (adbPort != -1) "ADB Enabled but Wi-Fi not connected" else "ADB Disabled."
        }

        monitorWiFiState()

        openConnBtn.setOnClickListener {
            if (!hasWifi){
                hasWifi = isWiFiConnected()
                if (!hasWifi){
                    Log.d(TAG, "Detected again and still no Wi-Fi")
                    Toast.makeText(this, "Wi-Fi is not connected, please check your settings", Toast.LENGTH_SHORT).show()
                }else setPortUI(adbWifiPort())
            }else {
                if (adbPort != -1) closeConnection() else openConnection()
            }
        }
    }

    private fun openConnection(){
        Log.d(TAG, "Opening connection")
        indicatorText.text = "Enabling ADB..."
        openConnBtn.isEnabled = false
        Toast.makeText(this, "Enabling connection", Toast.LENGTH_SHORT).show()
        val returnCode = runRootCmd(arrayOf("setprop service.adb.tcp.port $portNum", "stop adbd", "start adbd"))
        var success = false
        when {
            returnCode==Integer.MIN_VALUE -> {//timeout
                Log.w(TAG, "SU process timeout")
                Toast.makeText(this, "Timeout requesting root privilege", Toast.LENGTH_SHORT).show()
            }
            returnCode == Integer.MAX_VALUE -> {//output null
                Log.e(TAG, "OutputStream null, unknown problem")
                Toast.makeText(this, "Encountered unknown problem, try again later", Toast.LENGTH_SHORT).show()
            }
            returnCode != 0 -> {//failed
                Log.e(TAG, "SU process failed: $returnCode")
                Toast.makeText(this, "Failed to request root privilege, please make sure your root is working correctly", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Log.d(TAG, "SU process finished successfully")
//                Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show()
                success = true
            }
        }
        openConnBtn.isEnabled = true
        if (success) setPortUI(adbWifiPort())
    }

    private fun closeConnection(){
        Log.d(TAG, "Closing connection")
        indicatorText.text = "Disabling ADB..."
        openConnBtn.isEnabled = false
        Toast.makeText(this, "Disabling connection", Toast.LENGTH_SHORT).show()
        val returnCode = runRootCmd(arrayOf("setprop service.adb.tcp.port -1", "stop adbd", "start adbd"))
        var success = false
        when {
            returnCode==Integer.MIN_VALUE -> {//timeout
                Log.w(TAG, "SU process timeout")
                Toast.makeText(this, "Timeout requesting root privilege", Toast.LENGTH_SHORT).show()
            }
            returnCode == Integer.MAX_VALUE -> {//output null
                Log.e(TAG, "OutputStream null, unknown problem")
                Toast.makeText(this, "Encountered unknown problem, try again later", Toast.LENGTH_SHORT).show()
            }
            returnCode != 0 -> {//failed
                Log.e(TAG, "SU process failed: $returnCode")
                Toast.makeText(this, "Failed to request root privilege, please make sure your root is working correctly", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Log.d(TAG, "SU process finished successfully")
                Toast.makeText(this, "Disabled!", Toast.LENGTH_SHORT).show()
                success = true
            }
        }
        openConnBtn.isEnabled = true
        if (success) setPortUI(adbWifiPort())
    }

    private fun setPortUI(port: Int){
        adbPort = port
        if(adbPort == -1){//disabled
            indicatorText.text = "ADB Disabled."
            openConnBtn.text = "Enable ADB over Wi-Fi"
        }else{//enabled
            val ip = getIPAddr()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                indicatorText.text = Html.fromHtml("ADB Enabled! Connect to device with <i>adb connect \n<b>$ip:$adbPort</b></i>", Html.FROM_HTML_MODE_COMPACT)
            }else{
                indicatorText.text = Html.fromHtml("ADB Enabled! Connect to device with <i>adb connect \n<b>$ip:$adbPort</b></i>")
            }
            openConnBtn.text = "Disable ADB over Wi-Fi"
        }
    }

    private fun setWiFiUI(connected: Boolean){
        if (connected){
            setPortUI(adbWifiPort())
        }else{
            adbPort = adbWifiPort()
            indicatorText.text = if (adbPort != -1) "ADB Enabled but Wi-Fi not connected" else "ADB Disabled."
            openConnBtn.text = "Detect Again"
        }
    }

    private fun monitorWiFiState() {
        val connMgr = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        connMgr?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val builder = NetworkRequest.Builder()
            builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            val networkRequest = builder.build()
            networkCallback = object: ConnectivityManager.NetworkCallback () {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    Log.i(TAG, "WiFi Available")
                    runOnUiThread {setWiFiUI(true)}
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    Log.i(TAG, "WiFi lost")
                    runOnUiThread {setWiFiUI(false)}
                }
            }
            connMgr.registerNetworkCallback(networkRequest, networkCallback)
        }
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {//unregister network state listener
            (applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?)?.unregisterNetworkCallback(networkCallback)
        }
        super.onDestroy()
    }
}
