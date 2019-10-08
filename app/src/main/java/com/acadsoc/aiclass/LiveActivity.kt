package com.acadsoc.aiclass

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.*
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Size
import android.view.*
import android.webkit.*
import android.widget.ImageView
import android.widget.RelativeLayout
import com.acadsoc.aiclass.data.bean.*
import com.acadsoc.aiclass.utils.RelayoutUtil
import com.acadsoc.aiclass.widget.CompareSizesByArea
import com.agora.smallclass.core.ext.*
import com.google.gson.Gson
import com.gyf.barlibrary.ImmersionBar
import com.iflytek.cloud.*
import com.shuyu.gsyvideoplayer.GSYVideoManager
import com.shuyu.gsyvideoplayer.player.IjkPlayerManager
import com.shuyu.gsyvideoplayer.player.PlayerFactory
import com.shuyu.gsyvideoplayer.utils.GSYVideoType
import com.tbruyelle.rxpermissions2.RxPermissions
import com.ypz.bangscreentools.BangScreenTools
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_live.*
import kotlinx.android.synthetic.main.include_webview_error.*
import org.jetbrains.anko.toast
import org.w3c.dom.Element
import org.xml.sax.InputSource
import ren.yale.android.cachewebviewlib.WebViewCacheInterceptorInst
import timber.log.Timber
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import java.io.StringReader
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

class LiveActivity : AppCompatActivity() {

    // 课件地址
    private var mCourseWareUrl = "http://106.75.80.80:3000/acadsoc/project2/?browser=android"
    // 视频地址前缀
    private var mVideoUrl = "http://106.75.80.80:3000/acadsoc/video/"

    private val mRxPermissions = RxPermissions(this)

    private val mCompositeSubscription = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rUtil = RelayoutUtil(this@LiveActivity, 750, 1334)
        setContentView(rUtil.relayoutViewHierarchy(this, R.layout.activity_live, false))
        // 保持屏幕常亮
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setStatusBar()
        checkPermissions()

