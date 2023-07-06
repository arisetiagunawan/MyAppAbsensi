package com.example.myappabsensi

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity

class SplashScreen : AppCompatActivity() {
    private lateinit var pref: Preferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        supportActionBar?.hide()

        pref = Preferences(this)

        Handler().postDelayed({
            val intent =Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }, 1000)
    }
}
