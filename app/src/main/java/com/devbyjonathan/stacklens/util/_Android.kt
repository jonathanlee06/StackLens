package com.devbyjonathan.stacklens.util

import android.os.Build

fun isAtLeastAndroid8() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
fun isAtLeastAndroid12() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
fun isAtLeastAndroid13() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
fun isAtLeastAndroid15() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM