package com.droidjax.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.droidjax.floatinghelper.FloatingHelperActivity

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(32, 32, 32, 32)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )

                addView(
                    TextView(context).apply {
                        text = "DroidJax"
                        gravity = Gravity.CENTER
                        textSize = 24f
                    },
                )
                addView(
                    Button(context).apply {
                        text = "Open Helper"
                        isAllCaps = false
                        setOnClickListener {
                            startActivity(Intent(context, FloatingHelperActivity::class.java))
                        }
                    },
                )
            },
        )
    }
}
