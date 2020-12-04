package com.yujinchoi.fpma

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import com.yujinchoi.fpma.model.HoughCircle_sin
import com.yujinchoi.fpma.model.HoughCircle_xy
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File

class ReconstructionActivity : AppCompatActivity() {
    private val TAG = "ReconstructionActivity"
    private lateinit var root : File
    private lateinit var posList : MutableList<HoughCircle_xy>
    private lateinit var sinList : MutableList<HoughCircle_sin>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reconstruction)
        detectCircles()
    }

    private fun detectCircles() {
        root = Environment.getExternalStorageDirectory()
        posList = mutableListOf()
        sinList = mutableListOf()
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
                val c1xyr = circles.get(0, 0)
                circleXy = HoughCircle_xy(
                    Math.round(c1xyr[0]).toInt(),
                    Math.round(c1xyr[1]).toInt(),
                    Math.round(c1xyr[2]).toInt()
                )
                val xy = Point(circleXy.center_x!!.toDouble(), circleXy.center_y!!.toDouble())
                Log.d(TAG, "detected circle info : $xy , ${circleXy.radius}")
                posList.add(circleXy)
                sinList.add(posToSin(circleXy))
//                Imgproc.circle(detected, xy, circleXy.radius!!, Scalar(0.0, 0.0, 255.0), 3)
            }
//            Utils.matToBitmap(colorImage, paddedBitmap)
        }
        Log.d(TAG, "position list : $posList")
        Log.d(TAG, "sin list : $sinList")
    }

    private fun posToSin(circle : HoughCircle_xy) : HoughCircle_sin {
        var sin_x : Double? = null
        var sin_y : Double? = null
        if (circle.center_x in 140..160) {
            sin_x = -0.119145221
        } else if (circle.center_x in 225..245) {
            sin_x = -0.079745222
        } else if (circle.center_x in 295..350) {
            sin_x = -0.039968038
        } else if (circle.center_x in 390..420) {
            sin_x = 0.0
        } else if (circle.center_x in 470..510) {
            sin_x = 0.039968038
        } else if (circle.center_x in 570..600) {
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
}