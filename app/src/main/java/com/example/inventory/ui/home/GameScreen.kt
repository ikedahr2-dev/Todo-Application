package com.example.inventory.ui.home

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
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
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val waterStoredPercent = uiState.waterStoredPercent
    var currentLevel by remember { mutableStateOf(0) }
    var givenWaterCount by remember { mutableStateOf(0) }
    var currentHeightLayer by remember { mutableStateOf(0) }

    //放置時間を管理するための共通SharedPreferences
    val gamePrefs = remember { context.getSharedPreferences("game_time_prefs", Context.MODE_PRIVATE) }

    //最後に水をあげた時間をミリ秒で管理（データがなければ今の時間を初期値にする）
    var lastWateredTimeMillis by remember {
        mutableStateOf(gamePrefs.getLong("last_watered_time_key", System.currentTimeMillis()))
    }

    //3日（72時間）をミリ秒に換算 ➔ 72時間 × 60分 × 60秒 × 1000ミリ秒
    //val threeDaysInMillis = 72L * 60 * 60 * 1000

    //テスト用ショートカット（もし5分放置でDeathさせたい場合は、下の行のコメントアウトを外してください）
    val threeDaysInMillis = 5L * 60 * 1000

    //「枯れているかどうか」をリアルタイム計算するフラグ
    //Lv.1以上の状態で、最後に水をあげてから3日（72時間）以上経過している
    val isDead = currentLevel >= 1 && (System.currentTimeMillis() - lastWateredTimeMillis >= threeDaysInMillis)

    val totalWateringRequired = when (currentLevel) {
        //テスト用
        0 -> 1  //Lv.0 -> Lv.1
        1 -> 1  //Lv.1 -> Lv.2
        2 -> 1  //Lv.2 -> Lv.3
        3 -> 1  //Lv.3 -> Lv.4
        4 -> 1  //Lv.4 -> Lv.5
        5 -> 1  //Lv.5 -> Lv.6
        6 -> 1  //Lv.6 -> Lv.7
        7 -> 1  //Lv.7 -> Lv.8
        8 -> 1  //Lv.8 -> Lv.9
        9 -> 1  //Lv.9 -> Lv.10
        10 -> 1 //Lv.10 -> Lv.11
        11 -> 1 //Lv.11 -> Lv.12
        12 -> 1 //Lv.12 -> Lv.13
        13 -> 1 //Lv.13 -> Lv.14 (Max)
        //本番用
        /*0 -> 3 //Lv.0 -> Lv.1
        1 -> 18 //Lv.1 -> Lv.2
        2 -> 39 //Lv.2 -> Lv.3
        3 -> 52 //Lv.3 -> Lv.4
        4 -> 80 //Lv.4 -> Lv.5
        5 -> 120 //Lv.5 -> Lv.6
        6 -> 160 //Lv.6 -> Lv.7
        7 -> 250 //Lv.7 -> Lv.8
        8 -> 380 //Lv.8 -> Lv.9
        9 -> 470 //Lv.9 -> Lv.10
        10 -> 630 //Lv.10 -> Lv.11
        11 -> 670 //Lv.11 -> Lv.12
        12 -> 852 //Lv.12 -> Lv.13
        13 -> 1000 //Lv.13 -> Lv.14 (Max)*/
        else -> 0
    }

    //もし枯れている（isDead == true）なら、すべての階層を無視して即「Death画像」を最優先で表示する
    val currentImageResId = if (isDead) {
        R.drawable.game_tree_death_1 //追加：枯れてしまった時のDeath画像リソース
    } else {
        when (currentHeightLayer) {
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
                    8 -> R.drawable.game_tree_stratosphere_8
                    9 -> R.drawable.game_tree_stratosphere_9
                    10 -> R.drawable.game_tree_stratosphere_10
                    else -> R.drawable.game_tree_stratosphere_11_over
                }
            }
            3 -> {
                when (currentLevel) {
                    10 -> R.drawable.game_tree_exosphere_10
                    11 -> R.drawable.game_tree_exosphere_11
                    12 -> R.drawable.game_tree_exosphere_12
                    else -> R.drawable.game_tree_exosphere_13_over
                }
            }
            4 -> {
                when (currentLevel) {
                    12 -> R.drawable.game_tree_earth_12
                    13 -> R.drawable.game_tree_earth_13
                    else -> R.drawable.game_tree_earth_13
                }
            }
            else -> {
                when (currentLevel) {
                    0 -> R.drawable.game_tree_lv_0
                    1 -> R.drawable.game_tree_lv_1
                    2 -> R.drawable.game_tree_lv_2
                    3 -> R.drawable.game_tree_lv_3
                    4 -> R.drawable.game_tree_lv_4
                    5 -> R.drawable.game_tree_lv_5
                    6 -> R.drawable.game_tree_lv_6
                    else -> R.drawable.game_tree_lv_7_over
                }
            }
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = (waterStoredPercent / 2400f).coerceIn(0f, 10f),
        label = "WaterGaugeAnimation"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = currentImageResId),
            contentDescription = "ゲーム背景",
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    //枯れていない（isDead == false）ときだけ水をあげられるようにガード
                    if (!isDead && currentLevel < 14 && waterStoredPercent >= 100) {
                        viewModel.updateWaterStoredPercent(waterStoredPercent - 100)
                        givenWaterCount++

                        //水やりが成功したため、「最後に水をあげた時間」を現在時刻に更新してスマホに即保存
                        val now = System.currentTimeMillis()
                        lastWateredTimeMillis = now
                        gamePrefs.edit().putLong("last_watered_time_key", now).apply()

                        if (givenWaterCount >= totalWateringRequired) {
                            currentLevel++
                            givenWaterCount = 0

                            if (currentLevel == 6) {
                                currentHeightLayer = 1
                            } else if (currentLevel == 8) {
                                currentHeightLayer = 2
                            } else if (currentLevel == 10) {
                                currentHeightLayer = 3
                            } else if (currentLevel == 12) {
                                currentHeightLayer = 4
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

            //もし枯れてしまった場合の専用メッセージ表示
            if (isDead) {
                Text(
                    text = "3日間水を与えなかったため、木が枯れてしまった。",
                    color = Color.Red,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = msgAlign
                )
            } else {
                // 通常のレベルアップメッセージ分岐（既存のまま）
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
                    6 -> Text(text = if (currentHeightLayer == 1) "☁️ 上空へ到達！！Lv.6"
                    else "✨木が成長した！Lv.6！🌳", color = msgColor, fontSize = 13.sp, textAlign = msgAlign)
                    7 -> Text(text = if (currentHeightLayer == 1) "☁️ 雲の上をぐんぐん突き進む！Lv.7"
                    else "✨木が更に成長した！Lv.7🌳", color = msgColor, fontSize = 13.sp, textAlign = msgAlign)
                    8 -> Text(text = when (currentHeightLayer) { 2 -> "🚀 成層圏に到達！！！Lv.8" 1 -> "☁️ 雲食べれる！Lv.8"
                        else -> "🌳木はまだまだ成長できる！Lv.8" }, color = msgColor, fontSize = 13.sp, textAlign = msgAlign)
                    9 -> Text(text = when (currentHeightLayer) { 2 -> "🌈 対流圏に向けて伸びる！！！Lv.9" 1 -> "☁️ 星に手が届きそう！Lv.9"
                        else -> "🌳木はまだまだ成長できる！Lv.9" }, color = msgColor, fontSize = 13.sp, textAlign = msgAlign)
                    10 -> Text(text = when (currentHeightLayer) { 3 -> "⭐ 対流圏に到達！！！！Lv.10" 2 -> "🌌 そろそろ息が苦しい！！！Lv.10" 1 -> "☁️ 星を見下ろすはるか上空！Lv.10"
                        else -> "🌳🌳木はまだまだ成長できる！Lv.10" }, color = msgColor, fontSize = 13.sp, textAlign = msgAlign)
                    11 -> Text(text = when (currentHeightLayer) { 3 -> "⭐ 星を採集できる！！！！Lv.11" 2 -> "🌌 宇宙の果てへ進化完了！！！Lv.11" 1 -> "☁️ 星を見下ろすはるか上空！Lv.11"
                        else -> "🌳木はまだまだ成長できる！Lv.11" }, color = msgColor, fontSize = 13.sp, textAlign = msgAlign)
                    12 -> Text(text = when (currentHeightLayer) { 4 -> "🌠 月に届きそう！！！Lv.12" 3 -> "⭐ 星を採集できる！！！！Lv.12" 2 -> "🚀 宇宙の果てへ進化完了！！！Lv.11" 1 -> "☁️ ！Lv.12"
                        else -> "🌳木はまだまだ成長できる！Lv.12" }, color = msgColor, fontSize = 13.sp, textAlign = msgAlign)
                    else -> Text(text = when (currentHeightLayer) { 4 -> "🌕 ついに月に到達した！！！Lv.Max" 3 -> "⭐ 星を採集できる！！！！Lv.Max" 2 -> "🚀 息が苦しい！！！Lv.Max" 1 -> "☁️ 空は青いですね！Lv.Max"
                        else -> "🌳地球を覆いつくす木になった！Max" }, color = msgColor, fontSize = 13.sp, textAlign = msgAlign)
                }
            }
        }

        // 階層切り替えボタンエリア,変更：枯れた時は「復活ボタン」にすり替える
        if (isDead) {
            //枯れた時専用：ペナルティとしてリセットして復活させるボタン
            Button(
                onClick = {
                    currentLevel = 0
                    givenWaterCount = 0
                    currentHeightLayer = 0
                    //タイムスタンプを今にリセットして復活
                    val now = System.currentTimeMillis()
                    lastWateredTimeMillis = now
                    gamePrefs.edit().putLong("last_watered_time_key", now).apply()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 32.dp)
                    .size(80.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(text = "復活", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        } else if (currentLevel >= 6) {
            //通常の上下ボタン群（既存のまま）
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val canGoUp = when (currentHeightLayer) {
                    0 -> currentLevel >= 6
                    1 -> currentLevel >= 8
                    2 -> currentLevel >= 10
                    3 -> currentLevel >= 12
                    4 -> currentLevel >= 13
                    else -> false
                }

                if (canGoUp) {
                    val upLabel = when (currentHeightLayer) {
                        0 -> "上空"
                        1 -> "成層圏"
                        2 -> "外気圏"
                        3 -> "宇宙"
                        4 -> "リセット"
                        else -> ""
                    }
                    Button(
                        onClick = {
                            if (currentHeightLayer == 4) {
                                currentLevel = 0
                                givenWaterCount = 0
                                currentHeightLayer = 0
                            } else {
                                currentHeightLayer++
                            }
                        },
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(text = upLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (currentHeightLayer > 0) {
                    val downLabel = when (currentHeightLayer) {
                        1 -> "地上"
                        2 -> "上空"
                        3 -> "成層圏"
                        4 -> "外気圏"
                        else -> ""
                    }
                    Button(
                        onClick = { currentHeightLayer-- },
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(text = downLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}