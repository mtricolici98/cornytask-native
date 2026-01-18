package com.nobadhabbits.cornytask.features.todo

import android.os.Bundle
import android.view.Gravity
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.nobadhabbits.cornytask.ui.theme.Cornytaskv2Theme

class EditTodoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isFromWidget = intent.getBooleanExtra("isFromWidget", false)
        setContent {
            Cornytaskv2Theme {
                EditTodoScreen (onNavigateUp = {
                    if (isFromWidget) {
                        if (resources.configuration.smallestScreenWidthDp >= 600) {
                            val displayMetrics = resources.displayMetrics
                            val screenWidth = displayMetrics.widthPixels
                            val screenHeight = displayMetrics.heightPixels

                            val width = (screenWidth * 0.4).toInt()  // 40% of screen width
                            val height = (screenHeight * 0.70).toInt() // 50% of screen height

                            window.setLayout(width, height)
                            window.setGravity(Gravity.TOP or Gravity.END) // aligns to top-right
                        }
                        finishAndRemoveTask()
                    } else {
                        finish()
                    }
                })
            }
        }
    }
}
