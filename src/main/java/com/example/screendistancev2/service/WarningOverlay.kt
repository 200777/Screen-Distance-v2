package com.example.screendistancev2.service

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import com.example.screendistancev2.databinding.OverlayWarningBinding
import java.text.DecimalFormat

/**
 * 다른 모든 앱 위에 경고 화면을 띄우는 클래스.
 * 'SYSTEM_ALERT_WINDOW' 권한을 사용하여 WindowManager에 직접 뷰를 추가합니다.
 */
class WarningOverlay(private val context: Context) {

    // 시스템의 WindowManager 서비스에 접근합니다. 화면에 뷰를 추가하거나 제거하는 역할을 합니다.
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    // ViewBinding을 사용하여 'overlay_warning.xml' 레이아웃의 뷰에 안전하게 접근합니다.
    private val binding: OverlayWarningBinding

    // 경고창이 현재 화면에 떠 있는지 여부를 추적하는 상태 변수. 중복 추가/제거를 방지합니다.
    private var isShowing = false

    // 거리를 소수점 한 자리까지만 깔끔하게 표시하기 위한 포맷터
    private val decimalFormat = DecimalFormat("#.#")

    init {
        // XML 레이아웃 파일을 실제 View 객체로 변환(inflate)합니다.
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        binding = OverlayWarningBinding.inflate(inflater)
    }

    // 경고창의 속성을 정의하는 매개변수 객체.
    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,     // 너비를 화면 전체로
        WindowManager.LayoutParams.MATCH_PARENT,     // 높이를 화면 전체로
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, // 다른 앱 위에 띄울 수 있는 타입
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, // 이 창이 포커스를 뺏지 않도록 설정 (터치 방지)
        PixelFormat.TRANSLUCENT                      // 반투명 배경을 지원하도록 설정
    ).apply {
        gravity = Gravity.CENTER // 화면 정중앙에 위치하도록 설정
    }

    /**
     * 경고창을 화면에 표시합니다.
     * @param distance 측정된 거리를 cm 단위로 받아서 화면에 표시합니다.
     */
    fun show(distance: Float) {
        // 이미 창이 떠있지 않은 경우에만 WindowManager에 뷰를 추가합니다.
        if (!isShowing) {
            isShowing = true
            windowManager.addView(binding.root, params)
        }
        // 거리 텍스트를 업데이트합니다.
        binding.tvDistance.text = "${decimalFormat.format(distance)} cm"
    }

    /**
     * 경고창을 화면에서 제거합니다.
     */
    fun hide() {
        // 창이 현재 떠 있는 경우에만 뷰를 제거합니다.
        if (isShowing) {
            isShowing = false
            windowManager.removeView(binding.root)
        }
    }
}