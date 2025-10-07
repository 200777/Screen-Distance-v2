package com.example.screendistancev2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.screendistancev2.databinding.ActivityMainBinding
import com.example.screendistancev2.service.DistanceMonitoringService
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                checkNotificationPermission()
            } else {
                showPermissionRationale("카메라 권한이 필요합니다.", "camera")
            }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                checkOverlayPermission()
            } else {
                Toast.makeText(this, "알림 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
                checkOverlayPermission()
            }
        }

    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Settings.canDrawOverlays(this)) {
                startMonitoringService()
            } else {
                showPermissionRationale("다른 앱 위에 표시 권한이 필요합니다.", "overlay")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        binding.switchService.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkAndRequestPermissions()
            } else {
                stopMonitoringService()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            checkNotificationPermission()
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                checkOverlayPermission()
            }
        } else {
            checkOverlayPermission()
        }
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        } else {
            startMonitoringService()
        }
    }

    private fun startMonitoringService() {
        Toast.makeText(this, "거리 감지 서비스를 시작합니다.", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, DistanceMonitoringService::class.java).apply {
            action = DistanceMonitoringService.ACTION_START
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopMonitoringService() {
        Toast.makeText(this, "거리 감지 서비스를 중지합니다.", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, DistanceMonitoringService::class.java).apply {
            action = DistanceMonitoringService.ACTION_STOP
        }
        startService(intent)
    }

    private fun showPermissionRationale(message: String, type: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("권한 필요")
            .setMessage(message)
            .setPositiveButton("설정으로 이동") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("취소") { dialog, _ ->
                binding.switchService.isChecked = false
                dialog.dismiss()
            }
            .show()
    }
}