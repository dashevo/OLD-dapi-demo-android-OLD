package org.dashevo.dapidemo.model

import io.realm.RealmModel
import io.realm.annotations.RealmClass
import org.dashevo.dapiclient.model.DapObject

@RealmClass
open class DapObjectExt : DapObject(), RealmModel