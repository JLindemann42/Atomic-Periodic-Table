package com.jlindemann.science.activities.tables

import android.content.Context
import android.content.res.Configuration
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jlindemann.science.R
import com.jlindemann.science.activities.BaseActivity
import com.jlindemann.science.adapter.ElectrodeAdapter
import com.jlindemann.science.adapter.EquationsAdapter
import com.jlindemann.science.adapter.GeologyAdapter
import com.jlindemann.science.adapter.PoissonAdapter
import com.jlindemann.science.animations.Anim
import com.jlindemann.science.model.Dictionary
import com.jlindemann.science.model.DictionaryModel
import com.jlindemann.science.model.Equation
import com.jlindemann.science.model.Geology
import com.jlindemann.science.model.GeologyModel
import com.jlindemann.science.model.Poisson
import com.jlindemann.science.model.PoissonModel
import com.jlindemann.science.preferences.DictionaryPreferences
import com.jlindemann.science.preferences.PoissonPreferences
import com.jlindemann.science.preferences.ProVersion
import com.jlindemann.science.preferences.ThemePreference
import com.jlindemann.science.utils.ToastUtil
import com.jlindemann.science.utils.Utils
import java.util.*
import kotlin.collections.ArrayList


class GeologyActivity : BaseActivity(), GeologyAdapter.OnGeologyClickListener {
    private var geologyList = ArrayList<Geology>()
    var mAdapter = GeologyAdapter(geologyList, this, this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themePreference = ThemePreference(this)
        val themePrefValue = themePreference.getValue()
        if (themePrefValue == 100) {
            when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_NO -> { setTheme(R.style.AppTheme) }
                Configuration.UI_MODE_NIGHT_YES -> { setTheme(R.style.AppThemeDark) }
            }
        }
        if (themePrefValue == 0) { setTheme(R.style.AppTheme) }
        if (themePrefValue == 1) { setTheme(R.style.AppThemeDark) }
        setContentView(R.layout.activity_geology) //REMEMBER: Never move any function calls above this

        val recyclerView = findViewById<RecyclerView>(R.id.geo_view)
        recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        val item = ArrayList<Geology>()
        GeologyModel.getList(item)

        recyclerView()
        clickSearch()
        chipListeners(item, recyclerView)
        findViewById<Button>(R.id.clear_btn).visibility = View.GONE
        findViewById<FrameLayout>(R.id.geo_det_inc_background).setOnClickListener { hideInfoPanel() }
        findViewById<ImageButton>(R.id.close_detail_poisson_btn).setOnClickListener { hideInfoPanel() }

