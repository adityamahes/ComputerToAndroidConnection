package com.example.dronecontrolstream

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException

class TcpServer(
    private val onCommandReceived: (String) -> Unit
) {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val TAG = "TcpServer"

    suspend fun start(port: Int) = withContext(Dispatchers.IO) {
        try {
            serverSocket = ServerSocket(port)
            isRunning = true
            Log.d(TAG, "Server started on port $port")

            while (isRunning) {
                try {
                    val clientSocket = serverSocket?.accept()
                    clientSocket?.let { socket ->
                        Log.d(TAG, "Client connected: ${socket.inetAddress.hostAddress}")
                        handleClient(socket)
                    }
                } catch (e: Exception) {
                    if (isRunning) {
                        Log.e(TAG, "Error accepting connection", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting server", e)
        }
    }

    private suspend fun handleClient(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), "UTF-8"))
            var line: String?

            while (isRunning && socket.isConnected) {
                line = reader.readLine()
                if (line != null) {
                    val command = line.trim()
                    Log.d(TAG, "Received command: $command")
                    onCommandReceived(command)
                } else {
                    // Start of client closing connection
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling client", e)
        } finally {
            try {
                socket.close()
                Log.d(TAG, "Client disconnected")
            } catch (e: Exception) {
                Log.e(TAG, "Error closing client socket", e)
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping server", e)
        }
    }
}
