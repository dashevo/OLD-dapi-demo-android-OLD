package org.dashevo.dapidemo.model

import com.google.gson.annotations.SerializedName
import org.dashevo.dapiclient.model.BlockchainUser
import org.dashevo.dapiclient.model.DapObject

data class Contact(
        val user: UserContact,
        @SerializedName("hdextpubkey")
        val hdExtPubKey: String,
        var blockchainUser: BlockchainUser?,
        val meta: ContactMeta?
) : DapObject()