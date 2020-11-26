package com.yujinchoi.fpma

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.ColorSpaceTransform
import android.hardware.camera2.params.RggbChannelVector
import android.hardware.camera2.params.TonemapCurve
import android.icu.text.SimpleDateFormat
import android.media.ImageReader
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private val REQUEST_IMAGE_CAPTURE = 1
    private lateinit var currentPhotopath: String
    var exposure_time = 400

    companion object { init { OpenCVLoader.initDebug() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initSettings()
    }

    private val permissions: Unit
        get() {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED) {
                //Requesting permission.
                ActivityCompat.requestPermissions(
                        this, arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), 1
                )
            }
        }

    private fun initSettings() {
        permissions
        btn_gogallery.setOnClickListener {
        }
        btn_takepic.setOnClickListener {
            if (editText_exp.text.isNotEmpty()) {
                exposure_time = editText_exp.text.toString().toInt()
            }
            val intent = Intent(applicationContext, CameraActivity::class.java)
            intent.putExtra("exposure_time", exposure_time)
            startActivity(intent)
            finish()
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            if (takePictureIntent.resolveActivity(this.packageManager) != null) {
                // 찍은 사진을 그림파일로 만들기
                val photoFile: File? =
                    try {
                        createImageFile()
                    } catch (ex: IOException) {
                        Log.d("TAG", "dispatchTakePictureIntent error")
                        null
                    }

                // 그림파일을 성공적으로 만들었다면 onActivityForResult로 보내기
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this, "com.yujinchoi.fpma.fileprovider", it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }


    // 카메라로 촬영한 이미지를 파일로 저장해준다
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotopath = absolutePath
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode){
            1 -> {
                if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {

                    // 카메라로부터 받은 데이터가 있을경우에만
                    val file = File(currentPhotopath)
                    if (Build.VERSION.SDK_INT < 28) {
                        val bitmap = MediaStore.Images.Media
                            .getBitmap(contentResolver, Uri.fromFile(file))  //Deprecated
                        // imageview.setImageBitmap(bitmap)
                        // val tmp = Mat(bitmap.height, bitmap.width, CvType.CV_8UC1)
                        val src = Mat()
                        Log.d(
                            TAG,
                            "testing : $src"
                        )
                        Utils.bitmapToMat(bitmap, src)
                        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGB2GRAY)
                        Imgproc.GaussianBlur(src, src, Size(1.0, 1.0), 0.0)
                        Imgproc.HoughCircles(src, src, Imgproc.HOUGH_GRADIENT, 0.0, 0.0)
                        Utils.matToBitmap(src, bitmap)
                        view_img.setImageBitmap(bitmap)

                    } else {
                        val image = Imgcodecs.imread(currentPhotopath, Imgcodecs.IMREAD_COLOR)
                        val colorImage = Mat(image.rows(), image.cols(), CvType.CV_32F)
 //                       Core.extractChannel(image, colorImage, 0)
                        val bitmap = Bitmap.createBitmap(colorImage.cols(), colorImage.rows(), Bitmap.Config.ARGB_8888)
//                        Utils.matToBitmap(colorImage, bitmap)
//                        Utils.bitmapToMat(bitmap, colorImage)
                        Imgproc.cvtColor(colorImage, colorImage, Imgproc.COLOR_RGB2GRAY)
                        Imgproc.GaussianBlur(colorImage, colorImage, Size(1.0, 1.0), 0.0)
                        Imgproc.HoughCircles(colorImage, colorImage, Imgproc.HOUGH_GRADIENT, 0.0, 1.0)
                        Utils.matToBitmap(colorImage, bitmap)
                        view_img.setImageBitmap(bitmap)
                    }
                }
            }
        }
    }

}