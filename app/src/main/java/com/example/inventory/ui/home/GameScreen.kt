package com.example.inventory.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventory.R

@Composable
fun GameScreen() {
    val imageList = listOf(
        R.drawable.game_hill_default,            //1枚目（太陽と雲の丘）
        android.R.drawable.ic_menu_gallery,      //2枚目（テスト用仮リソース）
        android.R.drawable.ic_menu_day           //3枚目（テスト用仮リソース）
    )

    var currentImageIndex by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                // 画面をタップするたびに次の画像へ進む
                currentImageIndex = (currentImageIndex + 1) % imageList.size
            }
    ) {
        Image(
            painter = painterResource(id = imageList[currentImageIndex]),
            contentDescription = "ゲーム背景",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop //画面の比率を保ったまま全画面表示
        )

        //画面右上のメッセージ
        Text(
            text = "画面タップで切り替え",
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}