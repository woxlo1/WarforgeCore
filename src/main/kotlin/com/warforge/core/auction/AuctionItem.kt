package com.warforge.core.auction

import org.bukkit.inventory.ItemStack
import java.util.UUID

data class AuctionItem(
    val id: Int,
    val sellerUuid: UUID,
    val sellerName: String,
    val item: ItemStack,
    val startPrice: Double,
    var currentBid: Double,
    var highestBidderUuid: UUID? = null,
    var highestBidderName: String? = null,
    val listedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (60 * 60 * 1000L), // 1時間
    var status: AuctionStatus = AuctionStatus.ACTIVE
)

enum class AuctionStatus { ACTIVE, SOLD, EXPIRED, CANCELLED }
