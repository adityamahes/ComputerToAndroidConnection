package com.example.dronecontrolstream

import android.app.Application
import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.net.InetAddress
import java.nio.ByteOrder

class DroneViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DroneButtonState())
    val uiState: StateFlow<DroneButtonState> = _uiState.asStateFlow()

    private val _ipAddress = MutableStateFlow("Unknown IP")
    val ipAddress: StateFlow<String> = _ipAddress.asStateFlow()

    private val tcpServer = TcpServer { command ->
        processCommand(command)
    }

    // Keep track of active reset jobs to cancel them if re-triggered
    private var resetJobs = mutableMapOf<String, Job>()
    
    // Timer job to switch back to STILL state if no commands received
    private var stillStateJob: Job? = null

    init {
        getIpAddress()
        startServer()
    }

    private fun startServer() {
        viewModelScope.launch {
            tcpServer.start(8080)
        }
    }

    private fun getIpAddress() {
        viewModelScope.launch(Dispatchers.IO) {
            val ip = getIpAddressFromInterfaces()
            _ipAddress.value = ip
        }
    }

    private fun getIpAddressFromInterfaces(): String {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        val ip = address.hostAddress ?: "Unknown IP"
                        if (ip == "10.0.2.15") {
                            return "$ip (Emulator)\nUse: adb forward tcp:8080 tcp:8080"
                        }
                        return ip
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DroneViewModel", "Error getting IP", e)
        }
        return "No Network IP"
    }

    // TCP Command Processor (Stream 5Hz usually)
    private fun processCommand(command: String) {
        val cmd = command.uppercase().trim()
        if (cmd.isNotEmpty()) {
            activateButton(cmd)
        }
    }
    
    // Manual Control: Button Pressed (Stream Start)
    fun onButtonDown(command: String) {
        activateButton(command, isManual = true)
    }

    // Manual Control: Button Released (Stream End)
    fun onButtonUp(command: String) {
         updateState(command, false)
         scheduleStillState()
    }

    private fun activateButton(command: String, isManual: Boolean = false) {
        // Cancel pending still state transition
        stillStateJob?.cancel()
        
        // Cancel existing reset job for this command if any
        resetJobs[command]?.cancel()

        // Set state to true
        updateState(command, true)

        // For TCP (non-manual), we expect a stream. If stream stops, we should reset.
        // If it's a 5Hz stream (200ms), we wait slightly longer (e.g. 300ms) before resetting.
        if (!isManual) {
            val job = viewModelScope.launch {
                delay(300) 
                updateState(command, false)
                scheduleStillState()
            }
            resetJobs[command] = job
        }
    }

    private fun scheduleStillState() {
        // Check if all buttons are off, then enable STILL
        // We delay slightly to allow for fast switching or stream gaps
        stillStateJob?.cancel()
        stillStateJob = viewModelScope.launch {
            delay(50) // Short buffer
             _uiState.update { currentState ->
                 val anyActive = currentState.up || currentState.down || currentState.left || 
                                currentState.right || currentState.forward || currentState.backward
                 currentState.copy(still = !anyActive)
             }
        }
    }

    private fun updateState(command: String, isActive: Boolean) {
        _uiState.update { currentState ->
            val newState = when (command) {
                "UP" -> currentState.copy(up = isActive)
                "DOWN" -> currentState.copy(down = isActive)
                "LEFT" -> currentState.copy(left = isActive)
                "RIGHT" -> currentState.copy(right = isActive)
                "FORWARD" -> currentState.copy(forward = isActive)
                "BACKWARD" -> currentState.copy(backward = isActive)
                else -> currentState
            }
            // If any button is active, STILL must be false immediately
            if (isActive) {
                newState.copy(still = false)
            } else {
                newState // Wait for scheduleStillState to set it back to true
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tcpServer.stop()
    }
}
