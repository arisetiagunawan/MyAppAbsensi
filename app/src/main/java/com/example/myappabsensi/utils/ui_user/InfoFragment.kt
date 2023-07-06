package com.example.myappabsensi.utils.ui_user

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.example.myappabsensi.R

class InfoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tentang, container, false)

        val closeButton: Button = view.findViewById(R.id.btnClose)
        closeButton.setOnClickListener {
            activity?.finish()
        }

        return view
    }
}
