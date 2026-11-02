package com.example.laburin.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.laburin.domain.model.GeoLocation
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

data class MapMarker(val id: String, val position: GeoLocation, val title: String, val snippet: String)

/**
 * Only file that knows the map library (osmdroid). Switching provider means rewriting this body only;
 * callers depend on [GeoLocation] and [MapMarker].
 */
@Composable
fun DisplayMap(
    markers: List<MapMarker>,
    center: GeoLocation,
    modifier: Modifier = Modifier,
    zoomLevel: Double = 14.0,
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
            }
        },
        update = { mapView ->
            // Recenter only when the search center changes, so recompositions don't undo the user's panning.
            if (mapView.tag != center) {
                mapView.controller.setCenter(center.toOsmGeoPoint())
                mapView.tag = center
            }
            mapView.overlays.clear()
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
            mapView.invalidate()
        },
    )
}

private fun GeoLocation.toOsmGeoPoint() = GeoPoint(latitude, longitude)
