package com.example.fruitninjaauto

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    companion object { const val REQ_CAPTURE = 9001 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val info = TextView(this).apply {
            text = "Fruit Ninja Auto\n\n1) فعّل خدمة الوصول من إعدادات إمكانية الوصول.\n" +
                    "2) اضغط بدء الالتقاط ووافق على تصوير الشاشة.\n" +
                    "3) افتح Fruit Ninja ثم شغّل الخدمة.\n\n" +
                    "الخدمة تتجنب الأجسام الداكنة/البنفسجية الدائرية كاحتمال للقنابل."
            textSize = 17f
            setPadding(40, 50, 40, 30)
        }
        val capture = Button(this).apply {
            text = "بدء التقاط الشاشة"
            setOnClickListener {
                val mgr = getSystemService(MediaProjectionManager::class.java)
                startActivityForResult(mgr.createScreenCaptureIntent(), REQ_CAPTURE)
            }
        }
        val openA11y = Button(this).apply {
            text = "فتح إعدادات إمكانية الوصول"
            setOnClickListener {
                startActivity(Intent("android.settings.ACCESSIBILITY_SETTINGS"))
            }
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(info)
            addView(openA11y)
            addView(capture)
        }
        setContentView(layout)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CAPTURE && resultCode == RESULT_OK && data != null) {
            CaptureService.start(this, resultCode, data)
        }
    }
}
