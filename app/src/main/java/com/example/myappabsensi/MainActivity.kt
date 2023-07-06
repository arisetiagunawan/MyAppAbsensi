package com.example.myappabsensi

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.myappabsensi.databinding.ActivityMainBinding
import com.example.myappabsensi.utils.ui_user.ProfileFragment
import com.example.myappabsensi.utils.ui_user.InfoFragment
import com.example.myappabsensi.utils.ui_user.LaporanFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    val fragmentAbsen: Fragment = ProfileFragment()
    val fragmentLaporan: Fragment = LaporanFragment()
    val fragmentInfo: Fragment = InfoFragment()
    val fm: FragmentManager = supportFragmentManager
    var active: Fragment = fragmentAbsen

    lateinit var binding: ActivityMainBinding

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var menu: Menu
    private lateinit var menuItem: MenuItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        supportActionBar?.hide()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        buttonNavigation()
    }

    private fun buttonNavigation() {
        fm.beginTransaction().add(R.id.container, fragmentAbsen).show(fragmentAbsen).commit()
        fm.beginTransaction().add(R.id.container, fragmentLaporan).hide(fragmentLaporan).commit()
        fm.beginTransaction().add(R.id.container, fragmentInfo).hide(fragmentInfo).commit()

        bottomNavigationView = binding.bottomview
        menu = bottomNavigationView.menu
        menuItem = menu.getItem(0)
        menuItem.isChecked = true

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Absen -> {
                    callFrag(0, fragmentAbsen)
                }
                R.id.Laporan -> {
                    callFrag(1, fragmentLaporan)
                }
                R.id.Info -> {
                    callFrag(2, fragmentInfo)
                }
            }
            false
        }
    }

    private fun callFrag(index: Int, fragment: Fragment) {
        menuItem = menu.getItem(index)
        menuItem.isChecked = true
        fm.beginTransaction().hide(active).show(fragment).commit()
        active = fragment
    }
}