        findViewById<FrameLayout>(R.id.view_geo).systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        findViewById<ImageButton>(R.id.back_btn_geo).setOnClickListener {
            this.onBackPressed()
        }
    }

    override fun onApplySystemInsets(top: Int, bottom: Int, left: Int, right: Int) {
        findViewById<RecyclerView>(R.id.geo_view).setPadding(0, resources.getDimensionPixelSize(R.dimen.title_bar_ph) + top, 0, resources.getDimensionPixelSize(R.dimen.title_bar_ph))
        val params2 = findViewById<FrameLayout>(R.id.common_title_back_geo).layoutParams as ViewGroup.LayoutParams
        params2.height = top + resources.getDimensionPixelSize(R.dimen.title_bar_ph)
        findViewById<FrameLayout>(R.id.common_title_back_geo).layoutParams = params2

        val searchEmptyImgPrm = findViewById<LinearLayout>(R.id.empty_search_box_geo).layoutParams as ViewGroup.MarginLayoutParams
        searchEmptyImgPrm.topMargin = top + (resources.getDimensionPixelSize(R.dimen.title_bar))
        findViewById<LinearLayout>(R.id.empty_search_box_geo).layoutParams = searchEmptyImgPrm
    }

    private fun recyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.geo_view)
        val geology = ArrayList<Geology>()

        GeologyModel.getList(geology)
        recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        val adapter = GeologyAdapter(geology, this, this)
        recyclerView.adapter = adapter

        adapter.notifyDataSetChanged()

        findViewById<EditText>(R.id.edit_geo).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int){}
            override fun afterTextChanged(s: Editable) {
                filter(s.toString(), geology, recyclerView)
            }
        })
    }

    //Overrides the clickListener from GeologyAdapter to show InfoPanel when clicking on elements
    override fun geologyClickListener(item: Geology, position: Int) {
        showInfoPanel(item.name, item.start, item.end, item.type)
    }

    //Show the info panel with detailed information about poisson interavls for materials
    private fun showInfoPanel(title: String, start: Double, end: Double, type: String) {
        Anim.fadeIn(findViewById<ConstraintLayout>(R.id.geo_det_inc), 150)
        findViewById<FrameLayout>(R.id.geo_det_inc_background).visibility = View.VISIBLE

        findViewById<ProgressBar>(R.id.pb_poisson_detail).progress = (start*100*2).toInt() //*2 as 100% is 0.5
        findViewById<ProgressBar>(R.id.pb_poisson_detail).secondaryProgress = (end*100*2).toInt() //*2 as 100% is 0.5
        findViewById<TextView>(R.id.detail_poisson_title).text = title
    }

    //function for hiding info panel
    private fun hideInfoPanel() {
        Anim.fadeOutAnim(findViewById<ConstraintLayout>(R.id.geo_det_inc), 150)
        findViewById<FrameLayout>(R.id.geo_det_inc_background).visibility = View.GONE
    }

    //Filters the listView by different sorts of material by using the PoissonPreference to filter by the stringValue.
    private fun filter(text: String, list: ArrayList<Geology>, recyclerView: RecyclerView) {
        val filteredList: ArrayList<Geology> = ArrayList()
        for (item in list) {
            val poissonPreference = PoissonPreferences(this)
            val poissonPrefValue = poissonPreference.getValue()
            if (item.name.toLowerCase(Locale.ROOT).contains(text.toLowerCase(Locale.ROOT))) {
                if (item.type.toLowerCase(Locale.ROOT).contains(poissonPrefValue.toLowerCase(Locale.ROOT))) {
                    filteredList.add(item)
                }
            }
        }
        val handler = android.os.Handler()
        handler.postDelayed({
            if (recyclerView.adapter!!.itemCount == 0) {
                Anim.fadeIn(findViewById<LinearLayout>(R.id.empty_search_box_poi), 300)
            }
            else {
                findViewById<LinearLayout>(R.id.empty_search_box_poi).visibility = View.GONE
            }
        }, 10)
        mAdapter.filterList(filteredList)
        mAdapter.notifyDataSetChanged()
        recyclerView.adapter = GeologyAdapter(filteredList, this, this)
    }

    private fun clickSearch() {
        findViewById<ImageButton>(R.id.search_btn_poi).setOnClickListener {
            Utils.fadeInAnim(findViewById<FrameLayout>(R.id.search_bar_poi), 150)
            Utils.fadeOutAnim(findViewById<FrameLayout>(R.id.title_box_poi), 1)

            findViewById<EditText>(R.id.edit_poi).requestFocus()
            val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(findViewById<EditText>(R.id.edit_poi), InputMethodManager.SHOW_IMPLICIT)
        }
        findViewById<ImageButton>(R.id.close_poi_search).setOnClickListener {
            Utils.fadeOutAnim(findViewById<FrameLayout>(R.id.search_bar_poi), 1)

            val delayClose = Handler()
            delayClose.postDelayed({
                Utils.fadeInAnim(findViewById<FrameLayout>(R.id.title_box_poi), 150)
            }, 151)

            val view = this.currentFocus
            if (view != null) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }
        }
    }

    private fun chipListeners(list: ArrayList<Geology>, recyclerView: RecyclerView) {
        findViewById<Button>(R.id.rocks_btn).setOnClickListener {
            updateButtonColor("rocks_btn")
            val poissonPreference = PoissonPreferences(this)
            poissonPreference.setValue("rock")
            findViewById<EditText>(R.id.edit_poi).setText("test")
            findViewById<EditText>(R.id.edit_poi).setText("")
        }
        findViewById<Button>(R.id.soils_btn).setOnClickListener {
            updateButtonColor("soils_btn")
            val poissonPreference = PoissonPreferences(this)
            poissonPreference.setValue("soil")
            findViewById<EditText>(R.id.edit_poi).setText("test")
            findViewById<EditText>(R.id.edit_poi).setText("")
        }
        findViewById<Button>(R.id.minerals_btn).setOnClickListener {
            updateButtonColor("minerals_btn")
            val poissonPreference = PoissonPreferences(this)
            poissonPreference.setValue("mineral")
            findViewById<EditText>(R.id.edit_poi).setText("test")
            findViewById<EditText>(R.id.edit_poi).setText("")
        }
    }

    private fun updateButtonColor(btn: String) {
        findViewById<Button>(R.id.rocks_btn).background = getDrawable(R.drawable.chip)
        findViewById<Button>(R.id.soils_btn).background = getDrawable(R.drawable.chip)
        findViewById<Button>(R.id.minerals_btn).background = getDrawable(R.drawable.chip)

        val delay = Handler()
        delay.postDelayed({
            val resIDB = resources.getIdentifier(btn, "id", packageName)
            val button = findViewById<Button>(resIDB)
            button.background = getDrawable(R.drawable.chip_active)
        }, 200)

        findViewById<Button>(R.id.clear_btn).visibility = View.VISIBLE
        findViewById<Button>(R.id.clear_btn).setOnClickListener {
            val resIDB = resources.getIdentifier(btn, "id", packageName)
            val button = findViewById<Button>(resIDB)
            val poissonPreference = PoissonPreferences(this)
            button.background = getDrawable(R.drawable.chip)
            poissonPreference.setValue("")
            findViewById<EditText>(R.id.edit_poi).setText("test")
            findViewById<EditText>(R.id.edit_poi).setText("")
            findViewById<Button>(R.id.clear_btn).visibility = View.GONE
        }
    }

    //handle back action
    override fun onBackPressed() {
        if (findViewById<CardView>(R.id.poi_det_inc).visibility == View.VISIBLE) {
            hideInfoPanel()
            return
        } else { super.onBackPressed() }
    }
}



