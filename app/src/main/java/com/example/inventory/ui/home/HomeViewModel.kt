/*
 * Copyright (C) 2023 The Android Open Source Project
 * ... (ライセンス表記は省略可)
 */

package com.example.inventory.ui.home

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.inventory.data.Item
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class HomeUiState(
    val itemList: List<Item> = listOf(),              //アイテムリスト
    val showDatePicker: Boolean = false,              //カレンダー表示フラグ
    val showInputBox: Boolean = false,                //入力ダイアログ表示フラグ
    val savedItems: List<ScheduleItem> = emptyList(), //保存済みの予定
    val editingItem: ScheduleItem? = null             //現在編集中の予定
)

data class ScheduleItem(
    val id: Int,
    val text: String,
    val date: String,
    val time: String
)

class HomeViewModel : ViewModel() {

    //UIの状態を管理するMutableStateFlow
    private val _uiState = MutableStateFlow(HomeUiState())

    //HomeScreenから参照される読み取り専用のStateFlow
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var _editingItem = mutableStateOf<ScheduleItem?>(null)
    val editingItem: State<ScheduleItem?> = _editingItem

    //新しい予定ID用のカウンター
    private var nextId = 0

    //カレンダーを開く
    fun onCalenderClick() {
        _uiState.update { it.copy(showDatePicker = true) }
    }

    //新規追加用ダイアログを開く
    fun onAddClick() {
        _editingItem.value = null          //編集中アイテムはクリア
        _uiState.update { it.copy(showInputBox = true) }
    }

    //入力ダイアログを閉じる
    fun onDismissInputBox() {
        _editingItem.value = null
        _uiState.update { it.copy(showInputBox = false) }
    }

    //カレンダーを閉じる
    fun onDismissDatePicker() {
        _uiState.update { it.copy(showDatePicker = false) }
    }

    //新規追加
    fun addText(text: String, date: String, time: String) {
        val newItem = ScheduleItem(id = nextId++, text = text, date = date, time = time)
        _uiState.update { state ->
            state.copy(
                savedItems = state.savedItems + newItem,
                showInputBox = false      //追加後はダイアログを閉じる
            )
        }
    }

    //編集・削除
    //保存済みアイテムをクリック→編集対象にセットし、ダイアログ表示するとこ
    fun onEditSavedItem(item: ScheduleItem) {
        _editingItem.value = item
        _uiState.update { it.copy(showInputBox = true) }
    }

    //編集した予定を保存
    fun updateItem(item: ScheduleItem, newText: String, newDate: String, newTime: String) {
        _uiState.update { state ->
            state.copy(
                savedItems = state.savedItems.map {
                    if (it.id == item.id) it.copy(text = newText, date = newDate, time = newTime)
                    else it
                },
                showInputBox = false      //保存後はダイアログを閉じる
            )
        }
        _editingItem.value = null
    }

    //保存済みの予定を削除
    fun deleteItem(item: ScheduleItem) {
        _uiState.update { state ->
            state.copy(
                savedItems = state.savedItems.filter { it.id != item.id },
                showInputBox = false      // 削除後はダイアログを閉じる
            )
        }
        _editingItem.value = null
    }

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}