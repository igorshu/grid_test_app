package com.example.gridtestapp.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.gridtestapp.R
import com.example.gridtestapp.ui.theme.DarkColorScheme
import com.example.gridtestapp.ui.theme.LightColorScheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT <=  Build.VERSION_CODES.R) {
            window.decorView.apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
            }
        } else {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        setContent {
            val scope = rememberCoroutineScope()
            val context = LocalContext.current

            LaunchedEffect(key1 = 1) {
                scope.launch {
                    delay(1000)
                    context.startActivity(Intent(context, MainActivity::class.java))
                    finish()
                }
            }

            val colorScheme = when {
                isSystemInDarkTheme() -> DarkColorScheme
                else -> LightColorScheme
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painterResource(id = R.drawable.splash_icon),
                    colorFilter = ColorFilter.tint(iconColor(isSystemInDarkTheme())),
                    contentDescription = "",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth(0.3f)
                        .aspectRatio(1.0f)
                )
            }
        }
    }

    private fun iconColor(black: Boolean): Color = when {
        black -> Color.White
        else -> Color.Black
    }
}