package shunsuke.saito.jp.charitaro

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_search.*

class SearchActivity : AppCompatActivity() {

    private var mLongitude: Double = 0.0
    private var mLatitude: Double = 0.0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        mLongitude = intent.getDoubleExtra("Longitude", 0.0)
        mLatitude = intent.getDoubleExtra("Latitude", 0.0)
        Log.d("SENSOR_TAG", mLongitude.toString())
        longitude.append(mLongitude.toString())
        latitude.append(mLatitude.toString())
    }
}
