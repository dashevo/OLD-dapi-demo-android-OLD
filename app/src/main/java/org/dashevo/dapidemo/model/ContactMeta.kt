package org.dashevo.dapidemo.model

import io.realm.RealmModel
import io.realm.annotations.RealmClass

@RealmClass
data class ContactMeta(
        val buid: String,
        val uname: String
) : RealmModel