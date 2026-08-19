package com.example.fruitninjaauto

import android.app.*
import android.content.*
import android.graphics.*
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import androidx.core.app.NotificationCompat

class CaptureService : Service() {
    companion object {
        private const val CH = "fruit_ninja_capture"
        private const val ID = 71
        fun start(ctx: Context, resultCode: Int, data: Intent) {
            val i = Intent(ctx, CaptureService::class.java).apply {
                putExtra("code", resultCode); putExtra("data", data)
            }
            ctx.startForegroundService(i)
        }
    }

    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var display: android.hardware.display.VirtualDisplay? = null
    private var lastFrame = 0L

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH, "Screen capture", NotificationManager.IMPORTANCE_LOW))
        startForeground(ID, NotificationCompat.Builder(this, CH)
            .setContentTitle("Fruit Ninja Auto")
            .setContentText("تحليل الشاشة يعمل")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val code = intent?.getIntExtra("code", 0) ?: return START_NOT_STICKY
        val data = intent.getParcelableExtra<Intent>("data") ?: return START_NOT_STICKY
        val mgr = getSystemService(MediaProjectionManager::class.java)
        projection = mgr.getMediaProjection(code, data)

        val dm = resources.displayMetrics
        val w = dm.widthPixels
        val h = dm.heightPixels
        reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
        reader!!.setOnImageAvailableListener({ r ->
            val now = SystemClock.uptimeMillis()
            if (now - lastFrame < 90) { r.acquireLatestImage()?.close(); return@setOnImageAvailableListener }
            lastFrame = now
            val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                val p = image.planes[0]
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                bmp.copyPixelsFromBuffer(p.buffer)
                FruitDetector.process(bmp)
                bmp.recycle()
            } finally { image.close() }
        }, Handler(Looper.getMainLooper()))

        display = projection!!.createVirtualDisplay(
            "FruitNinjaAuto", w, h, dm.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader!!.surface, null, null
        )
        return START_STICKY
    }

    override fun onDestroy() {
        display?.release(); reader?.close(); projection?.stop()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?) = null
}
