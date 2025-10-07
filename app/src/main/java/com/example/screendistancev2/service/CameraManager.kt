package com.example.screendistancev2.service

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * CameraX와 ML Kit를 사용하여 전면 카메라로 얼굴을 감지하고 거리를 추정하는 클래스.
 * 서비스의 LifecycleOwner를 받아와 서비스의 수명주기에 맞춰 카메라를 안전하게 관리합니다.
 */
class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner // 서비스의 수명주기를 따르기 위해 필요
) {

    private var cameraProvider: ProcessCameraProvider? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    // ML Kit 얼굴 인식기 설정: 속도 우선 모드로 설정하여 배터리 효율을 높임
    private val faceDetectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .build()
    private val faceDetector = FaceDetection.getClient(faceDetectorOptions)

    @SuppressLint("MissingPermission") // 권한 체크는 MainActivity와 Service에서 이미 수행함
    fun startFaceDetection(onDistanceResult: (Float) -> Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            // 이미지 분석용 UseCase 설정
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // 최신 프레임만 분석
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, FaceAnalyzer(onDistanceResult))
                }

            // 전면 카메라 선택
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // 기존에 바인딩된 것이 있다면 모두 해제
                cameraProvider?.unbindAll()

                // 수명주기에 카메라와 이미지 분석 UseCase를 바인딩
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageAnalyzer
                )
            } catch (exc: Exception) {
                // 카메라 바인딩 실패 시 로그 (오류 처리)
            }

        }, ContextCompat.getMainExecutor(context))
    }

    fun stopFaceDetection() {
        // 모든 UseCase 바인딩을 해제하여 카메라를 안전하게 종료
        cameraProvider?.unbindAll()
    }

    /**
     * 각 카메라 프레임을 분석하여 얼굴을 찾고 거리를 계산하는 클래스
     */
    private inner class FaceAnalyzer(private val onDistanceResult: (Float) -> Unit) : ImageAnalysis.Analyzer {
        @SuppressLint("UnsafeOptInUsageError")
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                faceDetector.process(image)
                    .addOnSuccessListener { faces ->
                        if (faces.isNotEmpty()) {
                            // 감지된 첫 번째 얼굴 정보를 사용
                            val face = faces[0]
                            val boundingBox = face.boundingBox
                            // 얼굴 경계 상자의 높이를 이용해 거리 추정
                            val distance = calculateDistance(boundingBox.height())
                            onDistanceResult(distance)
                        } else {
                            onDistanceResult(-1f) // 얼굴이 감지되지 않음
                        }
                    }
                    .addOnFailureListener {
                        onDistanceResult(-1f) // 분석 오류
                    }
                    .addOnCompleteListener {
                        // 매우 중요: 작업이 끝나면 반드시 imageProxy를 닫아 다음 프레임을 받을 수 있도록 해야 함
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
    }

    /**
     * 얼굴의 픽셀 높이를 이용해 거리를 추정하는 함수.
     * 정확한 cm 변환은 카메라 센서 정보(초점 거리 등)가 필요하며 복잡하지만,
     * 여기서는 경험적인 상수를 사용하여 근사치를 계산합니다.
     * (얼굴이 화면에 크게 보일수록 = 픽셀 높이가 클수록 = 거리가 가깝다)
     */
    private fun calculateDistance(faceHeightInPixels: Int): Float {
        if (faceHeightInPixels <= 0) return -1f

        // 이 값은 여러 테스트를 통해 얻어진 경험적 상수(Magic Number)입니다.
        // 스마트폰 기종마다 최적의 값이 다를 수 있으나, 평균적인 값으로 설정합니다.
        // (상수 / 픽셀 높이)가 거리에 반비례하는 관계를 이용합니다.
        val constantFactor = 4000
        return constantFactor / faceHeightInPixels.toFloat()
    }
}