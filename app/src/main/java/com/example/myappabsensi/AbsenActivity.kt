package com.example.myappabsensi

import android.Manifest
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.PermissionListener
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class AbsenActivity : AppCompatActivity() {
    private val REQ_CAMERA = 101
    private var strCurrentLatitude = 0.0
    private var strCurrentLongitude = 0.0
    private var strFilePath: String = ""
    private lateinit var imageSelfie: ImageView
    private lateinit var strCurrentLocation: String
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage

    private lateinit var inputNama: EditText
    private lateinit var inputTanggal: EditText
    private lateinit var inputLokasi: EditText
    private lateinit var inputKeterangan: EditText
    private lateinit var btnAbsen: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_absen)
        supportActionBar?.hide()
        setInitLayout()
        setCurrentLocation()
        setUploadData()

        // Initialize Firebase Realtime Database
        database = FirebaseDatabase.getInstance()
        // Initialize Firebase Storage
        storage = FirebaseStorage.getInstance()
    }

    private fun setCurrentLocation() {
        val progressDialog = ProgressDialog(this@AbsenActivity)
        progressDialog.setMessage("Sedang mendapatkan lokasi...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this)
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permissions here if not granted.
            // Handle the permission request response in onRequestPermissionsResult()
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener(this) { location ->
                progressDialog.dismiss()
                if (location != null) {
                    strCurrentLatitude = location.latitude
                    strCurrentLongitude = location.longitude
                    val geocoder = Geocoder(this@AbsenActivity, Locale.getDefault())
                    try {
                        val addressList =
                            geocoder.getFromLocation(strCurrentLatitude, strCurrentLongitude, 1)
                        if (addressList != null && addressList.size > 0) {
                            val address = addressList[0]
                            strCurrentLocation = address.getAddressLine(0)
                            inputLokasi.setText(strCurrentLocation)
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
    }

    private fun setInitLayout() {
        inputNama = findViewById(R.id.inputNama)
        inputTanggal = findViewById(R.id.inputTanggal)
        inputLokasi = findViewById(R.id.inputLokasi)
        inputKeterangan = findViewById(R.id.inputKeterangan)
        btnAbsen = findViewById(R.id.btnAbsen)
        imageSelfie = findViewById(R.id.imageSelfie)

        inputTanggal.isFocusable = false
        inputTanggal.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                DatePickerDialog.OnDateSetListener { _, yearSelected, monthOfYear, dayOfMonth ->
                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(Calendar.YEAR, yearSelected)
                    selectedDate.set(Calendar.MONTH, monthOfYear)
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    inputTanggal.setText(simpleDateFormat.format(selectedDate.time))
                },
                year,
                month,
                day
            )
            datePickerDialog.show()
        }

        imageSelfie = findViewById(R.id.imageSelfie)
        imageSelfie.setOnClickListener {
            requestCameraPermission()
        }
    }

    private fun setUploadData() {
        btnAbsen.setOnClickListener {
            if (inputNama.text.toString().isEmpty()) {
                Toast.makeText(this, "Nama harus diisi", Toast.LENGTH_SHORT).show()
            } else if (inputTanggal.text.toString().isEmpty()) {
                Toast.makeText(this, "Tanggal harus diisi", Toast.LENGTH_SHORT).show()
            } else if (inputLokasi.text.toString().isEmpty()) {
                Toast.makeText(this, "Lokasi harus diisi", Toast.LENGTH_SHORT).show()
            } else {
                val progressDialog = ProgressDialog(this)
                progressDialog.setMessage("Mengunggah data...")
                progressDialog.setCancelable(false)
                progressDialog.show()

                val file = Uri.fromFile(File(strFilePath))
                val storageRef: StorageReference = storage.reference
                val imageRef = storageRef.child("images/" + file.lastPathSegment + "")
                val uploadTask = imageRef.putFile(file)

                uploadTask.addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        progressDialog.dismiss()
                        val databaseRef = database.reference.child("data_absensi")
                        val dataId = databaseRef.push().key
                        val dataAbsensi = dataId?.let { it1 ->
                            AbsenData(
                                it1,
                                inputNama.text.toString(),
                                inputTanggal.text.toString(),
                                inputLokasi.text.toString(),
                                uri.toString(),
                                inputKeterangan.text.toString()
                            )
                        }
                        if (dataId != null) {
                            databaseRef.child(dataId).setValue(dataAbsensi)
                        }
                        Toast.makeText(this, "Data berhasil diunggah", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }.addOnFailureListener {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Gagal mengunggah data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun requestCameraPermission() {
        Dexter.withContext(this)
            .withPermission(Manifest.permission.CAMERA)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                    openCamera()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                    if (response?.isPermanentlyDenied == true) {
                        // Handle permission permanently denied scenario
                        // Show a dialog or navigate to app settings
                    } else {
                        Toast.makeText(
                            this@AbsenActivity,
                            "Izin kamera ditolak",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: com.karumi.dexter.listener.PermissionRequest?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            }).check()
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                null
            }
            photoFile?.let {
                val photoURI: Uri = FileProvider.getUriForFile(
                    this,
                    "com.example.myappabsensi.fileprovider",
                    it
                )
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(intent, REQ_CAMERA)
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir("MyAppAbsensi")
        return File.createTempFile("IMG_${timeStamp}_", ".jpg", storageDir).apply {
            strFilePath = absolutePath
        }
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        )
    }

    private fun getRotationAngle(filePath: String): Float {
        var angle = 0f
        try {
            val exif = ExifInterface(filePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> angle = 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> angle = 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> angle = 270f
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return angle
    }

    private fun setPic() {
        val targetW: Int = imageSelfie.width
        val targetH: Int = imageSelfie.height

        val bmOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(strFilePath, this)
            val photoW: Int = outWidth
            val photoH: Int = outHeight

            val scaleFactor: Int = Math.min(photoW / targetW, photoH / targetH)

            inJustDecodeBounds = false
            inSampleSize = scaleFactor
            inPurgeable = true
        }

        val rotatedBitmap = BitmapFactory.decodeFile(strFilePath, bmOptions)?.let { source ->
            val angle = getRotationAngle(strFilePath)
            rotateImage(source, angle)
        }

        rotatedBitmap?.let {
            imageSelfie.setImageBitmap(it)
            imageSelfie.visibility = View.VISIBLE
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CAMERA && resultCode == RESULT_OK) {
            setPic()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CAMERA) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(
                    this@AbsenActivity,
                    "Izin kamera ditolak",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
