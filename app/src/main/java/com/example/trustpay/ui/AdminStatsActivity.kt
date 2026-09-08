package com.example.trustpay.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.trustpay.R

class AdminStatsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this).apply {
            text = "TrustPay SOC - Security Operations & Analytics"
            setTextColor(getColor(R.color.trust_cyan))
            textSize = 18f
            setPadding(32, 32, 32, 32)
            setBackgroundColor(getColor(R.color.primary_navy))
        }
        setContentView(tv)
    }
}