        mCurrentParams = mRlWebView.layoutParams
        fullScreen()
    }

    private fun initView() {
        mRlWebView.setRoundRect()
        mRlTeacher.setRoundRect()
        mRlStudent.setRoundRect()

        initVideoPlayer()
        initCamera()
    }

    private fun initVideoPlayer() {
        //ijk内核，默认模式
        PlayerFactory.setPlayManager(IjkPlayerManager::class.java)
        //切换渲染模式
        GSYVideoType.setShowType(GSYVideoType.SCREEN_MATCH_FULL)
        //ijk关闭log
        IjkPlayerManager.setLogLevel(IjkMediaPlayer.IJK_LOG_SILENT)
        //增加封面
        val imageView = ImageView(this)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        imageView.setImageResource(R.mipmap.uclass_default_loading)
        mVideoPlayer.thumbImageView = imageView
        //增加title
        mVideoPlayer.titleTextView.visibility = View.GONE
        //显示暂停页面
        mVideoPlayer.isShowPauseCover = false
        //设置返回键
        mVideoPlayer.backButton.visibility = View.GONE
        mVideoPlayer.fullscreenButton.visibility = View.GONE
        //是否可以滑动调整
        mVideoPlayer.setIsTouchWiget(false)
    }

    override fun onPause() {
        super.onPause()
        mVideoPlayer.onVideoPause()
        stopBackgroundThread()
    }

    override fun onResume() {
        super.onResume()
        mVideoPlayer.onVideoResume()
        startBackgroundThread()
    }

    private fun setStatusBar() {
        BangScreenTools.getBangScreenTools().blockDisplayCutout(window)
        ImmersionBar.hideStatusBar(window)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.navigationBarColor = Color.BLACK
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {

        // 修改WebView未加载出的背景
        mWebView.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
        mWebView.setBackgroundResource(R.mipmap.default_webview_bg)

        mWebView.settings.javaScriptEnabled = true
        mWebView.settings.cacheMode = WebSettings.LOAD_DEFAULT

        // 解决对某些标签的不支持出现白屏
        mWebView.settings.domStorageEnabled = true
        mWebView.addJavascriptInterface(JavaScriptInterface(), "androids")
        mWebView.webViewClient = object : WebViewClient() {

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                WebViewCacheInterceptorInst.getInstance().loadUrl(mWebView, request?.url.toString())
                return true
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                WebViewCacheInterceptorInst.getInstance().loadUrl(mWebView, url)
                return true
            }

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                return WebViewCacheInterceptorInst.getInstance().interceptRequest(request)
            }

            override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
                return WebViewCacheInterceptorInst.getInstance().interceptRequest(url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            }
        }
        mWebView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                return true
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    if (title!!.contains("404") || title.contains("500") || title.contains("Error")) {

                    }
                }
            }
        }

        WebViewCacheInterceptorInst.getInstance().loadUrl(mWebView, mCourseWareUrl)

        // WebView 加载失败，重新加载
        mWebViewErrorRetry.onClick {
            mWebViewErrorRetry.visibility = View.GONE
            WebViewCacheInterceptorInst.getInstance().loadUrl(mWebView, mCourseWareUrl)
        }
    }


    private fun checkPermissions() {
        val subscribe = mRxPermissions
            .request(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
            .subscribe { granted ->
                granted.yes {
                    initView()
                    initWebView()
                }.otherwise {
                    toast("无法获取权限会影响程序部分功能")
                    finish()
                }
            }
        mCompositeSubscription.add(subscribe)
    }

    // 当前停止录音，发送分数ID
    private var mCurrentStopAudioDetectId = -1

    /**
     * Android 与 JS 通信
     */
    inner class JavaScriptInterface {

        @JavascriptInterface
        fun jsAndroid(methodName: String, params: String) {
            Timber.i("JavaScript调用Android方法:$methodName - $params")
            when (methodName) {
                // 开始录音
                "startAudioDetect" -> {
                    val jsWordParamsBean = Gson().fromJson<JsWordParamsBean>(params)
                    justResponseToWebView(params)
                    // 开始录音
                    speechEvaluationStart(jsWordParamsBean.word)
                }
                // 获取音量
                "getVolume" -> {
                    val jsParamsBean = Gson().fromJson<JsParamsBean>(params)
                    val jsVolumeBean = JsVolumeBean(jsParamsBean.id, 88) // 音量范围0～99
                    responseDataToWebView(jsVolumeBean)
                }
                // 停止录音
                "stopAudioDetect" -> {
                    val jsParamsBean = Gson().fromJson<JsParamsBean>(params)
                    mCurrentStopAudioDetectId = jsParamsBean.id
                    // 结束录音
                    if (mSpeechEvaluator!!.isEvaluating) {
                        Timber.i("评测已停止，等待结果中...")
                        mSpeechEvaluator!!.stopEvaluating()
                    }
                }
                "playVideo" -> {
                    justResponseToWebView(params)
                    runOnUiThread {
                        // 参数：{"index":1}，视频编号，任何返回认为成功
                        val jsIndexBean = Gson().fromJson<JsIndexBean>(params)
                        mVideoPlayer.setUp(
                            mVideoUrl + "${jsIndexBean.index}.mp4",
                            true,
                            ""
                        )
                        Timber.i("视频地址：" + mVideoUrl + "${jsIndexBean.index}.mp4")
                        mVideoPlayer.startPlayLogic()
                    }
                }
                "fullScreen" -> {
                    runOnUiThread {
                        GSYVideoManager.onPause()
                    }
                    justResponseToWebView(params)
                    fullScreen()
                }
                "partScreen" -> {
                    justResponseToWebView(params)
                    partScreen()
                }
            }
        }
    }

    /**
     * 反馈数据给WebView
     */
    private fun responseDataToWebView(bean: Any) {
        runOnUiThread {
            mWebView.evaluateJavascript(
                "window.cc.find('Canvas').getComponent('NJ').NativeResponse2JsCallEx('${Gson().toJson(
                    bean
                )}')",
                null
            )
        }
    }

    /**
     * 只反馈id给WebView
     */
    private fun justResponseToWebView(params: String) {
        val jsParamsBean = Gson().fromJson<JsParamsBean>(params)
        val jsResponseIdBean = JsResponseIdBean(jsParamsBean.id)
        runOnUiThread {
            mWebView.evaluateJavascript(
                "window.cc.find('Canvas').getComponent('NJ').NativeResponse2JsCallEx('${Gson().toJson(
                    jsResponseIdBean
                )}')",
                null
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!mCompositeSubscription.isDisposed) {
            mCompositeSubscription.clear()
        }
        mVideoPlayer.setGSYVideoProgressListener(null)
        GSYVideoManager.instance().optionModelList = mutableListOf()
        GSYVideoManager.releaseAllVideos()
    }

    // =================================================================================================================
    // ==========================================相机代码===============================================================


    /**
     * A [Semaphore] to prevent the app from exiting before closing the camera.
     */
    private val cameraOpenCloseLock = Semaphore(1)

    /**
     * ID of the current [CameraDevice].
     */
    private lateinit var cameraId: String

    /**
     * A reference to the opened [CameraDevice].
     */
    private var mCameraDevice: CameraDevice? = null

    /**
     * The [android.util.Size] of camera preview.
     */
    private lateinit var previewSize: Size

    /**
     * [CaptureRequest.Builder] for the camera preview
     */
    private lateinit var previewRequestBuilder: CaptureRequest.Builder

    /**
     * An [ImageReader] that handles still image capture.
     */
    private var imageReader: ImageReader? = null

    /**
     * A [CameraCaptureSession] for camera preview.
     */
    private var captureSession: CameraCaptureSession? = null

    /**
     * Whether the current camera device supports Flash or not.
     */
    private var flashSupported = false

    /**
     * [CaptureRequest] generated by [.previewRequestBuilder]
     */
    private lateinit var previewRequest: CaptureRequest

    /**
     * A [Handler] for running tasks in the background.
     */
    private var backgroundHandler: Handler? = null

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private var backgroundThread: HandlerThread? = null

    /**
     * Orientation of the camera sensor
     */
    private var sensorOrientation = 0

    /**
     * [TextureView.SurfaceTextureListener] handles several lifecycle events on a
     * [TextureView].
     */
    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            openCamera(width, height)
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture) = true

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit

    }

    private fun initCamera() {
        Timber.i("Method initCamera")
        if (mTextureView.isAvailable) {
            openCamera(mTextureView.width, mTextureView.height)
        } else {
            mTextureView.surfaceTextureListener = surfaceTextureListener
        }
    }

    /**
     * [CameraDevice.StateCallback] is called when [CameraDevice] changes its state.
     */
    private val stateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            mCameraDevice = cameraDevice
            createCameraPreviewSession()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice.close()
            mCameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            onDisconnected(cameraDevice)
        }

    }

    /**
     * Creates a new [CameraCaptureSession] for camera preview.
     */
    private fun createCameraPreviewSession() {
        Timber.i("Method createCameraPreviewSession")
        try {
            val texture = mTextureView.surfaceTexture

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(previewSize.width, previewSize.height)

            // This is the output Surface we need to start preview.
            val surface = Surface(texture)

            // We set up a CaptureRequest.Builder with the output Surface.
            previewRequestBuilder = mCameraDevice!!.createCaptureRequest(
                CameraDevice.TEMPLATE_PREVIEW
            )
            previewRequestBuilder.addTarget(surface)

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice?.createCaptureSession(
                Arrays.asList(surface, imageReader?.surface),
                object : CameraCaptureSession.StateCallback() {

                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        // The camera is already closed
                        if (mCameraDevice == null) return

                        // When the session is ready, we start displaying the preview.
                        captureSession = cameraCaptureSession
                        try {
                            // Auto focus should be continuous for camera preview.
                            previewRequestBuilder.set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                            )
                            // Flash is automatically enabled when necessary.
                            setAutoFlash(previewRequestBuilder)

                            // Finally, we start displaying the camera preview.
                            previewRequest = previewRequestBuilder.build()
                            captureSession?.setRepeatingRequest(
                                previewRequest,
                                captureCallback, backgroundHandler
                            )
                        } catch (e: CameraAccessException) {
                            Timber.e(e.toString())
                        }

                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        toast("Failed")
                    }
                }, null
            )
        } catch (e: CameraAccessException) {
            Timber.e(e.toString())
        }

    }

    /**
     * A [CameraCaptureSession.CaptureCallback] that handles events related to JPEG capture.
     */
    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {

    }

    private fun setAutoFlash(requestBuilder: CaptureRequest.Builder) {
        Timber.i("Method setAutoFlash")
        if (flashSupported) {
            requestBuilder.set(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
            )
        }
    }

    /**
     * Opens the camera specified by [Camera2BasicFragment.cameraId].
     */
    @SuppressLint("MissingPermission")
    private fun openCamera(width: Int, height: Int) {
        Timber.i("Method openCamera")
        setUpCameraOutputs(width, height)
        configureTransform(width, height)
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            // Wait for camera to open - 2.5 seconds is sufficient
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            manager.openCamera(cameraId, stateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Timber.e(e.toString())
        } catch (e: InterruptedException) {
            Timber.e("Interrupted while trying to lock camera opening.")
        }

    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    private fun setUpCameraOutputs(width: Int, height: Int) {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (cameraId in manager.cameraIdList) {
                val cameraCharacteristics = manager.getCameraCharacteristics(cameraId)
                val facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)

                // 使用前置摄像头
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    val characteristics = manager.getCameraCharacteristics(cameraId)

                    val map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                    ) ?: continue

                    // For still image captures, we use the largest available size.
                    val largest = Collections.max(
                        Arrays.asList(*map.getOutputSizes(ImageFormat.JPEG)),
                        CompareSizesByArea()
                    )
                    imageReader = ImageReader.newInstance(
                        largest.width, largest.height,
                        ImageFormat.JPEG, /*maxImages*/ 2
                    ).apply {
                        // setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)
                    }

                    // Find out if we need to swap dimension to get the preview size relative to sensor
                    // coordinate.
                    val displayRotation = windowManager.defaultDisplay.rotation

                    sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
                    val swappedDimensions = areDimensionsSwapped(displayRotation)

                    val displaySize = Point()
                    windowManager.defaultDisplay.getSize(displaySize)
                    val rotatedPreviewWidth = if (swappedDimensions) height else width
                    val rotatedPreviewHeight = if (swappedDimensions) width else height
                    var maxPreviewWidth = if (swappedDimensions) displaySize.y else displaySize.x
                    var maxPreviewHeight = if (swappedDimensions) displaySize.x else displaySize.y

                    if (maxPreviewWidth > MAX_PREVIEW_WIDTH) maxPreviewWidth = MAX_PREVIEW_WIDTH
                    if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) maxPreviewHeight = MAX_PREVIEW_HEIGHT

                    // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                    // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                    // garbage capture data.
                    previewSize = chooseOptimalSize(
                        map.getOutputSizes(SurfaceTexture::class.java),
                        rotatedPreviewWidth, rotatedPreviewHeight,
                        maxPreviewWidth, maxPreviewHeight,
                        largest
                    )

                    // We fit the aspect ratio of TextureView to the size of preview we picked.
                    if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        mTextureView.setAspectRatio(previewSize.width, previewSize.height)
                    } else {
                        mTextureView.setAspectRatio(previewSize.height, previewSize.width)
                    }

                    // Check if the flash is supported.
                    flashSupported =
                        characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true

                    this.cameraId = cameraId

                    // We've found a viable camera and finished setting up member variables,
                    // so we don't need to iterate through other available cameras.
                    return
                }
            }
        } catch (e: CameraAccessException) {
            Timber.e(e.toString())
        } catch (e: NullPointerException) {
            Timber.e(e.toString())
        }

    }

    /**
     * Configures the necessary [android.graphics.Matrix] transformation to `textureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `textureView` is fixed.
     *
     * @param viewWidth  The width of `textureView`
     * @param viewHeight The height of `textureView`
     */
    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        val rotation = windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, previewSize.height.toFloat(), previewSize.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            val scale = Math.max(
                viewHeight.toFloat() / previewSize.height,
                viewWidth.toFloat() / previewSize.width
            )
            with(matrix) {
                setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
                postScale(scale, scale, centerX, centerY)
                postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
            }
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        mTextureView.setTransform(matrix)
    }

    /**
     * Starts a background thread and its [Handler].
     */
    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread?.looper)
    }

    /**
     * Stops the background thread and its [Handler].
     */
    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Timber.e(e.toString())
        }
    }

    /**
     * Determines if the dimensions are swapped given the phone's current rotation.
     *
     * @param displayRotation The current rotation of the display
     *
     * @return true if the dimensions are swapped, false otherwise.
     */
    private fun areDimensionsSwapped(displayRotation: Int): Boolean {
        var swappedDimensions = false
        when (displayRotation) {
            Surface.ROTATION_0, Surface.ROTATION_180 -> {
                if (sensorOrientation == 90 || sensorOrientation == 270) {
                    swappedDimensions = true
                }
            }
            Surface.ROTATION_90, Surface.ROTATION_270 -> {
                if (sensorOrientation == 0 || sensorOrientation == 180) {
                    swappedDimensions = true
                }
            }
            else -> {
                Timber.e("Display rotation is invalid: $displayRotation")
            }
        }
        return swappedDimensions
    }

    companion object {
        /**
         * Max preview width that is guaranteed by Camera2 API
         */
        private val MAX_PREVIEW_WIDTH = 1920

        /**
         * Max preview height that is guaranteed by Camera2 API
         */
        private val MAX_PREVIEW_HEIGHT = 1080

        /**
         * Given `choices` of `Size`s supported by a camera, choose the smallest one that
         * is at least as large as the respective texture view size, and that is at most as large as
         * the respective max size, and whose aspect ratio matches with the specified value. If such
         * size doesn't exist, choose the largest one that is at most as large as the respective max
         * size, and whose aspect ratio matches with the specified value.
         *
         * @param choices           The list of sizes that the camera supports for the intended
         *                          output class
         * @param textureViewWidth  The width of the texture view relative to sensor coordinate
         * @param textureViewHeight The height of the texture view relative to sensor coordinate
         * @param maxWidth          The maximum width that can be chosen
         * @param maxHeight         The maximum height that can be chosen
         * @param aspectRatio       The aspect ratio
         * @return The optimal `Size`, or an arbitrary one if none were big enough
         */
        @JvmStatic
        private fun chooseOptimalSize(
            choices: Array<Size>,
            textureViewWidth: Int,
            textureViewHeight: Int,
            maxWidth: Int,
            maxHeight: Int,
            aspectRatio: Size
        ): Size {

            // Collect the supported resolutions that are at least as big as the preview Surface
            val bigEnough = ArrayList<Size>()
            // Collect the supported resolutions that are smaller than the preview Surface
            val notBigEnough = ArrayList<Size>()
            val w = aspectRatio.width
            val h = aspectRatio.height
            for (option in choices) {
                if (option.width <= maxWidth && option.height <= maxHeight &&
                    option.height == option.width * h / w
                ) {
                    if (option.width >= textureViewWidth && option.height >= textureViewHeight) {
                        bigEnough.add(option)
                    } else {
                        notBigEnough.add(option)
                    }
                }
            }

            // Pick the smallest of those big enough. If there is no one big enough, pick the
            // largest of those not big enough.
            if (bigEnough.size > 0) {
                return Collections.min(bigEnough, CompareSizesByArea())
            } else if (notBigEnough.size > 0) {
                return Collections.max(notBigEnough, CompareSizesByArea())
            } else {
                Timber.e("Couldn't find any suitable preview size")
                return choices[0]
            }
        }
    }
    // =================================================================================================================

    //==============================================讯飞评测=============================================================

    private var mSpeechEvaluator: SpeechEvaluator? = null

    /**
     * 讯飞评测开始
     */
    fun speechEvaluationStart(word: String) {
        var evaText = "[word]\n$word"
        Timber.i("语音评测单词：$word")
        mSpeechEvaluator = SpeechEvaluator.createEvaluator(this@LiveActivity, null)
        if (mSpeechEvaluator == null) {
            toast("................")
            return
        }
        // 设置评测语种
        mSpeechEvaluator!!.setParameter(SpeechConstant.LANGUAGE, "en_us")
        // 设置评测题型
        mSpeechEvaluator!!.setParameter(SpeechConstant.ISE_CATEGORY, "read_word")
        // 设置结果等级，不同等级对应不同的详细程度
        mSpeechEvaluator!!.setParameter(SpeechConstant.RESULT_LEVEL, "plain")
        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mSpeechEvaluator!!.setParameter(SpeechConstant.VAD_BOS, "5000")
        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mSpeechEvaluator!!.setParameter(SpeechConstant.VAD_EOS, "1800")
        // 语音输入超时时间，即用户最多可以连续说多长时间；默认-1（无超时）
        mSpeechEvaluator!!.setParameter(SpeechConstant.KEY_SPEECH_TIMEOUT, "-1")
        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        mSpeechEvaluator!!.setParameter(SpeechConstant.AUDIO_FORMAT, "wav")
        mSpeechEvaluator!!.setParameter(
            SpeechConstant.ISE_AUDIO_PATH,
            Environment.getExternalStorageDirectory().absolutePath + "/msc/ise.wav"
        )
        // evaText 为试题内容
        mSpeechEvaluator!!.startEvaluating(evaText, null, mEvaluatorListener)
    }

    /**
     * 讯飞评测监听接口
     */
    private val mEvaluatorListener = object : EvaluatorListener {

        override fun onResult(result: EvaluatorResult, isLast: Boolean) {
            Timber.i("evaluator result :$isLast")

            if (isLast) {
                val builder = StringBuilder()
                builder.append(result.resultString)
                Timber.i("评测结束，sdk返回的结果:$builder")
                var score = getScore(builder.toString())
                var scoreH = (score * 20).toInt()
                Timber.i("得到分数:$scoreH")
                val volumeBean = JsScoreBean(mCurrentStopAudioDetectId, scoreH) // 分数范围0～99
                responseDataToWebView(volumeBean)
            }
        }

        override fun onError(error: SpeechError?) {
            Timber.i("evaluator over")
        }

        override fun onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            Timber.i("evaluator begin")
        }

        override fun onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            Timber.i("evaluator stoped")
        }

        override fun onVolumeChanged(volume: Int, data: ByteArray) {
            Timber.i("返回音频数据：" + data.size)
        }

        override fun onEvent(eventType: Int, arg1: Int, arg2: Int, obj: Bundle) {

        }

    }

    /**
     * 解析讯飞返回的结果
     */
    fun getScore(result: String): Double {
        //1.创建一个DocumentBuilderFactory对象
        val builderFactory = DocumentBuilderFactory.newInstance()
        try {
            //2.创建一个DocumentBuilder
            val documentBuilder = builderFactory.newDocumentBuilder()
            //通过DocumentBuilder对象的parse方法加载books.xml到当前项目下
            val parse = documentBuilder.parse(InputSource(StringReader(result)))
            //获取节点(book)的集合
            val totalScoreList = parse.getElementsByTagName("total_score")
            val item: Element = totalScoreList.item(0) as Element
            var value = item.getAttribute("value")
            return value.toDouble()
        } catch (e: Exception) {
            e.printStackTrace()
            return 0.0
        }
    }

    // =================================================================================================================
    // 切换全屏半屏

    private var mCurrentParams: ViewGroup.LayoutParams? = null
    /**
     * 全屏
     */
    private fun fullScreen() {
        runOnUiThread {
            var layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP)
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            mRlWebView.layoutParams = layoutParams
        }
    }

    /**
     * 半屏
     */
    private fun partScreen() {
        runOnUiThread {
            mRlWebView.layoutParams = mCurrentParams
        }
    }

}
