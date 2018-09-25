package shunsuke.saito.jp.charitaro

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_search.*

class SearchActivity : AppCompatActivity(), LocationListener, SensorEventListener {
    private fun radianToDegrees(angrad: Float): Int {
        return Math.floor(if (angrad >= 0) Math.toDegrees(angrad.toDouble()) else 360 + Math.toDegrees(angrad.toDouble())).toInt()
    }

    //  MainActivityから引き渡される登録済みの位置情報
    private var mLongitude: Double = 0.0
    private var mLatitude: Double = 0.0

    // GPSによる位置情報取得のための変数宣言
    private var mLocationManager: LocationManager? = null
    private var currentLongitude: Double = 0.0
    private var currentLatitude: Double = 0.0
    private var gpsDegree: Double = 0.0
    private var distance: Double = 0.0

    /* 方位センサ&加速度センサによる現在の方角を知るための変数 */
    // 行列数
    private val MATRIX_SIZE: Int = 16
    // 三次元
    private val DIMENTION: Int = 3
    // Sensor管理のための変数
    private lateinit var mSensorManager: SensorManager
    // 地磁気行列
    private var mMagneticValues: FloatArray? = null
    /** 加速度行列  */
    private var mAccelerometerValues: FloatArray? = null
    /** X軸の回転角度  */
    private var mPitchX: Int = 0
    /** Y軸の回転角度  */
    private var mRollY: Int = 0
    /** Z軸の回転角度(方位角)  */
    private var mAzimuthZ: Int = 0
    private lateinit var mAccelerameter: Sensor
    private lateinit var mMagineticField: Sensor
    var timer: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        mLongitude = intent.getDoubleExtra("Longitude", 0.0)
        mLatitude = intent.getDoubleExtra("Latitude", 0.0)
        Log.d("SENSOR_TAG", mLongitude.toString())
        longitude.append(mLongitude.toString())
        latitude.append(mLatitude.toString())
        // 位置情報センサの取得
        mLocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        // 方位センサと加速度センサの登録
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mAccelerameter = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mMagineticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    }

    override fun onResume() {
        super.onResume()
        mLocationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1.0F, this)
        mSensorManager.registerListener(this, mAccelerameter, SensorManager.SENSOR_DELAY_UI)
        mSensorManager.registerListener(this, mMagineticField, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onPause() {
        super.onPause()
        mLocationManager?.removeUpdates(this)
        mSensorManager.unregisterListener(this)
    }

    override fun onProviderDisabled(p0: String?) {

    }

    override fun onProviderEnabled(p0: String?) {

    }

    override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {

    }

    override fun onLocationChanged(location: Location?) {
        /*
        Latitude: 緯度(南-90〜北90)
        Longitude: 経度(西-180〜東180)
         */
        if (location != null) {
            currentLatitude = location.latitude
            currentLongitude = location.longitude
        }
        val myMathList: MyMathList = MyMathList()
        val distance = myMathList.calcDistance(mLongitude, mLatitude, currentLongitude, currentLatitude)
        // asinで求める方向が東側か判断する
        val isEast: Boolean = (mLongitude - currentLongitude) > 0.0
        // 自転車の場所にいるならば、sinはnull
        val sin: Double? = if (distance != 0.0) (mLatitude - currentLatitude) / distance else null
        if (sin == null) {
            return Unit
        }
        val radian: Double = Math.asin(sin)
        gpsDegree = Math.toDegrees(radian)
        Log.d("DEGREE", gpsDegree.toString())
        if (gpsDegree < 0.0) {
            gpsDegree = gpsDegree * -1 + 90
            if (!isEast) {
                gpsDegree *= -1
            }
        } else {
            gpsDegree = 90.0 - gpsDegree
        }
        val printFun: (Double) -> String = {
            if (it < 0.0) {
                 "西側で北から${gpsDegree}度回転したところ"
            } else {
                "東側で北から${gpsDegree}度回転したところ"
            }
        }
        Log.d("DEGREE", printFun(gpsDegree))
    }

    override fun onAccuracyChanged(event: Sensor?, accuracy: Int) {

    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return Unit
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                mAccelerometerValues = event.values
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                mMagneticValues = event.values
            }
        }
        if (mAccelerometerValues != null && mMagneticValues != null) {
            val rotationMatrix = FloatArray(MATRIX_SIZE)
            val inclinationMatrix = FloatArray(MATRIX_SIZE)
            val remapedMatrix = FloatArray(MATRIX_SIZE)
            val orientationValues = FloatArray(DIMENTION)
            // 加速度センサーと地磁気センサーから回転行列を取得
            SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, mAccelerometerValues, mMagneticValues)
            SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, remapedMatrix)
            SensorManager.getOrientation(remapedMatrix, orientationValues)
            // ラジアン値を変換し、それぞれの回転角度を取得する
            mAzimuthZ = radianToDegrees(orientationValues[0])
            mPitchX = radianToDegrees(orientationValues[1])
            mRollY = radianToDegrees(orientationValues[2])
            // ローパスフィルタ
            mAzimuthZ = (mAzimuthZ * 0.9 + radianToDegrees(orientationValues[0]) * 0.1).toInt()
            mPitchX = (mPitchX * 0.9 + radianToDegrees(orientationValues[1]) * 0.1).toInt()
            mRollY = (mRollY * 0.9 + radianToDegrees(orientationValues[2]) * 0.1).toInt()
            if (mAzimuthZ == 0) {
                Log.d("MAGNETICFIELD", "X=" + mPitchX + "Y=" + mRollY + "Z=" + mAzimuthZ)
            }
            timer++
            if (timer >= 20) {
                // -mAzimuthZ.toFloat()を代入して常に北を指し示す
                search_arrow.rotation = -mAzimuthZ.toFloat()
                // 常に北を指し示す矢印を、gpsDegree分回転させることで自転車の位置を示す
                Log.d("INSERT", gpsDegree.toString())
                search_arrow.rotation += gpsDegree.toFloat()
                Log.d("SearchArrow", search_arrow.rotation.toString())
                timer = 0
            }
            angle.text = mAzimuthZ.toString()
        }
    }
}

class MyMathList() {
    public fun pow(a: Double, times: Int): Double {
        var value = a
        for (i in 1..times - 1) {
            value *= value
        }
        return value
    }

    public fun calcDistance(targetLongitude: Double, targetLatitude: Double, currentLongitude: Double, currentLatitude: Double): Double {
        val diffLongitude = targetLongitude - currentLongitude
        val diffLatitude = targetLatitude - currentLatitude
        val value = pow(diffLongitude, 2) + pow(diffLatitude, 2)
        val distance = Math.sqrt(value)
        return distance
    }
}
