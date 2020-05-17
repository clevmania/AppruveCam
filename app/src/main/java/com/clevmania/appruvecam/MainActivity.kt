package com.clevmania.appruvecam

import android.Manifest
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
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.yalantis.ucrop.UCrop
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

class MainActivity : AppCompatActivity(), HasAndroidInjector {
    private var cameraUri: Uri? = null
    private var croppedImgPath: String? = null

    private var preview: Preview? = null
    private var imageCapture : ImageCapture? = null
    private var camera : Camera? = null

    private lateinit var outputDirectory : File
    private lateinit var cameraExecutor : ExecutorService

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var viewModelProviderFactory: ViewModelProvider.Factory

    private val viewModel by viewModels<MainActivityViewModel> { viewModelProviderFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        updateChanges(viewModel)

        ivChooseDocument.setOnClickListener { checkPermission() }
        tvChooseDocument.setOnClickListener { clearImageOrLaunchImageCaptureIntent() }
        ivCaptureImage.setOnClickListener { takePhoto() }

        captureAndUploadSelfIeImage()
    }

    private fun checkPermission(){
        if (allPermissionsGranted()){
            startCamera()
        }else{
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun captureAndUploadSelfIeImage() {
        btnContinue.setOnClickListener {
            if (croppedImgPath == null) {
                Snackbar.make(cvRootLayout, "Take Document Photograph", Snackbar.LENGTH_SHORT).show()
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

    private fun clearImageOrLaunchImageCaptureIntent() {
        if (tvChooseDocument.text == getString(R.string.remove_photo)) {
            iivChooseDocument.apply {
                setImageResource(0)
                croppedImgPath = null
                setImageDrawable(ContextCompat.getDrawable(this.context, R.drawable.camera))
            }

            tvChooseDocument.apply {
                text = getString(R.string.take_photo)
                setTextColor(ContextCompat.getColor(this.context, R.color.colorAccent))
            }
        }
    }

    private fun startCamera(){
        grpVerifyDocument.makeGone()
        grpImageCapture.makeVisible()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider : ProcessCameraProvider = cameraProviderFuture.get()

            preview = Preview.Builder().build()

            imageCapture = ImageCapture.Builder()
                .apply {
                    setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)//CAPTURE_MODE_MAXIMIZE_QUALITY
                }
                .build()

            val cameraSelector = CameraSelector
                .Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(this,
                    cameraSelector,preview,imageCapture)
                preview?.setSurfaceProvider(pvViewFinder.createSurfaceProvider(camera?.cameraInfo))
            }catch (ex: java.lang.Exception) {
                Log.e(TAG,"Use case binding failed", ex)
            }
        },
            ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto(){
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    cropSelectedImage(savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG,"Photo capture failed: ${exception.message}",exception)
                }

            }
        )
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if ( mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == REQUEST_CODE_PERMISSIONS){
            if(allPermissionsGranted()){
                startCamera()
            }else {
                Snackbar.make(cvRootLayout,
                    "Permissions not granted by the user.", Snackbar.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UCrop.REQUEST_CROP && resultCode == Activity.RESULT_OK) {
            data?.let {
                val uri = UCrop.getOutput(it)
                cameraUri = uri
                iivChooseDocument.setImageURI(uri)
                tvChooseDocument.apply {
                    text = getString(R.string.remove_photo)
                    setTextColor(ContextCompat.getColor(this.context, R.color.colorAccent))
                }
                grpImageCapture.makeGone()
                grpVerifyDocument.makeVisible()
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
            outputDirectory
        ).absolutePath

        croppedImgPath?.let {
            UCrop.of(source, Uri.fromFile(File(it)))
                .withOptions(options)
                .start(this)
        }

    }

    private fun randomStringGenerator(): String {
        val sb = StringBuilder(Constants.sizeOfRandomString)
        for (i in 0 until Constants.sizeOfRandomString)
            sb.append(Constants.alphaNumericCharacters[Random().nextInt(Constants.alphaNumericCharacters.length)])
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
                    Snackbar.make(cvRootLayout, "File Upload Successful", Snackbar.LENGTH_SHORT).show()
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

    override fun androidInjector(): AndroidInjector<Any> {
        return androidInjector
    }

    companion object{
        private const val TAG = "CameraXActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}
