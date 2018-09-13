package shunsuke.saito.jp.charitaro

import io.realm.RealmObject

open class LocationData(open var isSaved: Boolean = false,
                        open var latitude: Double = 0.0,
                        open var longitude: Double = 0.0): RealmObject(){}