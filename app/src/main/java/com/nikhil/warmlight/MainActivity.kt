package com.nikhil.warmlight

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var powerSwitch: Switch
    private lateinit var warmthSeekBar: SeekBar
    private lateinit var warmthLabel: TextView
    private lateinit var previewSwatch: View

    // Launched after the user returns from the "draw over other apps" settings screen
    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Settings.canDrawOverlays(this)) {
                proceedToEnable()
            } else {
                powerSwitch.isChecked = false
            }
        }

    // Android 13+ notification permission, needed for the foreground-service notification
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startOverlay() else powerSwitch.isChecked = false
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("warmlight_prefs", MODE_PRIVATE)

        powerSwitch = findViewById(R.id.switchPower)
        warmthSeekBar = findViewById(R.id.seekWarmth)
        warmthLabel = findViewById(R.id.textWarmthValue)
        previewSwatch = findViewById(R.id.viewPreview)

        val savedIntensity = prefs.getInt(KEY_INTENSITY, 55)
        warmthSeekBar.progress = savedIntensity
        warmthLabel.text = getString(R.string.warmth_value, savedIntensity)
        updatePreview(savedIntensity)

        // Reflect real state: only show "on" if we actually still hold the overlay permission
        powerSwitch.isChecked = prefs.getBoolean(KEY_ENABLED, false) && Settings.canDrawOverlays(this)

        powerSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) enableOverlay() else disableOverlay()
        }

        warmthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                warmthLabel.text = getString(R.string.warmth_value, progress)
                updatePreview(progress)
                prefs.edit().putInt(KEY_INTENSITY, progress).apply()
                if (powerSwitch.isChecked) sendIntensityUpdate(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun enableOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
            return
        }
        proceedToEnable()
    }

    private fun proceedToEnable() {
        val needsNotificationPermission =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED

        if (needsNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startOverlay()
        }
    }

    private fun startOverlay() {
        prefs.edit().putBoolean(KEY_ENABLED, true).apply()
        val intent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_START
            putExtra(OverlayService.EXTRA_INTENSITY, warmthSeekBar.progress)
        }
        ContextCompat.startForegroundService(this, intent)
        powerSwitch.isChecked = true
    }

    private fun disableOverlay() {
        prefs.edit().putBoolean(KEY_ENABLED, false).apply()
        startService(Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_STOP
        })
    }

    private fun sendIntensityUpdate(progress: Int) {
        startService(Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_UPDATE
            putExtra(OverlayService.EXTRA_INTENSITY, progress)
        })
    }

    private fun updatePreview(intensity: Int) {
        previewSwatch.setBackgroundColor(OverlayService.warmColorFor(intensity))
    }

    companion object {
        private const val KEY_ENABLED = "enabled"
        private const val KEY_INTENSITY = "intensity"
    }
}
