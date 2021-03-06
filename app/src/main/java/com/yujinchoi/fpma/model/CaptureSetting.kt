package com.yujinchoi.fpma.model

import java.io.File
import java.io.Serializable
import java.util.ArrayList

class CaptureSetting(
        var dotsInRow: Int,
        var dotRadius: Int,
        var stepSize: Int,
        var exposureTime: Long,
        var centerX: Int,
        var centerY: Int,
        var sampleheight: Int,
        var colorCode: Int,
        var reconstructMode: Int
) : Serializable