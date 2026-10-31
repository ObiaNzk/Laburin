package com.example.laburin.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.laburin.model.Offer
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun DisplayMap(
    offers: List<Offer>,
    modifier: Modifier = Modifier,
    zoomLevel: Double = 14.0,
    initialCenter: GeoPoint = GeoPoint(-34.6037, -58.3816)
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            Configuration.getInstance().userAgentValue = "Laburin"
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(zoomLevel)
                controller.setCenter(initialCenter)
            }
        },
        update = { mapView ->
            mapView.overlays.clear()
            offers.forEach { offer ->
                offer.location?.let { loc ->
                    val marker = Marker(mapView).apply {
                        position = loc
                        title = offer.title
                        snippet = "${offer.category} - ${offer.address}"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    mapView.overlays.add(marker)
                    if (offers.size == 1) {
                        mapView.controller.setCenter(loc)
                    }
                }
            }
            mapView.invalidate()
        }
    )
}
