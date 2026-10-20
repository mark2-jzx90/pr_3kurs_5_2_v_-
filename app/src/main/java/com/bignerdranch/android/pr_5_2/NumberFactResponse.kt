package com.bignerdranch.android.pr_5_2

data class NumberFactResponse(
    val text: String,
    val number: Int,
    val found: Boolean,
    val type: String
)