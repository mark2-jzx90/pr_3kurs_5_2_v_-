package com.bignerdranch.android.pr_5_2

import androidx.room.*
@Dao
interface FactDao {
    @Query("SELECT * FROM facts_table")
    fun getAllFacts(): List<FactEntity> // БЕЗ suspend, возвращает обычный List

    @Insert
    fun insertFact(fact: FactEntity) // БЕЗ suspend

    @Update
    fun updateFact(fact: FactEntity) // БЕЗ suspend

    @Delete
    fun deleteFact(fact: FactEntity) // БЕЗ suspend
}