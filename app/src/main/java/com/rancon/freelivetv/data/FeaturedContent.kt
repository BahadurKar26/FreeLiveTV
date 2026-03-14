package com.rancon.freelivetv.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FeaturedContent(
    val id: String = "featured_default",
    val title: String,
    val year: String,
    val genre: String,
    val rating: Double,
    val backdropUrl: String? = null, // Review 1011: Added backdrop support
    val description: String = "",
    val streamUrl: String = ""
) : Parcelable
