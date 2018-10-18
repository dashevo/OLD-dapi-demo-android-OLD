package org.dashevo.dapidemo.model

import org.dashevo.dapiclient.model.BlockchainUser

data class Contact(
        val user: UserContact,
        var blockchainUser: BlockchainUser?,
        val meta: ContactMeta?
) : DapObjectExt()