package com.example.screendistancev2.service

import android.app.Service
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ServiceLifecycleDispatcher

/**
 * 서비스(Service)가 라이프사이클 소유자(LifecycleOwner)처럼 행동할 수 있도록 만들어주는 헬퍼 클래스.
 *
 * 안드로이드의 CameraX 라이브러리는 액티비티나 프래그먼트의 생명주기(Lifecycle)에 맞춰
 * 카메라를 자동으로 켜고 끄도록 설계되었습니다.
 * 하지만 일반적인 서비스는 이러한 생명주기 정보를 가지고 있지 않습니다.
 *
 * 이 클래스는 서비스의 생명주기 이벤트(onCreate, onStart, onDestroy)를
 * CameraX가 이해할 수 있는 Jetpack Lifecycle 이벤트로 '번역'하고 전달해주는
 * '번역기' 또는 '어댑터' 역할을 합니다.
 *
 * 이를 통해 서비스가 종료될 때 CameraX 리소스가 안전하게 해제되도록 보장하여,
 * 앱의 안정성을 높이고 메모리 누수 및 배터리 낭비를 방지합니다.
 */
class ServiceLifecycleOwner(service: Service) : LifecycleOwner {
    // Service의 Lifecycle 이벤트를 관리하는 디스패처
    private val dispatcher = ServiceLifecycleDispatcher(this)

    // 실제 Lifecycle 상태를 저장하고 관리하는 레지스트리
    private val lifecycleRegistry = LifecycleRegistry(this)

    init {
        // 생성자에서 바로 ON_CREATE 이벤트를 전달할 준비를 합니다.
        dispatcher.onServicePreSuperOnCreate()
    }

    /**
     * DistanceMonitoringService의 onCreate()에서 호출되어야 합니다.
     * Lifecycle 상태를 CREATED로 설정합니다.
     */
    fun onServiceCreate() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    /**
     * DistanceMonitoringService의 onStartCommand()에서 호출되어야 합니다.
     * Lifecycle 상태를 STARTED로 설정합니다. CameraX는 이 상태에서 카메라를 활성화합니다.
     */
    fun onServiceStart() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    /**
     * DistanceMonitoringService의 onDestroy()에서 호출되어야 합니다.
     * Lifecycle 상태를 DESTROYED로 설정합니다. CameraX는 이 상태에서 카메라를 안전하게 해제합니다.
     */
    fun onServiceDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        dispatcher.onServicePreSuperOnDestroy()
    }

    // LifecycleOwner 인터페이스를 구현하기 위해 lifecycle 객체를 외부에 제공합니다.
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
}