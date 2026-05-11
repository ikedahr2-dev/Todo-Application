/*
 * Copyright (C) 2023 The Android Open Source Project
 * ... (ライセンス表記は省略可)
 */

package com.example.inventory.ui.home

import androidx.lifecycle.ViewModel
import com.example.inventory.data.Item
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ホーム画面のUI状態を保持するデータクラス
 */
data class HomeUiState(
    val itemList: List<Item> = listOf(),
    val showDatePicker: Boolean = false // カレンダー表示用のフラグ
)

/**
 * Roomデータベースからすべてのアイテムを取得するためのViewModel
 */
class HomeViewModel : ViewModel() {

    // UIの状態を管理するMutableStateFlow
    private val _uiState = MutableStateFlow(HomeUiState())

    // HomeScreenから参照される読み取り専用のStateFlow
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * 追加ボタン（FAB）が押された時にカレンダーを表示する
     */
    fun onAddClick() {
        _uiState.update { it.copy(showDatePicker = true) }
    }

    /**
     * カレンダーを閉じる（OK/キャンセル時）
     */
    fun onDismissDatePicker() {
        _uiState.update { it.copy(showDatePicker = false) }
    }

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}