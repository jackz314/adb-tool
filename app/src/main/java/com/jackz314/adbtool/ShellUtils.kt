package com.jackz314.adbtool

import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.lang.StringBuilder


private val TAG = "ShellUtils"

/* static helper methods */

//run root command
fun runRootCmd(cmds: Array<String>): Int{
    val suProc = Runtime.getRuntime().exec("su")

    if(suProc.outputStream != null){
        val suOut = DataOutputStream(suProc.outputStream)
        for (cmd in cmds) suOut.writeBytes(cmd+"\n")
        suOut.writeBytes("exit\n")
        suOut.flush()
        suOut.close()
    }else{ // OutputStream null, unknown problem
        return Integer.MAX_VALUE
    }

    val waitSuProc = ProcessWithTimeout(suProc)
    return waitSuProc.waitForProcess(60000)
}

//run root command with stdout, return null if error
fun runRootCmdOut(cmd: String): String?{
    val suProc = Runtime.getRuntime().exec("su")
    var output: String?
    if(suProc.outputStream != null){
        val suOut = DataOutputStream(suProc.outputStream)
        suOut.writeBytes(cmd+"\n")
        suOut.writeBytes("exit\n")
        suOut.flush()
        suOut.close()
        val stderr = suProc.errorStream
        val stdout = suProc.inputStream
        var bufferReader = BufferedReader(InputStreamReader(stdout))
        var line: String?
        val outputBuilder = StringBuilder()
        while (bufferReader.readLine().also { line = it } != null) {
            Log.d(TAG, "[Output] [$line]")
            outputBuilder.append(line)
        }
        bufferReader.close()
        bufferReader = BufferedReader(InputStreamReader(stderr))
        while (bufferReader.readLine().also { line = it } != null) {
            Log.e(TAG, "[Error] [$line]")
        }
        bufferReader.close()
        output = outputBuilder.toString()
    }else{ // OutputStream null, unknown problem
        Log.e(TAG, "OutputStream null, unknown problem")
        output = null
    }
    val waitSuProc = ProcessWithTimeout(suProc)
    val returnCode = waitSuProc.waitForProcess(60000)
    if (returnCode != 0){
        Log.e(TAG, "Return code is not zero: $returnCode")
        output = null
    }
    return output
}

fun runCmdOut(cmd: String, timeoutMillSec: Int = 5000): String?{
    val suProc = Runtime.getRuntime().exec(cmd)
    var output: String?
    val stderr = suProc.errorStream
    val stdout = suProc.inputStream
    var bufferReader = BufferedReader(InputStreamReader(stdout))
    var line: String?
    val outputBuilder = StringBuilder()
    while (bufferReader.readLine().also { line = it } != null) {
        Log.d(TAG, "[Output] [$line]")
        outputBuilder.append(line)
    }
    bufferReader.close()
    bufferReader = BufferedReader(InputStreamReader(stderr))
    while (bufferReader.readLine().also { line = it } != null) {
        Log.e(TAG, "[Error] [$line]")
    }
    bufferReader.close()
    output = outputBuilder.toString()
    val waitSuProc = ProcessWithTimeout(suProc)
    val returnCode = waitSuProc.waitForProcess(timeoutMillSec)
    if (returnCode != 0){
        Log.e(TAG, "Return code is not zero: $returnCode")
        output = null
    }
    return output
}