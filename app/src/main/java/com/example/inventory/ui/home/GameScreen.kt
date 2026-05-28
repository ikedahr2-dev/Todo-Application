package com.example.inventory.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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

    //水分量(テスト時は固定の値を代入する)
    //val waterStoredPercent = uiState.waterStoredPercent
    //val waterStoredPercent = 1000
    val waterStoredPercent = 100
    //val waterStoredPercent = 50

    //現在のレベルを管理する状態変数
    var currentLevel by remember { mutableStateOf(0) }

    //現在のレベルになってから「追加で水をあげた回数」を記録する
    var givenWaterCount by remember { mutableStateOf(0) }

    //高さを3段階で管理する状態変数 (0: 地上, 1: 上空, 2: 宇宙)
    var currentHeightLayer by remember { mutableStateOf(0) }

    //現在のレベルに応じて、次の進化に必要な回数を自動で切り替える
    val totalWateringRequired = when (currentLevel) {
        0 -> 1    //Lv.0 -> Lv.1
        1 -> 1    //Lv.1 -> Lv.2
        2 -> 1    //Lv.2 -> Lv.3
        3 -> 1    //Lv.3 -> Lv.4
        4 -> 1    //Lv.4 -> Lv.5
        5 -> 1    //Lv.5 -> Lv.6
        6 -> 1    //Lv.6 -> Lv.7
        7 -> 1    //Lv.7 -> Lv.8
        8 -> 1    //Lv.8 -> Lv.9
        9 -> 1    //Lv.9 -> Lv.10
        else -> 0
    }

    //3段階の高さレイア（地上・上空・宇宙）に応じた画像切り替えロジック
    val currentImageResId = when (currentHeightLayer) {
        1 -> {
            when (currentLevel) {
                6 -> R.drawable.game_tree_sky_6
                7 -> R.drawable.game_tree_sky_7
                8 -> R.drawable.game_tree_sky_8
                else -> R.drawable.game_tree_sky_9_over
            }
        }
        2 -> {
            when (currentLevel) {
                8 -> R.drawable.game_tree_earth_8
                9 -> R.drawable.game_tree_earth_9
                10 -> R.drawable.game_tree_earth_10
                else -> R.drawable.game_tree_earth_8
            }
        }
        else -> {
            when (currentLevel) {
                1 -> R.drawable.game_tree_lv_1
                2 -> R.drawable.game_tree_lv_2
                3 -> R.drawable.game_tree_lv_3
                4 -> R.drawable.game_tree_lv_4
                5 -> R.drawable.game_tree_lv_5
                6 -> R.drawable.game_tree_lv_6
                7 -> R.drawable.game_tree_lv_7_over
                8 -> R.drawable.game_tree_lv_7_over
                9 -> R.drawable.game_tree_lv_7_over
                10 -> R.drawable.game_tree_lv_7_over
                else -> R.drawable.game_tree_lv_0
            }
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = (waterStoredPercent / 2400f).coerceIn(0f, 10f),
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
                    if (currentLevel < 10 && waterStoredPercent >= 100) {
                        viewModel.updateWaterStoredPercent(waterStoredPercent - 100)
                        givenWaterCount++

                        if (givenWaterCount >= totalWateringRequired) {
                            currentLevel++
                            givenWaterCount = 0

                            if (currentLevel == 6) {
                                currentHeightLayer = 1
                            } else if (currentLevel == 8) {
                                currentHeightLayer = 2
                            }
                        }
                    }
                },
            contentScale = ContentScale.Crop
        )

        //上部：UIパネル
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

            val msgColor = Color(0xFFFFEB3B)
            val msgAlign = androidx.compose.ui.text.style.TextAlign.Center

            when (currentLevel) {
                0 -> Text(text = if (waterStoredPercent < 100) "タスクを完了させて水を貯めましょう！" else "水をあげて芽を咲かせましょう！", color = msgColor, fontSize = 13.sp, textAlign = msgAlign)
                1 -> Text(text = if (waterStoredPercent < 100) "水をあげて芽を咲かせましょう！" else "✨芽が生えてきました！Lv.1！🌱", color = msgColor, fontSize = 13.sp, textAlign = msgAlign)
                2 -> Text(text = if (waterStoredPercent < 100) "✨芽が生えてきました！Lv.1！🌱" else "✨芽が成長しました！Lv.2！🌱", color = msgColor, fontSize = 13.sp, textAlign = msgAlign)
                3 -> Text(text = if (waterStoredPercent < 100) "✨芽が成長しました！Lv.2！🌱" else "✨木に成長しました！Lv.3！🌳", color = msgColor, fontSize = 13.sp, textAlign = msgAlign)
                4 -> Text(text = if (waterStoredPercent < 100) "✨木に成長しました！Lv.3！🌳" else "✨木が成長しました！Lv.4！🌳", color = msgColor, fontSize = 13.sp, textAlign = msgAlign)
                5 -> Text(text = if (waterStoredPercent < 100) "✨木に成長しました！Lv.4！🌳" else "✨木が成長しました！Lv.5！🌳", color = msgColor, fontSize = 13.sp, textAlign = msgAlign)
                6 -> {
                    Text(
                        text = if (currentHeightLayer == 1) "☁️ 上空へ到達！！Lv.6" else "✨木が成長しました！Lv.6！🌳",
                        color = msgColor, fontSize = 13.sp, textAlign = msgAlign
                    )
                }
                7 -> {
                    Text(
                        text = if (currentHeightLayer == 1) "☁️ 雲の上をぐんぐん突き進む！Lv.7" else "✨木が更に成長しました！Lv.7🌳",
                        color = msgColor, fontSize = 13.sp, textAlign = msgAlign
                    )
                }
                8 -> {
                    val lvl8Msg = when (currentHeightLayer) {
                        2 -> "🚀 ついに宇宙空間へ突入！！！Lv.8"
                        1 -> "☁️ 大気圏突破まであと少し！Lv.8"
                        else -> "✨木が地上を覆うほど成長しました！Lv.8🌳"
                    }
                    Text(text = lvl8Msg, color = msgColor, fontSize = 13.sp, textAlign = msgAlign)
                }
                9 -> {
                    val lvl9Msg = when (currentHeightLayer) {
                        2 -> "🌈 成層圏へ到達！！！Lv.9"
                        1 -> "☁️ 星に手が届きそう！Lv.9"
                        else -> "✨大木へ成長しました！Lv.9🌳"
                    }
                    Text(text = lvl9Msg, color = msgColor, fontSize = 13.sp, textAlign = msgAlign)
                }
                10 -> {
                    val lvl9Msg = when (currentHeightLayer) {
                        2 -> "🌌 宇宙の果てへ進化完了！！！Lv.10"
                        1 -> "☁️ 星を見下ろすはるか上空！Lv.10"
                        else -> "✨伝説の巨木へと覚醒しました！Lv.10🌳"
                    }
                    Text(text = lvl9Msg, color = msgColor, fontSize = 13.sp, textAlign = msgAlign)
                }
            }
        }

        //階層切り替えボタンエリア（縦に並ぶ形に対応）
        if (currentLevel >= 6) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp), // ボタン同士の隙間
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                //─── 【パターンA】現在「上空（レイヤー1）」にいるとき（上下2つのボタンを表示） ───
                if (currentHeightLayer == 1) {
                    //①上に行く「宇宙」ボタン（※レベル8以上のときだけ表示）
                    if (currentLevel >= 8) {
                        Button(
                            onClick = { currentHeightLayer = 2 },
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(text = "宇宙", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    //②下に行く「地上」ボタン
                    Button(
                        onClick = { currentHeightLayer = 0 },
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(text = "地上", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                //─── 【パターンB】現在「地上（レイヤー0）」または「宇宙（レイヤー2）」にいるとき ───
                else {
                    Button(
                        onClick = {
                            //地上にいるなら上空(1)へ、宇宙にいるなら上空(1)へ戻る
                            currentHeightLayer = 1
                        },
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(text = "上空", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}