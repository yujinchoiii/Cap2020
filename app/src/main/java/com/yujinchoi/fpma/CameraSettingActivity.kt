package com.yujinchoi.fpma

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_camera_setting.*
import kotlinx.android.synthetic.main.activity_main.*

class CameraSettingActivity : AppCompatActivity() {
    var exposure_time = 23000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_setting)
        btn_opencamera.setOnClickListener {
            takePicture()
        }
        back_button_camset.setOnClickListener {
            finish()
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
}