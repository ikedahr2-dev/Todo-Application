package com.example.inventory.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.inventory.R
import com.example.inventory.ui.AppViewModelProvider

@Composable
fun GameScreen(
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    //ViewModelから最新のゲーム状態（水分量）をリアルタイムで取得
    val uiState by viewModel.uiState.collectAsState()
    val waterStoredPercent = uiState.waterStoredPercent

    //背景画像リスト
    val imageList = listOf(
        R.drawable.game_hill_default,
        R.drawable.game_hill_tree_level1,
    )
    var currentImageIndex by remember { mutableStateOf(0) }

    //ゲージのアニメーション用
    val animatedProgress by animateFloatAsState(
        targetValue = (waterStoredPercent / 100f).coerceIn(0f, 2f), //最大200%までゲージ表示可能に設定
        label = "WaterGaugeAnimation"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        //背景画像（タップで切り替え）
        Image(
            painter = painterResource(id = imageList[currentImageIndex]),
            contentDescription = "ゲーム背景",
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    currentImageIndex = (currentImageIndex + 1) % imageList.size
                },
            contentScale = ContentScale.Crop
        )

        //上部：水やりステータスUIパネル
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "💧 蓄えられた水分量: $waterStoredPercent %",
                color = Color.Cyan,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            //水分量ゲージ
            LinearProgressIndicator(
                progress = animatedProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
                color = Color(0xFF2196F3),
                trackColor = Color.Transparent
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (waterStoredPercent >= 100) "水やりが ${waterStoredPercent / 100} 回可能です！" else "タスクを完了して一括削除すると水が溜まるよ！",
                color = Color.White,
                fontSize = 13.sp
            )
        }
    }
}