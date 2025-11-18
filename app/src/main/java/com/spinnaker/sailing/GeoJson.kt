package com.spinnaker.sailing

import com.google.gson.annotations.SerializedName

// Renamed to avoid conflict with maps library
data class SailingGeoJson(
    @SerializedName("type") val type: String = "FeatureCollection",
    @SerializedName("features") val features: List<SailingFeature>
)

// Renamed to avoid conflict
data class SailingFeature(
    @SerializedName("type") val type: String = "Feature",
    @SerializedName("properties") val properties: SailingProperties,
    @SerializedName("geometry") val geometry: SailingGeometry
)

// Renamed to avoid conflict
data class SailingProperties(
    // For race details
    @SerializedName("race_description") val raceDescription: String? = null,
    @SerializedName("race_date") val raceDate: String? = null,
    @SerializedName("start_time") val startTime: String? = null,
    @SerializedName("finish_time") val finishTime: String? = null,

    // For marks
    @SerializedName("mark_name") val markName: String? = null,
    @SerializedName("mark_sequence") val markSequence: Int? = null
)

// Renamed to avoid conflict
data class SailingGeometry(
    @SerializedName("type") val type: String,
    @SerializedName("coordinates") val coordinates: Any
)
