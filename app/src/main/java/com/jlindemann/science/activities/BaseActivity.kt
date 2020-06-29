package com.jlindemann.science.activities

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.jlindemann.science.R
import com.jlindemann.science.utils.Pasteur

abstract class BaseActivity : AppCompatActivity(), View.OnApplyWindowInsetsListener {
    companion object {
        private const val TAG = "BaseActivity"
    }

    private var systemUiConfigured = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.TRANSPARENT
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    }

    override fun onStart() {
        super.onStart()

        val content = findViewById<View>(android.R.id.content)
        content.setOnApplyWindowInsetsListener(this)

        if (!systemUiConfigured) {
            systemUiConfigured = true
        }
    }

    open fun onApplySystemInsets(top: Int, bottom: Int) = Unit

    override fun onApplyWindowInsets(v: View, insets: WindowInsets): WindowInsets {
        Pasteur.info(TAG, "height: ${insets.systemWindowInsetBottom}")
        onApplySystemInsets(insets.systemWindowInsetTop, insets.systemWindowInsetBottom)
        return insets.consumeSystemWindowInsets()
    }
}