package com.example.noteapp

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.noteapp.Interface.EditImageFragmentListener
import com.example.noteapp.Interface.FilterListFragmentListener
import com.example.noteapp.Utils.BitmapUtils
import com.example.noteapp.Utils.NonSwipeableViewPage
import com.example.noteapp.adapater.ViewPagerAdapter
import com.example.noteapp.fragment.EditImageFragment
import com.example.noteapp.fragment.FilterListFragment
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.zomato.photofilters.imageprocessors.Filter
import com.zomato.photofilters.imageprocessors.subfilters.BrightnessSubFilter
import com.zomato.photofilters.imageprocessors.subfilters.ContrastSubFilter
import com.zomato.photofilters.imageprocessors.subfilters.SaturationSubfilter
import kotlinx.android.synthetic.main.activity_suaanh.*
import kotlinx.android.synthetic.main.content_main.*

class SuaAnhActivity : AppCompatActivity(), FilterListFragmentListener, EditImageFragmentListener {
    val SELECT_GALLERY_PERMISSION = 1000

    init {
        System.loadLibrary("NativeImageProcessor")
    }

    private var originalImage : Bitmap?= null
    private lateinit var filteredImage:Bitmap
    internal lateinit var finalImage:Bitmap
    private lateinit var image_preview: ImageView

    private lateinit var filterListFragment: FilterListFragment
    private lateinit var editImageFragment: EditImageFragment

    private var brightnessFinal = 0
    private var saturationFinal = 1.0f
    private var constrantFinal = 1.0f

    internal var imageUri:Uri? =null
    internal val CAMERA_REQUEST: Int = 9999

    object Main {
        val IMAGE_NAME = "flash.jpg"

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_suaanh)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = "Filter"
        image_preview = findViewById(R.id.image_preview)
        openCamera()
        loadImage()
        setupViewPager(viewPager)
        tabs.setupWithViewPager(viewPager)

