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
    //val waterStoredPercent = 0

    //現在のレベルを管理する状態変数 (0: 初期状態, 1: Lv1, 2: Lv2)
    var currentLevel by remember { mutableStateOf(0) }

    //現在のレベルになってから「追加で水をあげた回数」を記録する
    var givenWaterCount by remember { mutableStateOf(0) }

    //レベル6に達成した際に画像を切り替えの選択をする
    var showAlternativeImage by remember { mutableStateOf(false) }

    //現在のレベルに応じて、次の進化に必要な回数を自動で切り替える
    //現在のレベルに応じて、次の進化に必要な回数を自動で切り替える
    val totalWateringRequired = when (currentLevel) {
        0 -> 1    //Lv.0の時は3回（300%）でLv.1へ
        1 -> 1    //Lv.1の時は5回（500%）でLv.2へ
        2 -> 1   //Lv.2の時は12回（1200%）でLv.3へ
        3 -> 1   //Lv.3の時は20回（2000%）でLv.4へ
        4 -> 1   //Lv.4の時は35回（3500%）でLv.5へ
        5 -> 1   //Lv.5の時は53回（5300%）でLv.6へ
        6 -> 1   //Lv.6(上空)の時は90回(9000%)でLv.7へ
        else -> 0 //最大レベルの時はそれ以上必要なし
    }

    //レベルおよび画面切り替えに応じた背景画像切り替えロジック
    val currentImageResId = if (showAlternativeImage) {
        //上空（切り替え先）のレベル別画像
        when (currentLevel) {
            6 -> R.drawable.game_tree_sky_6   //Lv.6の上空画像
            7 -> R.drawable.game_tree_sky_7   //Lv.7の上空画像
            else -> R.drawable.game_tree_sky_6 //Lv.6の上空画像
        }
    } else {
        //地上（いつもの画面）のレベル別画像
        when (currentLevel) {
            1 -> R.drawable.game_tree_lv_1
            2 -> R.drawable.game_tree_lv_2
            3 -> R.drawable.game_tree_lv_3
            4 -> R.drawable.game_tree_lv_4
            5 -> R.drawable.game_tree_lv_5
            6 -> R.drawable.game_tree_lv_6 //地上側は大きな木のまま固定
            7 -> R.drawable.game_tree_lv_2 //地上側は大きな木のまま固定
            else -> R.drawable.game_tree_lv_0
        }
    }

    //ゲージのアニメーション用(2400%を上限に設定)
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
                    //最大レベル(8)未満で、水が100%以上あるときタップ可能
                    if (currentLevel < 7 && waterStoredPercent >= 100) {
                        viewModel.updateWaterStoredPercent(waterStoredPercent - 100)
                        givenWaterCount++

                        //目標回数に達したらレベルアップ
                        if (givenWaterCount >= totalWateringRequired) {
                            currentLevel++
                            givenWaterCount = 0 //カウントをリセットして次の進化へ

                            //レベル6になった瞬間、自動で「上空」に画面を切り替えて気づきやすくする演出
                            if (currentLevel == 6) {
                                showAlternativeImage = true
                            }
                        }
                    }
                },
            contentScale = ContentScale.Crop
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

            //下部：成長メッセージの表示制御
            val msgColor = Color(0xFFFFEB3B)
            val msgModifier = Modifier.fillMaxWidth()
            val msgAlign = androidx.compose.ui.text.style.TextAlign.Center

            when (currentLevel) {
                0 -> Text(text = if (waterStoredPercent < 100) "タスクを完了させて水を貯めましょう！"
                                 else "水をあげて芽を咲かせましょう！", color = msgColor, fontSize = 13.sp, textAlign = msgAlign)

                1 -> Text(text = if (waterStoredPercent < 100) "水をあげて芽を咲かせましょう！"
                                 else "✨芽が生えてきました！Lv.1！🌱", color = msgColor, fontSize = 13.sp, textAlign = msgAlign)

                2 -> Text(text = if (waterStoredPercent < 100) "✨芽が生えてきました！Lv.1！🌱"
                                 else "✨芽が成長しました！Lv.2！🌱", color = msgColor, fontSize = 13.sp, textAlign = msgAlign)

                3 -> Text(text = if (waterStoredPercent < 100) "✨芽が成長しました！Lv.2！🌱"
                                 else "✨木に成長しました！Lv.3！🌳", color = msgColor, fontSize = 13.sp, textAlign = msgAlign)

                4 -> Text(text = if (waterStoredPercent < 100) "✨木に成長しました！Lv.3！🌳"
                                 else "✨木が成長しました！Lv.4！🌳", color = msgColor, fontSize = 13.sp, textAlign = msgAlign)

                5 -> Text(text = if (waterStoredPercent < 100) "✨木に成長しました！Lv.4！🌳"
                                 else "✨木が成長しました！Lv.5！🌳", color = msgColor, fontSize = 13.sp, textAlign = msgAlign)

                6 -> {
                    //レベル6
                    Text(
                        text = if (showAlternativeImage) "☁️ 上空へ到達！！Lv.6"
                        else "✨木が成長しました！Lv.6！🌳",
                        color = msgColor, fontSize = 13.sp, textAlign = msgAlign
                    )
                }
                7 -> {
                    //レベル7
                    Text(
                        text = if (showAlternativeImage) "✨更に成長しました！Lv.7"
                        else "✨木が成長しました！Lv.7！🌳",
                        color = msgColor, fontSize = 13.sp, textAlign = msgAlign
                    )
                }
            }
        }

        //レベル6、7に到達している間は、いつでも地上と上空を行き来できるボタンを表示
        if (currentLevel >= 6) {
            Button(
                onClick = { showAlternativeImage = !showAlternativeImage },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 32.dp)
                    .size(64.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = if (showAlternativeImage) "地上" else "上空",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}