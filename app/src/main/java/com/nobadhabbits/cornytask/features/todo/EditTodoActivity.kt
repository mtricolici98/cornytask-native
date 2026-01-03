package com.nobadhabbits.cornytask.features.todo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.nobadhabbits.cornytask.ui.theme.Cornytaskv2Theme

class EditTodoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Cornytaskv2Theme {
                EditTodoScreen(onNavigateUp = { finish() })
            }
        }
    }
}
