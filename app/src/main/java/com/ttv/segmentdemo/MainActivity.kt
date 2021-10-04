package com.ttv.segmentdemo

import android.content.Context
import android.content.res.TypedArray
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.ttv.segment.TTVException
import com.ttv.segment.TTVSeg
import io.fotoapparat.Fotoapparat
import io.fotoapparat.parameter.Resolution
import io.fotoapparat.preview.Frame
import io.fotoapparat.selector.front
import io.fotoapparat.util.FrameProcessor
import io.fotoapparat.view.CameraView
import java.lang.Exception
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.*
import android.widget.ImageView
import androidx.core.graphics.drawable.toBitmap


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private val permissionsDelegate = PermissionsDelegate(this)
    private var hasPermission = false

    private var appCtx: Context? = null
    private var licenseValid = false
    private var humanSegInited = false
    private var cameraView: CameraView? = null
    private var imageView: ImageView? = null

    private var frontFotoapparat: Fotoapparat? = null
    private val mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            val i: Int = msg.what
            if (i == 0) {
                imageView!!.setImageBitmap(msg.obj as Bitmap)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        appCtx = applicationContext
        cameraView = findViewById<View>(R.id.camera_view) as CameraView
        imageView = findViewById<View>(R.id.image_view) as ImageView

        TTVSeg.createInstance(this)

        hasPermission = permissionsDelegate.hasPermissions()
        if (hasPermission) {
            cameraView!!.visibility = View.VISIBLE
        } else {
            permissionsDelegate.requestPermissions()
        }

        frontFotoapparat = Fotoapparat.with(this)
            .into(cameraView!!)
            .lensPosition(front())
            .frameProcessor(SampleFrameProcessor())
            .previewResolution { Resolution(1280,720) }
            .build()

        val ret = TTVSeg.getInstance().setLicenseInfo("")
        if(ret == 0) {
            licenseValid = true
            init()
        }
        Log.e(TAG, "activation: " + ret)
    }

    private fun init() {
        if (!licenseValid) {
            return
        }

        try {
            if (TTVSeg.getInstance().create(appCtx, 0, 0, 0) == 0) {
                humanSegInited = true
                return
            }
        } catch (e: TTVException) {
            e.printStackTrace()
        }
    }


    override fun onStart() {
        super.onStart()
        if (hasPermission) {
            frontFotoapparat!!.start()
        }
    }


    override fun onStop() {
        super.onStop()
        if (hasPermission) {
            try {
                frontFotoapparat!!.stop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (permissionsDelegate.hasPermissions() && !hasPermission) {
            hasPermission = true
            cameraView!!.visibility = View.VISIBLE
            frontFotoapparat!!.start()
        } else {
            permissionsDelegate.requestPermissions()
        }
    }

    inner class SampleFrameProcessor : FrameProcessor {
        private var ttvSeg: TTVSeg? = null
        private val bgColor = intArrayOf(255, 255, 255)
        private var bgIdx: Int
        private var bgBitmapArr: ArrayList<Bitmap>? = null

        init {
            ttvSeg = TTVSeg.getInstance()
            bgIdx = 0
            bgBitmapArr = ArrayList<Bitmap>()

            val ids:TypedArray = resources.obtainTypedArray(R.array.scene_3)
            val len = ids.length() - 1
            for(i in 0..len) {
                val bitmap : Bitmap = BitmapFactory.decodeResource(resources, ids.getResourceId(i, -1))
                bgBitmapArr!!.add(bitmap)
            }

        }

        override fun invoke(frame: Frame) {
            if(!humanSegInited) {
                return
            }

            if(bgIdx >= bgBitmapArr!!.size)
                bgIdx = 0;

            val iArr = IntArray(1)
            val segment: Bitmap = ttvSeg!!.process(
                frame.image,
                frame.size.width,
                frame.size.height,
                frame.rotation,
                1,
                bgColor,
                bgBitmapArr!!.get(bgIdx),
                iArr
            )
            bgIdx ++;

            sendMessage(0, segment)
        }

        /* access modifiers changed from: private */ /* access modifiers changed from: public */
        private fun sendMessage(w: Int, o: Any) {
            val message = Message()
            message.what = w
            message.obj = o
            mHandler.sendMessage(message)
        }
    }
}