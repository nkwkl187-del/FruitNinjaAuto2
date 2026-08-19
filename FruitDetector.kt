package com.example.fruitninjaauto

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.*

object FruitDetector {
    // Heuristic detector designed around the supplied Fruit Ninja screenshots.
    // It looks for saturated/colorful blobs and rejects dark-purple/black bomb-like blobs.
    fun process(bmp: Bitmap) {
        val w = bmp.width
        val h = bmp.height

        // Ignore HUD and bottom controls.
        val x0 = (w * 0.04f).toInt()
        val x1 = (w * 0.96f).toInt()
        val y0 = (h * 0.16f).toInt()
        val y1 = (h * 0.90f).toInt()

        val step = 10
        val candidates = mutableListOf<Pair<Float,Float>>()

        // Coarse grid: find strongly colored pixels, then cluster nearby points.
        val pts = ArrayList<Pair<Int,Int>>()
        for (y in y0 until y1 step step) {
            for (x in x0 until x1 step step) {
                val c = bmp.getPixel(x, y)
                val hsv = FloatArray(3)
                Color.colorToHSV(c, hsv)
                val sat = hsv[1]; val v = hsv[2]
                val r = Color.red(c); val g = Color.green(c); val b = Color.blue(c)

                // Reject dark purple/black bomb-like colors.
                val bombLike = (v < 0.62f && sat > 0.30f && hsv[0] in 245f..330f) ||
                                (r < 90 && g < 100 && b < 120)
                val colorfulFruit = sat > 0.42f && v > 0.50f
                if (colorfulFruit && !bombLike) pts.add(x to y)
            }
        }

        // Very simple spatial clustering.
        val used = BooleanArray(pts.size)
        for (i in pts.indices) {
            if (used[i]) continue
            val q = ArrayDeque<Int>()
            q.add(i); used[i] = true
            var sx = 0L; var sy = 0L; var n = 0
            while (q.isNotEmpty()) {
                val k = q.removeFirst()
                val (px, py) = pts[k]
                sx += px; sy += py; n++
                for (j in pts.indices) {
                    if (used[j]) continue
                    val (jx, jy) = pts[j]
                    if (abs(jx-px) <= 45 && abs(jy-py) <= 45) {
                        used[j] = true; q.add(j)
                    }
                }
            }
            if (n >= 5) {
                candidates.add((sx.toFloat()/n) to (sy.toFloat()/n))
            }
        }

        // Limit accidental detections and swipe between distinct targets.
        val chosen = candidates
            .filter { it.second > y0 && it.second < y1 }
            .distinctBy { (it.first/70).toInt() to (it.second/70).toInt() }
            .take(6)

        val service = FruitAccessibilityService.instance ?: return
        for ((x,y) in chosen) {
            // Extra safety check around the target: reject if local area is mostly dark purple.
            if (!safeLocalArea(bmp, x.toInt(), y.toInt())) continue
            service.swipe(x, min(y + 70f, h - 20f), x + 35f, max(y - 50f, 20f))
        }
    }

    private fun safeLocalArea(b: Bitmap, cx: Int, cy: Int): Boolean {
        var bad = 0; var total = 0
        for (dy in -25..25 step 10) for (dx in -25..25 step 10) {
            val x = (cx+dx).coerceIn(0,b.width-1)
            val y = (cy+dy).coerceIn(0,b.height-1)
            val c = b.getPixel(x,y)
            val hsv = FloatArray(3); Color.colorToHSV(c,hsv)
            if ((hsv[0] in 245f..330f && hsv[1] > .30f && hsv[2] < .62f) ||
                (Color.red(c)<80 && Color.green(c)<90 && Color.blue(c)<110)) bad++
            total++
        }
        return bad < total * 0.38
    }
}
