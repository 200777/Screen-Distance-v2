package com.example.screendistancev2.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.screendistancev2.MainActivity
import com.example.screendistancev2.R
import kotlinx.coroutines.*

class DistanceMonitoringService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private lateinit var proximitySensorManager: ProximitySensorManager
    private lateinit var cameraManager: CameraManager
    private lateinit var warningOverlay: WarningOverlay
    private lateinit var lifecycleOwner: ServiceLifecycleOwner

    // 사용자가 설정할 수 있는 값 (지금은 30cm로 고정)
    private val warningDistanceCm = 30.0f

    override fun onCreate() {
        super.onCreate()
        // CameraX가 서비스 환경에서 작동하기 위해 필요한 LifecycleOwner를 초기화합니다.
        lifecycleOwner = ServiceLifecycleOwner(this)
        lifecycleOwner.onServiceCreate()

        warningOverlay = WarningOverlay(this)
        // CameraManager를 생성할 때 LifecycleOwner를 전달합니다.
        cameraManager = CameraManager(this, lifecycleOwner)
        proximitySensorManager = ProximitySensorManager(this)

        setupProximitySensor()
    }

    private fun setupProximitySensor() {
        proximitySensorManager.setListener(object : ProximitySensorManager.Listener {
            override fun onProximityNear() {
                // 근접 센서가 '가까움'을 감지하면 카메라와 얼굴 인식을 시작합니다.
                startFaceDetection()
            }

            override fun onProximityFar() {
                // 근접 센서가 '멂'을 감지하면 카메라를 끄고 경고창을 숨겨 배터리를 절약합니다.
                stopFaceDetection()
                warningOverlay.hide()
            }
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleOwner.onServiceStart() // 서비스가 시작되었음을 Lifecycle에 알립니다.

        when (intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
        }
        return START_STICKY // 서비스가 강제 종료되어도 시스템이 다시 시작하도록 설정
    }

    private fun start() {
        startForeground(NOTIFICATION_ID, createNotification())
        proximitySensorManager.register()
    }

    private fun stop() {
        proximitySensorManager.unregister()
        stopFaceDetection()
        warningOverlay.hide()
        stopForeground(STOP_FOREGROUND_REMOVE) // 알림을 제거하며 포그라운드 상태를 종료
        stopSelf()
    }

    private fun startFaceDetection() {
        // 얼굴 인식은 IO 스레드에서 비동기적으로 처리합니다.
        scope.launch {
            cameraManager.startFaceDetection { distance ->
                // 거리 측정 결과를 UI 스레드에서 처리하여 경고창을 띄웁니다.
                MainScope().launch {
                    if (distance > 0 && distance < warningDistanceCm) {
                        warningOverlay.show(distance)
                    } else {
                        warningOverlay.hide()
                    }
                }
            }
        }
    }

    private fun stopFaceDetection() {
        cameraManager.stopFaceDetection()
    }

    private fun createNotification(): android.app.Notification {
        val channelId = "distance_service_channel"
        // 안드로이드 Oreo (API 26) 이상에서는 알림 채널 생성이 필수입니다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Screen Distance Service",
                NotificationManager.IMPORTANCE_LOW // 사용자에게 방해되지 않는 낮은 중요도
            ).apply {
                description = "화면 거리 감지 서비스가 실행 중임을 알립니다."
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        // 알림을 클릭했을 때 MainActivity를 열도록 설정합니다.
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("화면 거리 감지 서비스")
            .setContentText("백그라운드에서 실행 중입니다.")
            .setSmallIcon(R.drawable.ic_eye)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // 사용자가 쉽게 지울 수 없도록 설정
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        lifecycleOwner.onServiceDestroy() // 서비스가 종료됨을 Lifecycle에 알립니다.
        job.cancel() // 모든 코루틴 작업을 취소합니다.
        proximitySensorManager.unregister() // 센서 리스너를 해제하여 메모리 누수를 방지합니다.
        cameraManager.stopFaceDetection() // 카메라를 확실히 종료합니다.
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        private const val NOTIFICATION_ID = 1
    }
}