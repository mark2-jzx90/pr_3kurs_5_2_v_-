package com.bignerdranch.android.pr_5_2

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "facts_table")
data class FactEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var number: String,
    var factText: String
)