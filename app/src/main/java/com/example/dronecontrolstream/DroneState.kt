package com.example.dronecontrolstream

data class DroneButtonState(
    val up: Boolean = false,
    val down: Boolean = false,
    val left: Boolean = false,
    val right: Boolean = false,
    val forward: Boolean = false,
    val backward: Boolean = false,
    val still: Boolean = true
)
