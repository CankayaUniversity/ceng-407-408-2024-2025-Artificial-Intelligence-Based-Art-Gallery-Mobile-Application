package com.example.socialmediaapp

import android.app.Application

class MyApplication: Application() {
    companion object
    {
        lateinit var instance: MyApplication
            private set

    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}