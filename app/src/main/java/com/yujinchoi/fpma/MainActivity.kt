package com.yujinchoi.fpma

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.yujinchoi.fpma.model.HoughCircle_sin
import com.yujinchoi.fpma.model.HoughCircle_xy
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File


class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_PICK_IMAGE = 11
    var exposure_time = 40000
    private lateinit var root : File

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
        btn_reconstruct.setOnClickListener {
            reconstructImages()
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

    private fun reconstructImages() {
        root = Environment.getExternalStorageDirectory()
        for (i in 1 until 36) {
            Log.d(TAG, "detected circle info : num is $i")
            val imgName = (i).toString().padStart(3, '0') + ".jpg"
            val filepath = "$root/Download/$imgName"
            val image = Imgcodecs.imread(filepath, Imgcodecs.IMREAD_COLOR)
            Log.d(TAG, "returned image info is ${image}")
            val colorImage = Mat(image.rows(), image.cols(), CvType.CV_32F)
            Core.extractChannel(image, colorImage, 0)
            var bitmap = Bitmap.createBitmap(
                colorImage.cols(),
                colorImage.rows(),
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(colorImage, bitmap)
            bitmap = android.media.ThumbnailUtils.extractThumbnail(bitmap, 400, 300)
            val paddedBitmap = Bitmap.createBitmap(800, 700, Bitmap.Config.ARGB_8888)
            val can = Canvas(paddedBitmap)
            can.drawARGB(Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK) //black padding
            can.drawBitmap(bitmap, 200.toFloat(), 200.toFloat(), Paint(Paint.FILTER_BITMAP_FLAG))
//                    view_img.setImageBitmap(paddedBitmap)
            Utils.bitmapToMat(paddedBitmap, colorImage)
            Imgproc.cvtColor(colorImage, colorImage, Imgproc.COLOR_BGR2GRAY)
            Imgproc.medianBlur(colorImage, colorImage, 5)
            Imgproc.GaussianBlur(colorImage, colorImage, Size(1.0, 1.0), 100.0, 100.0)
            val circles = Mat()
            Imgproc.HoughCircles(
                colorImage, circles, Imgproc.HOUGH_GRADIENT, 1.0, 30.0,
                30.0, 20.0, 175, 200
            )
            val detected = colorImage.clone()
            var circleXy: HoughCircle_xy?
            if (circles.cols() > 0) {
                Log.d(TAG, "detected circle info : circleNum is ${circles.cols()}")
                val c1xyr = circles.get(0, 0)
                circleXy = HoughCircle_xy(
                    Math.round(c1xyr[0]).toInt(), Math.round(c1xyr[1]).toInt(), Math.round(
                        c1xyr[2]
                    ).toInt()
                )
                val xy = Point(circleXy.center_x!!.toDouble(), circleXy.center_y!!.toDouble())
                Log.d(TAG, "detected circle info : $xy , ${circleXy.radius}")
                Imgproc.circle(detected, xy, circleXy.radius!!, Scalar(0.0, 0.0, 255.0), 3)
            }
            Utils.matToBitmap(colorImage, paddedBitmap)
        }
    }

    private fun pickFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.setAction(Intent.ACTION_GET_CONTENT)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
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
                    Log.d(TAG, "returned clipdata is ${data?.clipData}")
                    Log.d(TAG, "returned data is ${data?.data}")
                    }
//                    data?.let { houghCircleDetection(it) }
                }
            }

    }

    fun posToSin(circle : HoughCircle_xy) : HoughCircle_sin {
        var sin_x : Double? = null
        var sin_y : Double? = null
        if (circle.center_x in 145..155) {
            sin_x = -0.119145221
        } else if (circle.center_x in 225..245) {
            sin_x = -0.079745222
        } else if (circle.center_x in 295..350) {
            sin_x = -0.039968038
        } else if (circle.center_x in 390..420) {
            sin_x = 0.0
        } else if (circle.center_x in 470..510) {
            sin_x = 0.039968038
        } else if (circle.center_x in 575..585) {
            sin_x = 0.079681907
        } else if (circle.center_x in 640..680) {
            sin_x = 0.119051369
        }

        if (circle.center_y in 80..100) {
            sin_y = -0.119051369
        } else if (circle.center_y in 160..180) {
            sin_y = -0.079681907
        } else if (circle.center_y in 240..260) {
            sin_y = -0.039968038
        } else if (circle.center_y in 330..350) {
            sin_y = 0.0
        } else if (circle.center_y in 420..450) {
            sin_y = 0.039968038
        } else if (circle.center_y in 510..530) {
            sin_y = 0.079681907
        } else if (circle.center_y in 590..610) {
            sin_y = 0.119051369
        }
        return HoughCircle_sin(sin_x, sin_y)
    }

    private fun houghCircleDetection(data: Intent) {

        data.clipData?.let {
            for (i in 0 until it.itemCount ) {
                var imgString = ""
                val uri = it.getItemAt(i).uri
                Log.d(TAG, "returned clipdata uri are ${uri}")
                val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
                val cursor: Cursor? = contentResolver.query(
                    uri,
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
                var bitmap = Bitmap.createBitmap(
                    colorImage.cols(),
                    colorImage.rows(),
                    Bitmap.Config.ARGB_8888
                )
                Utils.matToBitmap(colorImage, bitmap)
                bitmap = android.media.ThumbnailUtils.extractThumbnail(bitmap, 400, 300)
                val paddedBitmap = Bitmap.createBitmap(800, 700, Bitmap.Config.ARGB_8888)
                val can = Canvas(paddedBitmap)
                can.drawARGB(Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK) //black padding
                can.drawBitmap(bitmap, 200.toFloat(), 200.toFloat(), Paint(Paint.FILTER_BITMAP_FLAG))
//                    view_img.setImageBitmap(paddedBitmap)
                Utils.bitmapToMat(paddedBitmap, colorImage)
                Imgproc.cvtColor(colorImage, colorImage, Imgproc.COLOR_BGR2GRAY)
                Imgproc.GaussianBlur(colorImage, colorImage, Size(1.0, 1.0), 0.0)
                val circles = Mat()
                Imgproc.HoughCircles(
                    colorImage, circles, Imgproc.HOUGH_GRADIENT, 1.0, 30.0,
                    30.0, 20.0, 180, 200
                )
                val detected = colorImage.clone()
                var circle_Xy : HoughCircle_xy?
                if (circles.cols() > 0) {
                    Log.d(TAG, "detected circle info : ${circles.cols()}")
                    for (x in 0 until circles.cols()) {
                        val c1xyr = circles.get(0, x)
                        circle_Xy = HoughCircle_xy(
                            Math.round(c1xyr[0]).toInt(), Math.round(c1xyr[1]).toInt(), Math.round(
                                c1xyr[2]
                            ).toInt()
                        )
                        val xy = Point(circle_Xy.center_x!!.toDouble(), circle_Xy.center_y!!.toDouble())
                        Log.d(TAG, "detected circle info : $xy , ${circle_Xy.radius}")
                        Imgproc.circle(detected, xy, circle_Xy.radius!!, Scalar(0.0, 0.0, 255.0), 3)
                    }
                }
                Utils.matToBitmap(colorImage, paddedBitmap)
            }
        }
    }
}