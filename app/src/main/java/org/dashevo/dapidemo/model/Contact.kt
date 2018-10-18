package org.dashevo.dapidemo.model

import org.dashevo.dapiclient.model.BlockchainUser
import org.dashevo.dapiclient.model.DapObject

data class Contact(
        val user: UserContact,
        var blockchainUser: BlockchainUser?,
        val meta: ContactMeta?
) : DapObject()