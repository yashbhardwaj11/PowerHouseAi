package com.yash.powerhouseai

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.yash.powerhouseai.databinding.ActivityChooseCountyBinding
import com.yash.powerhouseai.databinding.ActivityMainBinding

class ChooseCounty : AppCompatActivity() {
    private lateinit var binding : ActivityChooseCountyBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChooseCountyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button1.setOnClickListener {
            goToActivity(binding.button1.text.toString())
        }
        binding.button2.setOnClickListener {
            goToActivity(binding.button2.text.toString())
        }
        binding.button3.setOnClickListener {
            goToActivity(binding.button3.text.toString())
        }
        binding.button4.setOnClickListener {
            goToActivity(binding.button4.text.toString())
        }
        binding.button5.setOnClickListener {
            goToActivity(binding.button5.text.toString())
        }
        binding.button6.setOnClickListener {
            goToActivity(binding.button6.text.toString())
        }

    }

    private fun goToActivity(stateName: String) {
        val intent = Intent(this,ShowMoreActivity::class.java)
        intent.putExtra("stateName",stateName)
        startActivity(intent)
    }

}