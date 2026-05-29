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
import com.example.inventory.data.Game // 🌟【追記】さっき作ったGameクラスを使えるようにインポート！

@Composable
fun GameScreen(
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    //ViewModelから最新のゲーム状態（水分量）をリアルタイムで取得
    // 🌟【変更】ここを既存の uiState から、新しく作った gameUiState（RoomDBと直結する箱）に変更したよ！
    // 🌟【理由】元々の uiState はタスク等の別データ用なので、ゲーム専用のデータストリームを監視するためです。
    val gameDataStream by viewModel.gameUiState.collectAsState(initial = null)

    // 🌟【追記】データベースがまだ空っぽ（アプリ初回起動時など）の場合の初期値を用意しておく安心安全ロジック！
    val gameData = gameDataStream ?: Game(waterStoredPercent = 0, currentLevel = 0, givenWaterCount = 0, currentHeightLayer = 0)

    //水分量(テスト時は固定の値を代入する)
    // 🌟【変更】uiState.waterStoredPercent だったのを、上で取得した gameData から取り出す形に変更！
    val waterStoredPercent = gameData.waterStoredPercent
    //val waterStoredPercent = 2400
    //val waterStoredPercent = 1000
    //val waterStoredPercent = 100
    //val waterStoredPercent = 50

    //現在のレベルを管理する状態変数
    // 🌟【変更】「var ... = remember」をコメントアウトして、DBの値（gameData）を直接入れる形に変更！
    // 🌟【理由】rememberのままだと画面に一時保存されるだけでDBに保存されず、アプリを落としたらリセットされてしまうため。
    // 🌟【移動】ここで行っていたタップ時の「計算処理（進化ロジック）」は、すべてViewModelの「waterTree()」関数へ引っ越しました！
    val currentLevel = gameData.currentLevel

    //現在のレベルになってから「追加で水をあげた回数」を記録する
    // 🌟【変更】上のレベルと同じ理由で、rememberからDBの値を直接見に行く形に変更したよ！
    val givenWaterCount = gameData.givenWaterCount

    //高さを5段階で管理する状態変数 (0: 地上, 1: 上空, 2: 成層圏, 3: 外気圏, 4: 宇宙)
    // 🌟【変更】これも同じ理由でrememberを廃止してDB直結に。ボタンを押した時の増減関数はViewModelへ移動しました！
    val currentHeightLayer = gameData.currentHeightLayer

    //現在のレベルに応じて、次の進化に必要な回数を自動で切り替える
    val totalWateringRequired = when (currentLevel) {
        0 -> 3     //Lv.0 -> Lv.1
        1 -> 18    //Lv.1 -> Lv.2
        2 -> 39    //Lv.2 -> Lv.3
        3 -> 52    //Lv.3 -> Lv.4
        4 -> 80    //Lv.4 -> Lv.5
        5 -> 120   //Lv.5 -> Lv.6
        6 -> 160   //Lv.6 -> Lv.7
        7 -> 250   //Lv.7 -> Lv.8
        8 -> 380   //Lv.8 -> Lv.9
        9 -> 470   //Lv.9 -> Lv.10
        10 -> 630  //Lv.10 -> Lv.11
        11 -> 670  //Lv.11 -> Lv.12
        12 -> 852  //Lv.12 -> Lv.13
        13 -> 1000 //Lv.13 -> Lv.14 (Max)
        else -> 0
    }

    //5段階の高さレイヤーに応じた画像切り替えロジック
    val currentImageResId = when (currentHeightLayer) {
        1 -> {//上空
            when (currentLevel) {
                6 -> R.drawable.game_tree_sky_6
                7 -> R.drawable.game_tree_sky_7
                8 -> R.drawable.game_tree_sky_8
                else -> R.drawable.game_tree_sky_9_over
            }
        }
        2 -> {//成層圏
            when (currentLevel) {
                8 -> R.drawable.game_tree_stratosphere_8
                9 -> R.drawable.game_tree_stratosphere_9
                10 -> R.drawable.game_tree_stratosphere_10
                else -> R.drawable.game_tree_stratosphere_11_over
            }
        }
        3 -> {//外気圏
            when (currentLevel) {
                10 -> R.drawable.game_tree_exosphere_10
                11 -> R.drawable.game_tree_exosphere_11
                12 -> R.drawable.game_tree_exosphere_12
                else -> R.drawable.game_tree_exosphere_13_over
            }
        }
        4 -> {//宇宙
            when (currentLevel) {
                12 -> R.drawable.game_tree_earth_12
                13 -> R.drawable.game_tree_earth_13
                else -> R.drawable.game_tree_earth_13
            }
        }
        else -> {//地上
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
                    // 🌟【変更】タップされたら、ViewModelに新しく作った進化関数「waterTree()」を1行呼ぶだけに大スッキリ化！
                    // 🌟【理由】「画面をタップしたからDBの値を計算して書き換えてね」という命令をViewModelに伝えるためです。
                    viewModel.waterTree()

                    /* 元々ここに書いてあったロジックはすべてViewModel側の「waterTree()」に完全移植されました！
                    if (currentLevel < 14 && waterStoredPercent >= 100) {
                        viewModel.updateWaterStoredPercent(waterStoredPercent - 100)
                        givenWaterCount++

                        if (givenWaterCount >= totalWateringRequired) {
                            currentLevel++
                            givenWaterCount = 0

                            //成長レベルに応じた自動ワープ演出（各解放レベルとシンクロ）
                            if (currentLevel == 6) {
                                currentHeightLayer = 1 //レベル6以上で上空追加＆自動移動
                            } else if (currentLevel == 8) {
                                currentHeightLayer = 2 //レベル8以上で成層圏追加＆自動移動
                            } else if (currentLevel == 10) {
                                currentHeightLayer = 3 //レベル10以上で外気圏追加＆自動移動
                            } else if (currentLevel == 12) {
                                currentHeightLayer = 4 //レベル12以上で宇宙追加＆自動移動
                            }
                        }
                    }
                    */
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
                        text = if (currentHeightLayer == 1) "☁️ 上空へ到達！！Lv.6" else "✨木が成長した！Lv.6！🌳",
                        color = msgColor, fontSize = 13.sp, textAlign = msgAlign
                    )
                }
                7 -> {
                    Text(
                        text = if (currentHeightLayer == 1) "☁️ 雲の上をぐんぐん突き進む！Lv.7" else "✨木が更に成長した！Lv.7🌳",
                        color = msgColor, fontSize = 13.sp, textAlign = msgAlign
                    )
                }
                8 -> {
                    val lvl8Msg = when (currentHeightLayer) {
                        2 -> "🚀 成層圏に到達！！！Lv.8"
                        1 -> "☁️ 雲食べれる！Lv.8"
                        else -> "🌳木はまだまだ成長できる！Lv.8"
                    }
                    Text(text = lvl8Msg, color = msgColor, fontSize = 13.sp, textAlign = msgAlign)
                }
                9 -> {
                    val lvl9Msg = when (currentHeightLayer) {
                        2 -> "🌈 対流圏に向けて伸びる！！！Lv.9"
                        1 -> "☁️ 星に手が届きそう！Lv.9"
                        else -> "🌳木はまだまだ成長できる！Lv.9"
                    }
                    Text(text = lvl9Msg, color = msgColor, fontSize = 13.sp, textAlign = msgAlign)
                }
                10 -> {
                    val lvl10Msg = when (currentHeightLayer) {
                        3 -> "⭐ 対流圏に到達！！！！Lv.10"
                        2 -> "🌌 そろそろ息が苦しい！！！Lv.10"
                        1 -> "☁️ 星を見下ろすはるか上空！Lv.10"
                        else -> "🌳🌳木はまだまだ成長できる！Lv.10"
                    }
                    Text(text = lvl10Msg, color = msgColor, fontSize = 13.sp, textAlign = msgAlign)
                }
                11 -> {
                    val lvl11Msg = when (currentHeightLayer) {
                        3 -> "⭐ 星を採集できる！！！！Lv.11"
                        2 -> "🌌 宇宙の果てへ進化完了！！！Lv.11"
                        1 -> "☁️ 星を見下ろすはるか上空！Lv.11"
                        else -> "🌳木はまだまだ成長できる！Lv.11"
                    }
                    Text(text = lvl11Msg, color = msgColor, fontSize = 13.sp, textAlign = msgAlign)
                }
                12 -> {
                    val lvl12Msg = when (currentHeightLayer) {
                        4 -> "🌠 月に届きそう！！！Lv.12"
                        3 -> "⭐ 星を採集できる！！！！Lv.12"
                        2 -> "🚀 宇宙の果てへ進化完了！！！Lv.12"
                        1 -> "☁️ ！Lv.12"
                        else -> "🌳木はまだまだ成長できる！Lv.12"
                    }
                    Text(text = lvl12Msg, color = msgColor, fontSize = 13.sp, textAlign = msgAlign)
                }
                else -> { // 13, 14以上の最大値
                    val lvl13Msg = when (currentHeightLayer) {
                        4 -> "🌕 ついに月に到達した！！！Lv.Max"
                        3 -> "⭐ 星を採集できる！！！！Lv.Max"
                        2 -> "🚀 息が苦しい！！！Lv.Max"
                        1 -> "☁️ 空は青いですね！Lv.Max"
                        else -> "🌳地球を覆いつくす木になった！Max"
                    }
                    Text(text = lvl13Msg, color = msgColor, fontSize = 13.sp, textAlign = msgAlign)
                }
            }
        }

        //階層切り替えボタンエリア（解放レベルに応じて上下2ボタンが動的に追加）
        if (currentLevel >= 6) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                //─── ① 上に行くボタンの表示制御 ───
                //各レベル（6以上、8以上、10以上、12以上）に達している時のみ、対応する上の階層へのボタンを追加表示
                val canGoUp = when (currentHeightLayer) {
                    0 -> currentLevel >= 6  //地上からは、レベル6以上で「上空」が追加
                    1 -> currentLevel >= 8  //上空からは、レベル8以上で「成層圏」が追加
                    2 -> currentLevel >= 10 //成層圏からは、レベル10以上で「外気圏」が追加
                    3 -> currentLevel >= 12 //外気圏からは、レベル12以上で「宇宙」が追加
                    else -> false           //宇宙より上はない
                }

                if (canGoUp) {
                    val upLabel = when (currentHeightLayer) {
                        0 -> "上空"
                        1 -> "成層圏"
                        2 -> "外気圏"
                        3 -> "宇宙"
                        else -> ""
                    }
                    Button(
                        // 🌟【変更】currentHeightLayer++ を、ViewModelの「changeLayer(isUp = true)」に変更！
                        // 🌟【理由】階層ボタンを押した時も、ちゃんとDBの高さデータを書き換えるため。
                        onClick = { viewModel.changeLayer(isUp = true) },
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

                //─── ② 下に行くボタンの表示制御 ───
                //現在地が「地上（0）」より上であれば、いつでも安全に1つ下の階層へ降りられます
                if (currentHeightLayer > 0) {
                    val downLabel = when (currentHeightLayer) {
                        1 -> "地上"
                        2 -> "上空"
                        3 -> "成層圏"
                        4 -> "外気圏"
                        else -> ""
                    }
                    Button(
                        // 🌟【変更】currentHeightLayer-- を、ViewModelの「changeLayer(isUp = false)」に変更！
                        onClick = { viewModel.changeLayer(isUp = false) },
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