package com.example.corekotlinremaster

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.ActionBar
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class privacyInfoActivity : AppCompatActivity() {
    private val TAG = "privacyInfoActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_info)
        supportActionBar?.hide()
        val btnAgree:Button = findViewById(R.id.info_agree)
        val btnNotAgree:Button = findViewById(R.id.info_not_agree)
        val intent = Intent(this, MainActivity::class.java)


        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "no need for info check")
            startActivity(intent)
            finish() //跳转后销毁此activity
        }else {
            Log.d(TAG, "run info check")
        }

        btnAgree.setOnClickListener {
            startActivity(intent)
            finish() //跳转后销毁此activity
        }
        btnNotAgree.setOnClickListener {
            finish()
        }
    }
}