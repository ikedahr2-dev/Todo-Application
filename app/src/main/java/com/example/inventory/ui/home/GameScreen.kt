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

    //テスト用に初期状態を100%満タンで固定（ViewModelの値が0でも100%からスタートさせる）
    //本番でToDoアプリと連動させる時は、ここを「uiState.waterStoredPercent」に戻す
    val waterStoredPercent = uiState.waterStoredPercent
    //val waterStoredPercent = 100

    //成長に必要な水やりの回数（仮:3回タップ）
    val growthTapLimit = 3
    //現在のタップ回数を保持する状態変数
    var currentTapCount by remember { mutableStateOf(0) }

    //木がLv.0からLv.1へ成長したかどうかを判定する状態変数
    var isGrown by remember { mutableStateOf(false) }

    //レベルに応じた背景画像の切り替えロジック
    val currentImageResId = if (isGrown) {
        R.drawable.game_tree_level_1 //成長後の画像
    } else {
        R.drawable.game_tree_lv_0   //成長前の画像
    }

    //ゲージのアニメーション用
    val animatedProgress by animateFloatAsState(
        targetValue = (waterStoredPercent / 100f).coerceIn(0f, 2f), //最大200%までゲージ表示可能に設定(仮)
        label = "WaterGaugeAnimation"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        //背景画像（タップで成長させる）
        Image(
            painter = painterResource(id = currentImageResId),
            contentDescription = "ゲーム背景",
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    //成長前（isGrown == false）の時だけタップを検知する
                    if (!isGrown && waterStoredPercent >= 100) {
                        currentTapCount++

                        //タップが3回に達したら成長させる(レベルごとに回数を変更予定)
                        if (currentTapCount >= growthTapLimit) {
                            viewModel.updateWaterStoredPercent(waterStoredPercent - 100)
                            isGrown = true
                        }
                    }
                },
            contentScale = ContentScale.Crop //画面の比率を保ったまま全画面表示
        )

        //上部：水やりステータスおよび成長ステータスUIパネル
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

            Spacer(modifier = Modifier.height(12.dp))

            //下部：成長メッセージ
            if (!isGrown) {
                if (waterStoredPercent >= 100) {
                    //成長させる方法を表示
                    val remainingTaps = growthTapLimit - currentTapCount
                    Text(
                        text = "タップで水をあげる！",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                else {
                    Text(
                        text = "水が足りません(100%必要です)。\nタスクを完了させて水を貯めよう！",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                }
            }
            else
            {
                //成長後のメッセージ
                Text(
                    text = "木が成長しました！Lv.1！🌱",
                    color = Color(0xFF4CAF50), //緑色
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }