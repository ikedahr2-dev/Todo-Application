/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.inventory.ui.home

import androidx.lifecycle.ViewModel
import com.example.inventory.data.Item
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel to retrieve all items in the Room database.
 */

data class HomeUiState(
    val itemList: List<Item> = listOf(),
    val showDatePicker: Boolean = false //カレンダー表示中のフラグ
)

class HomeViewModel() : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())

    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onAddClick(){
        _uiState.update { it.copy(showDatePicker = true) }
    } //プラスボタンを押した際のカレンダー呼び出し

    fun onDismissDatePicker() {
        _uiState.update { it.copy(showDatePicker = false) }
    } //カレンダーを閉じる際に呼び出し

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}