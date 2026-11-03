package com.example.laburin.ui.components

import android.graphics.DashPathEffect
import android.graphics.drawable.GradientDrawable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.laburin.domain.model.GeoLocation
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

data class MapMarker(val id: String, val position: GeoLocation, val title: String, val snippet: String)

data class MapCircle(val center: GeoLocation, val radiusMeters: Double)

/**
 * Only file that knows the map library (osmdroid). Switching provider means rewriting this body only;
 * callers depend on [GeoLocation], [MapMarker] and [MapCircle].
 */
@Composable
fun DisplayMap(
    markers: List<MapMarker>,
    center: GeoLocation,
    modifier: Modifier = Modifier,
    zoomLevel: Double = DEFAULT_ZOOM,
    userLocation: GeoLocation? = null,
    searchArea: MapCircle? = null,
    bottomObstructionFraction: Float = 0f,
    onMarkerClick: (String) -> Unit = {},
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            Configuration.getInstance().userAgentValue = "Laburin"
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(zoomLevel)
                // Keep the search center visible above a bottom panel that covers part of the map.
                addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
                    if (bottom - top != oldBottom - oldTop) {
                        setMapCenterOffset(0, mapCenterOffsetY(bottom - top, bottomObstructionFraction))
                        (tag as? GeoLocation)?.let { controller.setCenter(it.toOsmGeoPoint()) }
                    }
                }
            }
        },
        update = { mapView ->
            // Recenter only when the search center changes, so recompositions don't undo the user's panning.
            if (mapView.tag != center) {
                mapView.controller.setCenter(center.toOsmGeoPoint())
                mapView.tag = center
            }
            mapView.overlays.clear()
            searchArea?.let { mapView.overlays.add(searchAreaOverlay(it)) }
            markers.forEach { marker ->
                mapView.overlays.add(
                    Marker(mapView).apply {
                        position = marker.position.toOsmGeoPoint()
                        title = marker.title
                        snippet = marker.snippet
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        setOnMarkerClickListener { _, _ -> onMarkerClick(marker.id); true }
                    }
                )
            }
            userLocation?.let { mapView.overlays.add(userLocationOverlay(mapView, it)) }
            mapView.invalidate()
        },
    )
}

private const val DEFAULT_ZOOM = 15.0
private const val USER_DOT_SIZE_PX = 44
private const val USER_DOT_BORDER_PX = 8
private const val USER_BLUE = 0xFF1A73E8.toInt()
private const val WHITE = 0xFFFFFFFF.toInt()
private const val AREA_STROKE = 0xFF555555.toInt()
private const val AREA_FILL = 0x141A73E8

private fun searchAreaOverlay(area: MapCircle) = Polygon().apply {
    points = Polygon.pointsAsCircle(area.center.toOsmGeoPoint(), area.radiusMeters)
    outlinePaint.color = AREA_STROKE
    outlinePaint.strokeWidth = 4f
    outlinePaint.pathEffect = DashPathEffect(floatArrayOf(20f, 14f), 0f)
    fillPaint.color = AREA_FILL
    setOnClickListener { _, _, _ -> false }
}

private fun userLocationOverlay(mapView: MapView, location: GeoLocation) = Marker(mapView).apply {
    position = location.toOsmGeoPoint()
    icon = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(USER_BLUE)
        setStroke(USER_DOT_BORDER_PX, WHITE)
        setSize(USER_DOT_SIZE_PX, USER_DOT_SIZE_PX)
    }
    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
    infoWindow = null
    setOnMarkerClickListener { _, _ -> true }
}

private fun GeoLocation.toOsmGeoPoint() = GeoPoint(latitude, longitude)
