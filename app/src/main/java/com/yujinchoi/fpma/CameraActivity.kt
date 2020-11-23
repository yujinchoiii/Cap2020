package com.yujinchoi.fpma

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.hardware.Camera
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import com.yujinchoi.fpma.camera.CameraController
import kotlinx.android.synthetic.main.activity_camera.*
import java.io.File
import kotlin.math.sqrt

class CameraActivity : AppCompatActivity() {
    lateinit var cameraController : CameraController
    private var previewHolder: SurfaceHolder? = null
    private var gridHolder: SurfaceHolder? = null
    private var camera: Camera? = null
    private var inPreview = false
    private var cameraConfigured = false
    private var CAM_ID = 0
    private var mDist = 0f
    private val mZoomSpeed = 2f // 1 is too slow
    private var gridflag = false
    private var firstFileToScan: String? = null // 0th file name, for passing
    private var count = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        initUI()
    }

    private fun initUI(){
        // Hide status bar
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        grid.setBackgroundColor(Color.TRANSPARENT)
        gridHolder = grid.holder
        gridHolder?.addCallback(surfaceCallback)
        gridHolder?.setFormat(PixelFormat.TRANSPARENT)
        previewHolder = focus_preview.holder
        previewHolder?.addCallback(surfaceCallback)
        previewHolder?.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)

        // pinch zoom & focusing
        full_area.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(view: View, event: MotionEvent): Boolean {
                view.performClick()
                if (camera == null) return false

                // Get the pointer ID
                val params = camera!!.parameters
                val action = event.action
                if (event.pointerCount > 1) {
                    // handle multi-touch events
                    if (action == MotionEvent.ACTION_POINTER_DOWN) {
                        mDist = getFingerSpacing(event)
                    } else if (action == MotionEvent.ACTION_MOVE && params.isZoomSupported) {
                        camera!!.cancelAutoFocus()
                        handleZoom(event, params)
                    }
                }
                if (event.pointerCount == 1 && action == MotionEvent.ACTION_DOWN) {
                    if(!gridflag){
                        DrawGrid(gridHolder)
                        gridflag = true
                    }
                    else{
                        ClearGrid(gridHolder)
                        gridflag = false
                    }
                }
                return true
            }
            private fun ClearGrid(holder: SurfaceHolder?) {
                var canvas = holder?.lockCanvas()
                canvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                holder?.unlockCanvasAndPost(canvas)
            }

            private fun DrawGrid(holder: SurfaceHolder?) {
                var canvas = holder?.lockCanvas()
                //border's propertiesE
                val paint = Paint().apply {
                    color = Color.rgb(255, 255, 255)
                    // Smooths out edges of what is drawn without affecting shape.
                    isAntiAlias = true
                    // Dithering affects how colors with higher-precision than the device are down-sampled.
                    isDither = true
                    style = Paint.Style.FILL // default: FILL
                    strokeJoin = Paint.Join.ROUND // default: MITER
                    strokeCap = Paint.Cap.BUTT // default: BUTT
                    strokeWidth = 3F // default: Hairline-width (really thin)
                }
                //Log.d("PREVIEW RATE", previewScale.toString() + "")
                val height = grid!!.height
                val width = grid!!.width
                val preview_x = 0F
                val preview_y = 0F
                // Horizontal Line
                canvas?.drawLine(preview_x, (height/2).toFloat(), width.toFloat(), (height/2).toFloat(), paint)
                canvas?.drawLine(preview_x, (height/4).toFloat(), width.toFloat(), (height/4).toFloat(), paint)
                canvas?.drawLine(preview_x, (height*3/4).toFloat(), width.toFloat(), (height*3/4).toFloat(), paint)
                // Vertical Line
                canvas?.drawLine((width/2).toFloat(), preview_y, (width/2).toFloat(), height.toFloat(), paint)
                canvas?.drawLine((width/4).toFloat(), preview_y, (width/4).toFloat(), height.toFloat(), paint)
                canvas?.drawLine((width*3/4).toFloat(), preview_y, (width*3/4).toFloat(), height.toFloat(), paint)
                canvas?.drawLine((width*3/8).toFloat(), preview_y, (width*3/8).toFloat(), height.toFloat(), paint)
                canvas?.drawLine((width*5/8).toFloat(), preview_y, (width*5/8).toFloat(), height.toFloat(), paint)

                holder?.unlockCanvasAndPost(canvas)
            }

            private fun getFingerSpacing(event: MotionEvent): Float {
                val x = event.getX(0) - event.getX(1)
                val y = event.getY(0) - event.getY(1)
                return sqrt(x * x + y * y.toDouble()).toFloat()
            }

            private fun handleZoom(event: MotionEvent, params: Camera.Parameters) {
                val maxZoom = params.maxZoom
                var zoom = params.zoom
                val newDist = getFingerSpacing(event)
                if (newDist > mDist) {
                    //zoom in
                    if (zoom < maxZoom) zoom += mZoomSpeed.toInt()
                } else if (newDist < mDist) {
                    //zoom out
                    if (zoom > 0) zoom -= mZoomSpeed.toInt()
                }
                mDist = newDist
                params.zoom = zoom
                if (zoom in 1..maxZoom) camera!!.parameters = params
            }
        })

        confirm_button.setOnClickListener{ goToNextActivity() }
        zoom_in_button.setOnClickListener{ zoomIn() }
        zoom_out_button.setOnClickListener{ zoomOut() }
        back_button.setOnClickListener{ finish() }
    }

    public override fun onResume() {
        super.onResume()

        // front facing camera
        initCamera()
//        for (i in 0 until Camera.getNumberOfCameras()) {
//            val info = Camera.CameraInfo()
//            Camera.getCameraInfo(i, info)
//            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
//                CAM_ID = i
//                break
//            }
//        }
//        cameraController = CameraController(applicationContext)
//        cameraController.openCamera()
    }

    private fun initCamera(){
        // initialize camera
     //   val manager = applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraController = CameraController(applicationContext)
        cameraController.openCamera()?.let {
            camera = Camera.open(it)
            setCameraDisplayOrientation(camera)
            startPreview()
        }
        Log.d("picture # to be taken", "init camera")
    }

    public override fun onPause() {
        if (inPreview) {
            camera!!.stopPreview()
        }
        camera!!.release()
        camera = null
        inPreview = false
        super.onPause()
    }

    private fun setCameraDisplayOrientation(mCamera: Camera?) {
        if (mCamera == null) return
        val info = Camera.CameraInfo()
        Camera.getCameraInfo(CAM_ID, info)//assertion 주
        val winManager = applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val rotation = winManager.defaultDisplay.rotation
        var degrees = 0
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }
        var result: Int
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
            //Log.d("facing", "FACING FRONT")
            result = (info.orientation + degrees) % 360
            result = (360 - result) % 360 // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360
        }
        mCamera.setDisplayOrientation(result)
    }

    private fun getBestPreviewSize(width: Int, height: Int,
                                   parameters: Camera.Parameters): Camera.Size? {
        var result: Camera.Size? = null
        for (size in parameters.supportedPreviewSizes) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size
                } else {
                    val resultArea = result.width * result.height
                    val newArea = size.width * size.height
                    if (newArea > resultArea) {
                        result = size
                    }
                }
            }
        }
        return result
    }

    private fun initPreview(width: Int, height: Int) {
        if (camera != null && previewHolder!!.surface != null) {
            try {
                camera!!.setPreviewDisplay(previewHolder)
            } catch (t: Throwable) {
                Log.e("PreviewDemo-surfaceCallback",
                    "Exception in setPreviewDisplay()", t)
                Toast
                    .makeText(applicationContext, t.message, Toast.LENGTH_LONG)
                    .show()
            }
            if (!cameraConfigured) {
                val parameters = camera!!.parameters
                val size = getBestPreviewSize(width, height,
                    parameters)
                if (size != null) {
                    parameters.setPreviewSize(size.width, size.height)
                    camera!!.parameters = parameters
                    cameraConfigured = true
                }
            }
        }
    }

    private fun startPreview() {
        if (cameraConfigured && camera != null) {
            camera!!.startPreview()
            inPreview = true
        }
    }

    private var surfaceCallback: SurfaceHolder.Callback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            // no-op -- wait until surfaceChanged()
        }

        override fun surfaceChanged(holder: SurfaceHolder,
                                    format: Int, width: Int,
                                    height: Int) {

            // set preview size
            setPreviewSize(focus_preview)
            initPreview(focus_preview.width, focus_preview.height)
            startPreview()
        }

        fun setPreviewSize(preview: SurfaceView?) {
            val previewScale = 3.0 / 4.0 // default ratio 3:4
            //Log.d("PREVIEW RATE", previewScale.toString() + "")
            val height = preview!!.height
            val width = (height * previewScale).toInt()
            //Log.d("height, previewScale, width", preview.layoutParams.height.toString() + ", " + height + ", " + previewScale + ", " + width)
            val lp = preview.layoutParams
            lp.width = width
            lp.height = height
            preview.layoutParams = lp
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
        }
    }

    private fun goToNextActivity() {
        val exposureTime_var = 1.toLong()
        val file: File = cameraController.takePicture(count, exposureTime_var)!!
        val fName = file.absolutePath

        // select 0th file for scanning
        val endString = fName.substring(fName.length - 8)
        val compString = "_000.jpg"
        if (endString == compString) firstFileToScan = fName // scan only 0th file
        Log.d("file path", file.absolutePath)
        count++
//        val intent = Intent(applicationContext, CaptureOptionActivity::class.java)
//        startActivity(intent)
//        overridePendingTransition(0, R.anim.fade_out)
//        finish()
    }

    private fun zoomIn() {
        // Get the pointer ID
        val params = camera!!.parameters
        val zoomStep = 10
        val maxZoom = params.maxZoom
        var zoom = params.zoom
        zoom = Math.min(zoom + zoomStep, maxZoom)
        params.zoom = zoom
        camera!!.parameters = params
    }

    private fun zoomOut() {
        // Get the pointer ID
        val params = camera!!.parameters
        val zoomStep = 10
        var zoom = params.zoom
        zoom = Math.max(zoom - zoomStep, 0)
        params.zoom = zoom
        camera!!.parameters = params
    }
}