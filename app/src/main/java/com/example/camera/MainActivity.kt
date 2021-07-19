package com.example.camera

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
    companion object {
        private const val TAG = "MainActivity"
        fun getOutputFileDirectory(context: Context) = context.applicationContext.filesDir
    }
}