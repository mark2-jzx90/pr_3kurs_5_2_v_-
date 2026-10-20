package com.bignerdranch.android.pr_5_2

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {


    private lateinit var screenAuth: LinearLayout
    private lateinit var screenMain: LinearLayout
    private lateinit var screenHistory: LinearLayout


    private lateinit var etNameInput: EditText
    private lateinit var tvUserHello: TextView
    private lateinit var etNumber: EditText
    private lateinit var tvFactOutput: TextView
    private lateinit var btnSaveFact: Button
    private lateinit var lvSavedItems: ListView


    private var loadedNumber = ""
    private var loadedFact = ""
    private lateinit var roomDb: AppDatabase
    private var factsList = ArrayList<FactEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        roomDb = AppDatabase.getDatabase(this)


        screenAuth = findViewById(R.id.screenAuth)
        screenMain = findViewById(R.id.screenMain)
        screenHistory = findViewById(R.id.screenHistory)
        etNameInput = findViewById(R.id.etNameInput)
        tvUserHello = findViewById(R.id.tvUserHello)
        etNumber = findViewById(R.id.etNumber)
        tvFactOutput = findViewById(R.id.tvFactOutput)
        btnSaveFact = findViewById(R.id.btnSaveFact)
        lvSavedItems = findViewById(R.id.lvSavedItems)


        val prefs = getSharedPreferences("StudentPrefs", Context.MODE_PRIVATE)
        val savedName = prefs.getString("username", "")

        if (!savedName.isNullOrEmpty()) {
            openMainScreen(savedName)
        }


        findViewById<Button>(R.id.btnSignIn).setOnClickListener {
            val name = etNameInput.text.toString().trim()
            if (name.isEmpty()) {
                etNameInput.error = "Имя не может быть пустым!"
            } else {
                prefs.edit().putString("username", name).apply()
                openMainScreen(name)
            }
        }


        findViewById<Button>(R.id.btnSearchFact).setOnClickListener {
            val num = etNumber.text.toString().trim()
            if (num.isEmpty()) {
                etNumber.error = "Введите число!"
            } else {
                sendApiRequest(num)
            }
        }


        btnSaveFact.setOnClickListener {
            if (loadedFact.isNotEmpty()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val newFact = FactEntity(number = loadedNumber, factText = loadedFact)
                    roomDb.factDao().insertFact(newFact)


                    lifecycleScope.launch(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Сохранено в базу ROOM!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }


        findViewById<Button>(R.id.btnGoToHistory).setOnClickListener {
            screenMain.visibility = View.GONE
            screenHistory.visibility = View.VISIBLE
            updateListViewFromRoom()
        }


        findViewById<Button>(R.id.btnBack).setOnClickListener {
            screenHistory.visibility = View.GONE
            screenMain.visibility = View.VISIBLE
        }


        lvSavedItems.setOnItemLongClickListener { _, _, position, _ ->
            val clickedFact = factsList[position]

            val dialog = AlertDialog.Builder(this)
            dialog.setTitle("Управление записью Room")

            val inputEdit = EditText(this)
            inputEdit.setText(clickedFact.factText)
            dialog.setView(inputEdit)

            dialog.setPositiveButton("Изменить") { _, _ ->
                val newText = inputEdit.text.toString().trim()
                if (newText.isNotEmpty()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        clickedFact.factText = newText
                        roomDb.factDao().updateFact(clickedFact) // Обновление в БД
                        updateListViewFromRoom()
                    }
                }
            }

            dialog.setNegativeButton("Удалить") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    roomDb.factDao().deleteFact(clickedFact) // Удаление из БД
                    updateListViewFromRoom()

                    lifecycleScope.launch(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Запись удалена!", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            dialog.setNeutralButton("Отмена", null)
            dialog.show()
            true
        }
    }

    private fun openMainScreen(name: String) {
        screenAuth.visibility = View.GONE
        screenMain.visibility = View.VISIBLE
        tvUserHello.text = "Привет, Студент: $name!"


        val prefs = getSharedPreferences("StudentPrefs", Context.MODE_PRIVATE)
        val jsonString = prefs.getString("last_json_data", "")
        if (!jsonString.isNullOrEmpty()) {
            try {
                val factResponse = Gson().fromJson(jsonString, NumberFactResponse::class.java)
                loadedNumber = factResponse.number.toString()
                loadedFact = factResponse.text
                tvFactOutput.text = "Последний кэш (из SharedPrefs через Gson):\n$loadedFact"
                btnSaveFact.visibility = View.VISIBLE
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun sendApiRequest(number: String) {
        val url = "http://numbersapi.com"
        val requestQueue = Volley.newRequestQueue(this)

        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->

                val prefs = getSharedPreferences("StudentPrefs", Context.MODE_PRIVATE)
                prefs.edit().putString("last_json_data", response).apply()


                val factResponse = Gson().fromJson(response, NumberFactResponse::class.java)
                loadedNumber = factResponse.number.toString()
                loadedFact = factResponse.text

                tvFactOutput.text = "Факт о числе $loadedNumber:\n$loadedFact"
                btnSaveFact.visibility = View.VISIBLE
            },
            {
                Toast.makeText(this, "Ошибка запроса API!", Toast.LENGTH_SHORT).show()
            })
        requestQueue.add(stringRequest)
    }


    private fun updateListViewFromRoom() {
        lifecycleScope.launch(Dispatchers.IO) {
            val listFromDb = roomDb.factDao().getAllFacts()


            lifecycleScope.launch(Dispatchers.Main) {
                factsList.clear()
                factsList.addAll(listFromDb)

                val textData = ArrayList<String>()
                for (item in listFromDb) {
                    textData.add("Число ${item.number}: ${item.factText}")
                }

                val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, textData)
                lvSavedItems.adapter = adapter
            }
        }
    }
}