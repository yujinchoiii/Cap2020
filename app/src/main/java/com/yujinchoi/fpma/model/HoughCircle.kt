package com.yujinchoi.fpma.model

data class HoughCircle_xy(val center_x : Int?, val center_y : Int?, val radius : Int? ) {

    override fun toString(): String {
        return "HoughCircle_xy(center_x=$center_x, center_y=$center_y, radius=$radius)"
    }
}

data class HoughCircle_sin(val sin_x : Double?, val sin_y : Double?) {

}