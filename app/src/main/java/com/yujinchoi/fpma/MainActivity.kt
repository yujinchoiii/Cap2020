package com.yujinchoi.fpma

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_PICK_IMAGE = 11
    var exposure_time = 40000

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
            pickFromGallery()
        }
        btn_takepic.setOnClickListener {
            takePicture()
        }
    }

    private fun takePicture(){
        if (editText_exp.text.isNotEmpty()) {
            exposure_time = editText_exp.text.toString().toInt()
        }
        val intent = Intent(applicationContext, CameraActivity::class.java)
        intent.putExtra("exposure_time", exposure_time)
        startActivity(intent)
        finish()
    }

    private fun pickFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        val mimeTypes = arrayOf("image/jpeg", "image/png")
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        startActivityForResult(intent, REQUEST_PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode){
            1 -> {
                if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
                    }
                }
            11 -> {
                if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
                    Log.d(TAG, "returned data is ${data?.data?.path}")
                    var imgString = ""
                    val selectedImage = data!!.data
                    val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
                    val cursor: Cursor? = contentResolver.query(
                        selectedImage!!,
                        filePathColumn, null, null, null
                    )
                    cursor!!.moveToFirst()

                    val columnIndex: Int = cursor.getColumnIndex(filePathColumn[0])
                    imgString = cursor.getString(columnIndex)
                    cursor.close()

                    val file = File(imgString)
                    val image = Imgcodecs.imread(file.absolutePath, Imgcodecs.IMREAD_COLOR)
                    Log.d(TAG, "returned image info is ${image}")
                    val colorImage = Mat(image.rows(), image.cols(), CvType.CV_32F)
                    Core.extractChannel(image, colorImage, 0)
                    val bitmap = Bitmap.createBitmap(
                        colorImage.cols(),
                        colorImage.rows(),
                        Bitmap.Config.ARGB_8888
                    )
                    Utils.matToBitmap(colorImage, bitmap)
                    Utils.bitmapToMat(bitmap, colorImage)
                    Imgproc.cvtColor(colorImage, colorImage, Imgproc.COLOR_BGR2GRAY)
                    Imgproc.GaussianBlur(colorImage, colorImage, Size(1.0, 1.0), 0.0)
                    val circles = Mat()
                    Imgproc.HoughCircles(colorImage, circles, Imgproc.HOUGH_GRADIENT, 1.0, 30.0,
                        40.0, 30.0, 0, 0)
                    val detected = colorImage.clone()
                    if (circles.cols()>0) {
                        Log.d(TAG, "detected circle info : ${circles.cols()}")
                        for (x in 0 until circles.cols()) {
                            val c1xyr = circles.get(0, x)
                            val xy = Point(Math.round(c1xyr[0]).toDouble(), Math.round(c1xyr[1]).toDouble())
                            val radius = Math.round(c1xyr[2]).toInt()
                            Log.d(TAG, "detected circle info : $xy , $radius")
                            Imgproc.circle(detected, xy, radius, Scalar(0.0, 0.0, 255.0), 3)
                        }
                    }
                    Utils.matToBitmap(colorImage, bitmap)
                    view_img.setImageBitmap(bitmap)
                }

            }
        }
    }

}