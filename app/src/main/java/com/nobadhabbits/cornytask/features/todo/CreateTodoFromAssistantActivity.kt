package com.nobadhabbits.cornytask.features.todo

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class CreateTodoFromAssistantActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("AssistantEntry", "action=${intent.action} data=${intent.data} extras=${intent.extras}")
        val raw = getQuery()

        val regex = Regex("""\bto\s+(.+?)\s+for\s+(\d+)\s+coins?\b""", RegexOption.IGNORE_CASE)
        val match = regex.find(raw)

        val title = match?.groupValues?.getOrNull(1)?.trim()
        val coins = match?.groupValues?.getOrNull(2)?.toIntOrNull()


        if (title != null && coins != null) {
            val todoRepository = TodoRepository(application)
            lifecycleScope.launch {
                todoRepository.addTodo(title, "", coins, null)
                finish()
            }
        } else {
            finish()
        }
    }

    private fun getQuery(): String {
        // 1) extras
        intent.getStringExtra("query")?.let { return it }

        // 2) URI query param
        val uri = intent.data
        uri?.getQueryParameter("query")?.let { return it }

        // 3) fallback (some callers use "q")
        intent.getStringExtra("q")?.let { return it }
        uri?.getQueryParameter("q")?.let { return it }

        return ""
    }

}