package com.clevmania.appruvecam

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import androidx.lifecycle.Observer
import android.provider.MediaStore
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.snackbar.Snackbar
import com.yalantis.ucrop.UCrop
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private var cameraUri: Uri? = null
    private val cameraRequestCode = 1001
    private val storageRequestCode = 1002
    private lateinit var currentPhotoPath: String
    private var croppedImgPath: String? = null

    private lateinit var viewModel : MainActivityViewModel
    private lateinit var viewModelFactory: ViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /**
         * This shouldn't be so, given enough time,
         * i'd have used dependency injection
         * */
        val apiService = AppruveApiService()
        val dataSrc = DocumentRepository(apiService)

        viewModelFactory = ViewModelFactory(dataSrc)
        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(MainActivityViewModel::class.java)

        updateChanges(viewModel)

        ivChooseDocument.setOnClickListener { checkStoragePermission() }
        tvChooseDocument.setOnClickListener { checkStoragePermission() }

        captureAndUploadSelfIeImage()
    }

    private fun captureAndUploadSelfIeImage() {
        btnContinue.setOnClickListener {
            if (croppedImgPath == null) {
                Snackbar.make(cvRootLayout, "Take a passport photograph", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            cameraUri?.let {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(
                            contentResolver,
                            it
                        )
                    )
                } else {
                    MediaStore.Images.Media.getBitmap(contentResolver, it)
                }

                try{
                    val bos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 40, bos)

                    val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
                        .addFormDataPart("user_id", UUID.randomUUID().toString())
                        .addFormDataPart("document", it.toFile().name,
                            RequestBody.create(MultipartBody.FORM, bos.toByteArray())
                        ).build()

                    viewModel.uploadDocument(requestBody)
                }catch (e:Exception){
                    e.printStackTrace()
                }
            }

        }
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.CAMERA
                )
                == PackageManager.PERMISSION_DENIED ||
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_DENIED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ), storageRequestCode
                )
            } else {
                // permission granted, launch camera
                clearImageOrLaunchImageCaptureIntent()

            }
        } else {
            // runtime permission not needed, so show camera
            clearImageOrLaunchImageCaptureIntent()
        }
    }

    private fun clearImageOrLaunchImageCaptureIntent() {
        if (tvChooseDocument.text == getString(R.string.remove_photo)) {
            iivChooseDocument.setImageResource(0)
            croppedImgPath = null
            iivChooseDocument.setImageDrawable(
                ContextCompat.getDrawable(
                    this, R.drawable.camera
                )
            )
            tvChooseDocument.let {
                it.text = getString(R.string.take_photo)
                it.setTextColor(ContextCompat.getColor(this, R.color.colorAccent))
            }
        } else {
            dispatchTakePictureIntent()
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    /*val photoURI: Uri*/ cameraUri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileProvider",
                    it
                )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri)
                    startActivityForResult(takePictureIntent, cameraRequestCode)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == cameraRequestCode && resultCode == Activity.RESULT_OK) {
            cropSelectedImage(Uri.fromFile(File(currentPhotoPath)))
        } else if (requestCode == UCrop.REQUEST_CROP && resultCode == Activity.RESULT_OK) {
            data?.let {
                val uri = UCrop.getOutput(it)
                cameraUri = uri
                iivChooseDocument.setImageURI(uri)
                tvChooseDocument.text = getString(R.string.remove_photo)
                tvChooseDocument.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.colorAccent
                    )
                )
            }
        }
    }

    private fun cropSelectedImage(source: Uri) {
        val options = UCrop.Options()
        options.setCompressionQuality(80)
        options.setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
        options.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary))
        options.setToolbarWidgetColor(ContextCompat.getColor(this, R.color.colorTextBody))
        options.setActiveWidgetColor(
            ContextCompat.getColor(
                this, R.color.colorAccent
            )
        )
        options.withMaxResultSize(600, 600)
        options.setHideBottomControls(true)
        options.setFreeStyleCropEnabled(true)

        croppedImgPath = File.createTempFile(
            randomStringGenerator(), ".jpg",
            getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        ).absolutePath

        croppedImgPath?.let {
            UCrop.of(source, Uri.fromFile(File(it)))
                .withOptions(options)
                .start(this)
        }

    }

    private fun randomStringGenerator(): String {
        val random = Random()
        val sb = StringBuilder(Constants.sizeOfRandomString)
        for (i in 0 until Constants.sizeOfRandomString)
            sb.append(Constants.alphaNumericCharacters[random.nextInt(Constants.alphaNumericCharacters.length)])
        return "min_${sb}"
    }

    private fun updateChanges(viewModel: MainActivityViewModel){
        with(viewModel) {
            progress.observe(this@MainActivity, Observer { uiEvent ->
                uiEvent.getContentIfNotHandled()?.let {
                    toggleProgressVisibility(it)
                }
            })

            error.observe(this@MainActivity, Observer { uiEvent ->
                uiEvent.getContentIfNotHandled()?.let {
                    Snackbar.make(cvRootLayout, it, Snackbar.LENGTH_SHORT).show()
                }
            })

            uploadedDocument.observe(this@MainActivity, Observer {uiEvent ->
                uiEvent.getContentIfNotHandled()?.let {
                    Snackbar.make(cvRootLayout, it, Snackbar.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun toggleProgressVisibility(state: Boolean) {
        when (state) {
            true -> {
                pb_upload_status.makeVisible()
                tv_status.makeVisible()
            }
            false -> {
                pb_upload_status.makeGone()
                tv_status.makeGone()
            }
        }
    }
}
