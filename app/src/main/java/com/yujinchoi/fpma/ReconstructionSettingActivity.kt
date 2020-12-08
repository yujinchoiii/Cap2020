package com.yujinchoi.fpma

import android.content.Intent
import android.database.Cursor
import android.graphics.*
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.yujinchoi.fpma.model.HoughCircle_xy
import kotlinx.android.synthetic.main.activity_reconstruction_setting.*
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.core.Point
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File


class ReconstructionSettingActivity : AppCompatActivity() {
    private val TAG = "ReconstructionSettingActivity"
    private lateinit var root : File
    private lateinit var imgName : String
    private lateinit var filepath : String
    private var dP = 1
    private var minDist = 30
    private var param1 = 30
    private var param2 = 20
    private var minR = 175
    private var maxR = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reconstruction_setting)

        initUI()
    }

    private fun initUI() {
        root = Environment.getExternalStorageDirectory()
        imgName = (20).toString().padStart(3, '0') + ".jpg"
        filepath = "$root/Download/$imgName"
        val image = File(filepath)
        val bmOptions: BitmapFactory.Options = BitmapFactory.Options()
        var bitmap: Bitmap? = BitmapFactory.decodeFile(image.absolutePath, bmOptions)
        bitmap = Bitmap.createScaledBitmap(bitmap!!, 400, 300, true)
        view_img.setImageBitmap(bitmap)

        btn_check.setOnClickListener {
            setParams()
            houghCircleDetection()
        }
        btn_confirm.setOnClickListener {
            goToNextActivity()
        }
        back_button_rescset.setOnClickListener {
            finish()
        }
    }

    private fun setParams(){
        if (editText_dP.text.isNotEmpty()) {
            dP = editText_dP.text.toString().toInt()
        }
        if (editText_minDist.text.isNotEmpty()) {
            minDist = editText_minDist.text.toString().toInt()
        }
        if (editText_param1.text.isNotEmpty()) {
            param1 = editText_param1.text.toString().toInt()
        }
        if (editText_param2.text.isNotEmpty()) {
            param2 = editText_param2.text.toString().toInt()
        }
        if (editText_minR.text.isNotEmpty()) {
            minR = editText_minR.text.toString().toInt()
        }
        if (editText_maxR.text.isNotEmpty()) {
            maxR = editText_maxR.text.toString().toInt()
        }
    }

    private fun goToNextActivity() {
        val intent = Intent(applicationContext, ReconstructionActivity::class.java)
        intent.putExtra("dP", dP)
        intent.putExtra("minDist", minDist)
        intent.putExtra("param1", param1)
        intent.putExtra("param2", param2)
        intent.putExtra("minR", minR)
        intent.putExtra("maxR", maxR)
        startActivity(intent)
        finish()
    }

    private fun houghCircleDetection() {
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
            colorImage, circles, Imgproc.HOUGH_GRADIENT, dP.toDouble(), minDist.toDouble(),
            param1.toDouble(), param2.toDouble(), minR, maxR)
        val detected = colorImage.clone()
        var circleXy: HoughCircle_xy?
        if (circles.cols() > 0) {
            for(i in 0 until circles.cols()) {
                val c1xyr = circles.get(0, i)
                circleXy = HoughCircle_xy(
                    Math.round(c1xyr[0]).toInt(),
                    Math.round(c1xyr[1]).toInt(),
                    Math.round(c1xyr[2]).toInt()
                )
                val xy = Point(circleXy.center_x!!.toDouble(), circleXy.center_y!!.toDouble())
                Log.d(TAG, "detected circle info : $xy , ${circleXy.radius}")
                Imgproc.circle(detected, xy, circleXy.radius!!, Scalar(255.0, 255.0, 255.0), 3)
            }
        }
        var bitmap2 = Bitmap.createBitmap(
            detected.cols(),
            detected.rows(),
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(detected, bitmap2)
        view_img.setImageBitmap(bitmap2)
    }

}