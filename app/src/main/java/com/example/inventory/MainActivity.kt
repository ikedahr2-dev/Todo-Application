package com.example.inventory

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.inventory.ui.theme.InventoryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // 1. アプリ起動時に通知チャンネルを作成
        createNotificationChannel()

        setContent {
            InventoryTheme {
                // 2. Android 13以上向け：アプリ起動時に通知権限を要求
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val launcher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { }
                    LaunchedEffect(Unit) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                android.Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    InventoryApp() // ★元のコードのまま、引数は追加しません
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            //5分前通知用（バナー・音ありの派手なチャンネル）
            val name1 = "ToDoリマインダー"
            val descriptionText1 = "タスクの期限を通知します"
            val importance1 = NotificationManager.IMPORTANCE_HIGH
            val channel1 = NotificationChannel("todo_notifications", name1, importance1).apply {
                description = descriptionText1
            }
            notificationManager.createNotificationChannel(channel1)

            //常駐通知用（バナーも音も出ない静かなチャンネル）
            val name2 = "ToDoリスト常駐状況"
            val descriptionText2 = "未完了のタスク数を常に表示します"
            val importance2 = NotificationManager.IMPORTANCE_LOW // ◀ LOW（低）にすることでバナーや音が一切出なくなります！
            val channel2 = NotificationChannel("ongoing_status", name2, importance2).apply {
                description = descriptionText2
            }
            notificationManager.createNotificationChannel(channel2)
        }
    }

}
