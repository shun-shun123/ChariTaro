package shunsuke.saito.jp.charitaro

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required

open class LocationData: RealmObject() {
    @PrimaryKey
    var isUpdate: Boolean? = null
    @Required
    var latitude: Double = 0.0
    @Required
    var longitude: Double = 0.0
}