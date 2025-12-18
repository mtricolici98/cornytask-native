package com.example.cornytask_v2.features.todo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.cornytask_v2.ui.theme.Cornytaskv2Theme

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