        filterListFragment = FilterListFragment.getInstance(null)
    }

    private fun setupViewPager(viewPager: NonSwipeableViewPage?) {
        val adapter = ViewPagerAdapter(supportFragmentManager)

        filterListFragment = FilterListFragment()
        filterListFragment.setListener(this)

        editImageFragment = EditImageFragment()
        editImageFragment.setListener(this)

        adapter.addFragment(filterListFragment,"FILTERS")
        adapter.addFragment(editImageFragment,"EDIT")

        viewPager?.adapter = adapter

    }

    private fun loadImage() {
        originalImage = BitmapUtils.getBitmapFromAssets(this,Main.IMAGE_NAME,300,300)
        filteredImage = originalImage!!.copy(Bitmap.Config.ARGB_8888,true)
        finalImage  = originalImage!!.copy(Bitmap.Config.ARGB_8888,true)
        image_preview.setImageBitmap(originalImage)
    }

    override fun onFilterSelected(filter: Filter) {
        resetControl()
        filteredImage = originalImage!!.copy(Bitmap.Config.ARGB_8888,true)
        Log.e("halo","ddaay")
        image_preview.setImageBitmap(filter.processFilter(filteredImage))
        finalImage = filteredImage.copy(Bitmap.Config.ARGB_8888,true)
    }

    private fun resetControl() {
        editImageFragment.resetControl()
        brightnessFinal = 0
        constrantFinal = 1.0f
        saturationFinal = 1.0f
    }

    override fun onBrightnessChanged(brightness: Int) {
        brightnessFinal = brightness
        val myFilter = Filter()
        Log.e("bri","ok")
        myFilter.addSubFilter(BrightnessSubFilter(brightness))
        image_preview.setImageBitmap(myFilter.processFilter(finalImage.copy(Bitmap.Config.ARGB_8888,true)))
    }

    override fun onSaturationChanged(saturation: Float) {
        saturationFinal = saturation
        val myFilter = Filter()
        myFilter.addSubFilter(SaturationSubfilter(saturation))
        image_preview.setImageBitmap(myFilter.processFilter(finalImage.copy(Bitmap.Config.ARGB_8888,true)))
    }

    override fun onConstrantChanged(constrant: Float) {
        constrantFinal = constrant
        val myFilter = Filter()
        myFilter.addSubFilter(ContrastSubFilter(constrant))
        image_preview.setImageBitmap(myFilter.processFilter(finalImage.copy(Bitmap.Config.ARGB_8888,true)))
    }

    override fun onEditStarted() {

    }

    override fun onEditCompleted() {
        val bitmap = filteredImage.copy(Bitmap.Config.ARGB_8888,true)
        val myFilter = Filter()
        myFilter.addSubFilter(BrightnessSubFilter(brightnessFinal))
        myFilter.addSubFilter(SaturationSubfilter(saturationFinal))
        myFilter.addSubFilter(ContrastSubFilter(constrantFinal))
        finalImage = myFilter.processFilter(bitmap)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            else -> {
            }
        }
        if (id == R.id.action_open){
            openImageFromGallery() // sau nhớ thay bằng mở từ firebase
            return true
        }else if (id == R.id.action_save){
            saveImageFromGallery() // sau nhớ thay bằng lưu vào firebase
            return true
        }else if (id == R.id.action_camera){
            openCamera()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openCamera() {
        val withListener = Dexter.withActivity(this)
            .withPermissions(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                    if (p0!!.areAllPermissionsGranted()){
                        val value = ContentValues()
                        value.put(MediaStore.Images.Media.TITLE,"New Picture")
                        value.put(MediaStore.Images.Media.DESCRIPTION,"From Camera")
                        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,value)

                        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri)
                        startActivityForResult(cameraIntent,CAMERA_REQUEST)
                    }else
                        Toast.makeText(applicationContext,"Permission denied",Toast.LENGTH_SHORT).show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<PermissionRequest>?,
                    p1: PermissionToken?
                ) {
                    p1!!.continuePermissionRequest()
                }

            }).check()
    }

    private fun saveImageFromGallery() {
        val withListener = Dexter.withActivity(this)
            .withPermissions(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                    if (p0!!.areAllPermissionsGranted()){
                        val path = BitmapUtils.insertImage(contentResolver,finalImage,
                            System.currentTimeMillis().toString() + "_profile.jpg","")
                        if (!TextUtils.isEmpty(path)){
                            val snackBar = Snackbar.make(coordinator,"Image save to gallery",Snackbar.LENGTH_LONG)
                                .setAction("OPEN") {
                                    openImage(path)
                                }
                            snackBar.show()
                        }
                        else {
                            val snackBar = Snackbar.make(coordinator,"Unable to save image",Snackbar.LENGTH_LONG)
                            snackBar.show()
                        }
                    }else
                        Toast.makeText(applicationContext,"Permission denied",Toast.LENGTH_SHORT).show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<PermissionRequest>?,
                    p1: PermissionToken?
                ) {
                    p1!!.continuePermissionRequest()
                }

            }).check()
    }

    private fun openImage(path:String?) {
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.setDataAndType(Uri.parse(path),"image/*")
        startActivity(intent)
    }

    private fun openImageFromGallery() {
        val withListener = Dexter.withActivity(this)
            .withPermissions(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                    if (p0!!.areAllPermissionsGranted()){
                        val intent = Intent(Intent.ACTION_PICK)
                        intent.type = "image/*"
                        startActivityForResult(intent,SELECT_GALLERY_PERMISSION)
                    }else
                        Toast.makeText(applicationContext,"Permission denied",Toast.LENGTH_SHORT).show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<PermissionRequest>?,
                    p1: PermissionToken?
                ) {
                    p1!!.continuePermissionRequest()
                }

            }).check()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == SELECT_GALLERY_PERMISSION){
            val bitmap = BitmapUtils.getBitmapFromGallery(this,data?.data!!,800,800)

            originalImage!!.recycle()
            filteredImage.recycle()
            finalImage.recycle()

            originalImage= bitmap?.copy(Bitmap.Config.ARGB_8888,true)
            filteredImage = originalImage!!.copy(Bitmap.Config.ARGB_8888,true)
            finalImage = filteredImage.copy(Bitmap.Config.ARGB_8888,true)

            image_preview.setImageBitmap(originalImage)
            bitmap?.recycle()

            filterListFragment.displayImage(originalImage)

        }else if (resultCode == Activity.RESULT_OK && requestCode == CAMERA_REQUEST){
            val bitmap = BitmapUtils.getBitmapFromGallery(this,imageUri!!,800,800)

            originalImage!!.recycle()
            filteredImage.recycle()
            finalImage.recycle()

            originalImage= bitmap?.copy(Bitmap.Config.ARGB_8888,true)
            filteredImage = originalImage!!.copy(Bitmap.Config.ARGB_8888,true)
            finalImage = filteredImage.copy(Bitmap.Config.ARGB_8888,true)

            image_preview.setImageBitmap(originalImage)
            bitmap?.recycle()
            filterListFragment = FilterListFragment.getInstance(originalImage)
            filterListFragment.setListener(this)


        }
    }
}