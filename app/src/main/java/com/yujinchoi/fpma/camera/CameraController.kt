package com.yujinchoi.fpma.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.hardware.camera2.params.ColorSpaceTransform
import android.hardware.camera2.params.RggbChannelVector
import android.hardware.camera2.params.TonemapCurve
import android.media.ImageReader
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class CameraController (private val context: Context){
    private val TAG = "CameraController"
    private var mCameraId: String? = null
    private var mCaptureSession: CameraCaptureSession? = null
    private var mCameraDevice: CameraDevice? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var imageReader: ImageReader? = null
    private var file: File? = null

    private val exposure: Long = 0
    private var timeStamp: String? = null
    private val mCameraOpenCloseLock = Semaphore(1)

    internal class CompareSizesByArea : Comparator<Size?> {
        override fun compare(lhs: Size?, rhs: Size?): Int {
            // We cast here to ensure the multiplications won't overflow
            return java.lang.Long.signum(lhs?.width!!.toLong() * lhs.height - rhs!!.width.toLong() * rhs.height)
        }
    }

    fun openCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        setUpCameraOutputs()
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            startBackgroundThread()
            Log.e(TAG, "THREAD STARTED!")
            manager.openCamera(mCameraId!!, mStateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.", e)
        }
    }

    private fun setUpCameraOutputs() {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)

                // front camera setting
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    continue
                }
                val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    ?: continue

                // For still image captures, we use the largest available size.
                val largest = Collections.max(Arrays.asList(*map.getOutputSizes(ImageFormat.JPEG)), CompareSizesByArea())
                imageReader = ImageReader.newInstance(largest.width, largest.height, ImageFormat.JPEG,  /*maxImages*/2)
                imageReader!!.setOnImageAvailableListener(mOnImageAvailableListener, backgroundHandler)
                Log.d("image available listener made", "made!")
                mCameraId = cameraId
                return
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: NullPointerException) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            e.printStackTrace()
        }
    }

    private fun computeTemperature(): RggbChannelVector // use factor to get rggb
    {
//            return new RggbChannelVector(0.635f + (0.0208333f * factor), 1.0f, 1.0f, 3.7420394f + (-0.0287829f * factor));
        return RggbChannelVector(1.5f, 1.0f, 1.0f, 2.5f)
    }

    private fun transformer(): ColorSpaceTransform {
        val elementsArray = intArrayOf(1, 1, 0, 1, 0, 1,
            0, 1, 1, 1, 0, 1,
            0, 1, 0, 1, 1, 1)

        return ColorSpaceTransform(elementsArray)
    }

    private fun toneCurve(): TonemapCurve {
        val curveRed = floatArrayOf(0.0f, 0.0f, 1.0f, 1.0f)
        val curveGreen = floatArrayOf(0.0f, 0.0f, 1.0f, 1.0f)
        val curveBlue = floatArrayOf(0.0f, 0.0f, 1.0f, 1.0f)

        return TonemapCurve(curveRed, curveGreen, curveBlue)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun takePicture(cnt : Int, exp: Long): File? {
        file = getOutputMediaFile(cnt)
        try {
            if (null == mCameraDevice) {
                // return null;
            }

            // This is the CaptureRequest.Builder that we use to take a picture.
            val captureBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(imageReader!!.surface)

            captureBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF)
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            // white balance part
            captureBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF) // auto off
            captureBuilder.set(CaptureRequest.CONTROL_EFFECT_MODE, CaptureRequest.CONTROL_EFFECT_MODE_OFF)
            captureBuilder.set(CaptureRequest.CONTROL_POST_RAW_SENSITIVITY_BOOST, 100)
            captureBuilder.set(CaptureRequest.CONTROL_SCENE_MODE, CaptureRequest.CONTROL_SCENE_MODE_DISABLED)
            captureBuilder.set(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE, CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_OFF) // white balance
            captureBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX) // mode set
            captureBuilder.set(CaptureRequest.COLOR_CORRECTION_GAINS, computeTemperature()) // white balance
            captureBuilder.set(CaptureRequest.COLOR_CORRECTION_TRANSFORM, transformer()) // white balance
            // KC
            captureBuilder.set(CaptureRequest.BLACK_LEVEL_LOCK, false) // white balance
            captureBuilder.set(CaptureRequest.CONTROL_EFFECT_MODE, CaptureRequest.CONTROL_EFFECT_MODE_OFF)
            captureBuilder.set(CaptureRequest.DISTORTION_CORRECTION_MODE, CaptureRequest.DISTORTION_CORRECTION_MODE_OFF)
            captureBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_OFF)
            captureBuilder.set(CaptureRequest.HOT_PIXEL_MODE, CaptureRequest.HOT_PIXEL_MODE_OFF)
            captureBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF)
            captureBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_MINIMAL)
            captureBuilder.set(CaptureRequest.SHADING_MODE, CaptureRequest.SHADING_MODE_OFF)
            captureBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF)
            captureBuilder.set(CaptureRequest.TONEMAP_MODE, CaptureRequest.TONEMAP_MODE_CONTRAST_CURVE)
            captureBuilder.set(CaptureRequest.TONEMAP_CURVE,toneCurve())
            // exposure part
            captureBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 695) // 695, 300:a50
            captureBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exp*1000)
            captureBuilder.set(CaptureRequest.JPEG_QUALITY, 100.toByte())

            val CaptureCallback: CameraCaptureSession.CaptureCallback = object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                    Log.e("CAPTURED", "captured")
//                    Log.e("RESULT", result.partialResults[0].toString() + "")
                }
            }
            mCaptureSession!!.stopRepeating()
            mCaptureSession!!.capture(captureBuilder.build(), CaptureCallback, null)
            Log.e(TAG, "capturing...$cnt") // debug
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return file
    }

    private fun getOutputMediaFile(cnt: Int): File? {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        val mediaStorageDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "OLED_FPM")

        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null
            }
        }
        val mediaFile: File
        // Create a media file name
        if (cnt == 0) {
            timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        }
        mediaFile = File(
                mediaStorageDir.path + File.separator + "IMG_" +
                        timeStamp + "_"  + "_" + cnt.toString().padStart(3, '0')  + ".jpg")
        return mediaFile
    }
}