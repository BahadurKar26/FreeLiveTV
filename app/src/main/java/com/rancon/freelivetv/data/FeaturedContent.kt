package com.rancon.freelivetv.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FeaturedContent(
    val title: String,
    val year: String,
    val genre: String,
    val rating: Double,
    val posterUrl: String = "",
    val streamUrl: String = ""
) : Parcelable
