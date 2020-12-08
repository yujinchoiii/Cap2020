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
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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
            goReconstructionSetting()
        }
        btn_takepic.setOnClickListener {
            goCameraSetting()
        }
    }

    private fun goCameraSetting(){
        val intent = Intent(applicationContext, CameraSettingActivity::class.java)
        startActivity(intent)
       // finish()
    }

    private fun goReconstructionSetting() {
        val intent = Intent(applicationContext, ReconstructionSettingActivity::class.java)
        startActivity(intent)
       // finish()
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
}