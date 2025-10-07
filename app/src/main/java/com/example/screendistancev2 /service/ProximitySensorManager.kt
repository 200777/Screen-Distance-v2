package com.example.screendistancev2.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * 스마트폰의 근접 센서를 관리하는 클래스.
 * 이 클래스의 핵심 역할은 배터리 소모가 큰 카메라를 항상 켜두는 대신,
 * 전력 소모가 거의 없는 근접 센서를 이용해 사용자가 폰을 얼굴 가까이 가져왔을 때만
 * 카메라를 활성화하도록 신호를 보내는 것입니다.
 */
class ProximitySensorManager(context: Context) {

    /**
     * 근접 센서의 상태 변경을 서비스에 알리기 위한 콜백 인터페이스
     */
    interface Listener {
        fun onProximityNear() // 센서가 '가까움'을 감지했을 때 호출
        fun onProximityFar()  // 센서가 '멂'을 감지했을 때 호출
    }

    private var listener: Listener? = null
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val proximitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private var isRegistered = false

    // SensorEventListener 구현체: 실제 센서 값의 변화를 감지합니다.
    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                // 근접 센서의 값은 values[0]에 cm 단위로 저장됩니다.
                val distance = it.values[0]

                // 센서의 측정값이 최대 범위(보통 5cm 또는 8cm)보다 작으면 '가까운' 상태로 간주합니다.
                // 0에 가까울수록 더 가깝다는 의미입니다.
                if (distance < (proximitySensor?.maximumRange ?: 5f)) {
                    listener?.onProximityNear()
                } else {
                    listener?.onProximityFar()
                }
            }
        }

        // 센서의 정확도가 변경될 때 호출되지만, 이 앱에서는 사용하지 않습니다.
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    /**
     * 외부(DistanceMonitoringService)에서 리스너를 등록하기 위한 함수
     */
    fun setListener(listener: Listener) {
        this.listener = listener
    }

    /**
     * 센서 감지를 시작합니다. 서비스가 시작될 때 호출됩니다.
     */
    fun register() {
        // 기기에 근접 센서가 있고, 아직 등록되지 않은 경우에만 리스너를 등록합니다.
        if (proximitySensor != null && !isRegistered) {
            sensorManager.registerListener(sensorEventListener, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
            isRegistered = true
        }
    }

    /**
     * 센서 감지를 중지합니다. 서비스가 종료될 때 호출되어 배터리 낭비를 막습니다.
     */
    fun unregister() {
        // 리스너가 등록된 상태일 때만 해제합니다.
        if (isRegistered) {
            sensorManager.unregisterListener(sensorEventListener)
            isRegistered = false
        }
    }
}