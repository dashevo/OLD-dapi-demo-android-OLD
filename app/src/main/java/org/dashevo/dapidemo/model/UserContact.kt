package org.dashevo.dapidemo.model

import io.realm.RealmModel
import io.realm.annotations.RealmClass

@RealmClass
data class UserContact(val userId: String) : RealmModel