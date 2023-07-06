package com.example.myyappabsensi.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.myappabsensi.AbsenData
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class AbsenViewModel(application: Application) : AndroidViewModel(application) {
    private val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().reference.child("userData")

    fun addDataAbsen(
        foto: String, nama: String,
        tanggal: String, lokasi: String, keterangan: String
    ) {
        val absenData = AbsenData(foto, nama, tanggal, lokasi, keterangan)
        val newAbsenRef = databaseReference.child("absensi").push()
        newAbsenRef.setValue(absenData)
            .addOnSuccessListener {
                // Berhasil menambahkan data absen ke Firebase Realtime Database
            }
            .addOnFailureListener { e ->
                // Gagal menambahkan data absen ke Firebase Realtime Database
            }
    }
}
