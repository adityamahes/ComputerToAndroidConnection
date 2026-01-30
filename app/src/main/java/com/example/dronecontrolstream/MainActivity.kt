package com.example.dronecontrolstream

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: DroneViewModel

    private lateinit var btnUp: Button
    private lateinit var btnDown: Button
    private lateinit var btnLeft: Button
    private lateinit var btnRight: Button
    private lateinit var btnForward: Button
    private lateinit var btnBackward: Button
    private lateinit var btnStill: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[DroneViewModel::class.java]

        initViews()
        setupListeners()
        observeViewModel()
    }

    private fun initViews() {
        btnUp = findViewById(R.id.btnUp)
        btnDown = findViewById(R.id.btnDown)
        btnLeft = findViewById(R.id.btnLeft)
        btnRight = findViewById(R.id.btnRight)
        btnForward = findViewById(R.id.btnForward)
        btnBackward = findViewById(R.id.btnBackward)
        btnStill = findViewById(R.id.btnStill)

    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners() {
        val buttons = mapOf(
            btnUp to "UP",
            btnDown to "DOWN",
            btnLeft to "LEFT",
            btnRight to "RIGHT",
            btnForward to "FORWARD",
            btnBackward to "BACKWARD"
        )

        buttons.forEach { (button, command) ->
            button.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        viewModel.onButtonDown(command)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        viewModel.onButtonUp(command)
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {


                launch {
                    viewModel.uiState.collect { state ->
                        updateButtonColor(btnUp, state.up)
                        updateButtonColor(btnDown, state.down)
                        updateButtonColor(btnLeft, state.left)
                        updateButtonColor(btnRight, state.right)
                        updateButtonColor(btnForward, state.forward)
                        updateButtonColor(btnBackward, state.backward)
                        updateButtonColor(btnStill, state.still, Color.GREEN) 
                    }
                }
            }
        }
    }

    private fun updateButtonColor(button: Button, isActive: Boolean, activeColor: Int = Color.CYAN) {
        val color = if (isActive) {
            activeColor
        } else {
            Color.parseColor("#333333") // Idle color (Dark Grey)
        }
        button.backgroundTintList = ColorStateList.valueOf(color)
    }
}
