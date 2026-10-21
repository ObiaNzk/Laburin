package com.example.laburin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.laburin.ui.components.LaburinNavigationBar
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

data class Offer(
    val title: String,
    val category: String,
    val distance: Int,
    val location: GeoPoint,
    val address: String
)

@Composable
fun MapScreen() {
    var rubro by remember { mutableStateOf("Todos") }
    var radio by remember { mutableStateOf("2") }
    
    val offerList = listOf(
        Offer("Ayudante de taller", "Mecanica", 5, GeoPoint(-34.6080, -58.3712), "Av. Corrientes 1234"),
        Offer("Mozo", "Gastronomia", 700, GeoPoint(-34.5997, -58.3819), "Calle Ficticia 456"),
        Offer("Repositor", "Repositor", 1200, GeoPoint(-34.6120, -58.3900), "Av. Rivadavia 2000")
    )
    
    val filteredOffersByRubro = offerList.filter { rubro == "Todos" || it.category == rubro }
    val filteredOffersByRadio = filteredOffersByRubro.filter { it.distance <= radio.toInt() * 1000 }
    var textToSearch by remember { mutableStateOf("") }
    val filteredOffersByText = filteredOffersByRadio.filter { it.title.contains(textToSearch, ignoreCase = true) }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 6.dp,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(vertical = 8.dp)
                ) {
                    SearchBar(textToSearch, onTextChange = { textToSearch = it })
                    FilterButtons(
                        rubro = rubro, onRubroChange = { rubro = it },
                        radio = radio, onRadioChange = { radio = it }
                    )
                    PublishButton()
                }
            }
        },
        bottomBar = {
            LaburinNavigationBar()
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            DisplayMap(
                offers = filteredOffersByText,
                modifier = Modifier.fillMaxSize()
            )
            
            FloatingActionButton(
                onClick = {}, 
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 260.dp), 
                shape = CircleShape
            ) {
                Text("QR", fontWeight = FontWeight.Bold)
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.42f)
                    .align(Alignment.BottomCenter), 
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                shadowElevation = 16.dp,
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .background(Color.LightGray, RoundedCornerShape(2.dp))
                        )
                    }

                    Text(
                        "${filteredOffersByText.size} búsquedas destacadas",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredOffersByText) { offer ->
                            OfferItem(offer.title, offer.category, offer.distance, offer.address)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBar(textToSearch: String, onTextChange: (String) -> Unit, modifier: Modifier = Modifier){
    OutlinedTextField(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        value = textToSearch,
        onValueChange = onTextChange,
        placeholder = { Text("Buscar zona o puesto") }
    )
}

@Composable
fun FilterButtons(rubro: String, onRubroChange: (String) -> Unit,
                  radio: String, onRadioChange: (String) -> Unit,
                  modifier: Modifier = Modifier){

    var expandedRadio by remember { mutableStateOf(false) }
    var expandedRubro by remember { mutableStateOf(false) }
    
    val radioOptions = listOf("2", "5", "10", "15")
    val rubroOptions = listOf("Todos", "Gastronomia", "Mecanica", "Repositor")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier.weight(1f),
        ) {
            AssistChip(
                onClick = { expandedRadio = true },
                label = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center){
                            Text("Radio: $radio km")
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            DropdownMenu(
                expanded = expandedRadio, 
                onDismissRequest = { expandedRadio = false }
            ) {
                radioOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text("$option km") },
                        onClick = {
                            onRadioChange(option)
                            expandedRadio = false
                        }
                    )
                }
            }
        }
        Box(
            modifier = Modifier.weight(1f),
        ){
            AssistChip(
                onClick = { expandedRubro = true },
                label = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center){
                            Text("Rubro: $rubro")
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            DropdownMenu(
                expanded = expandedRubro,
                onDismissRequest = { expandedRubro = false }
            ) {
                rubroOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onRubroChange(option)
                            expandedRubro = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PublishButton(modifier: Modifier = Modifier){
    Button(
        onClick = {}, 
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text("¿Buscás empleados? Publicá gratis", fontWeight = FontWeight.Bold)
    }
}

@Composable
fun OfferItem(title: String, category: String, distance: Int, address: String, modifier: Modifier = Modifier){
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ){
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp), 
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ){
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(category, color = Color.Gray, fontSize = 13.sp)
                Text(address, fontSize = 12.sp, color = Color.DarkGray)
            }
            Text("$distance m", fontWeight = FontWeight.Bold, color = Color.DarkGray)
        }
    }
}

@Composable
fun DisplayMap(offers: List<Offer>, modifier: Modifier = Modifier){
    AndroidView(
        modifier = modifier,
        factory = { context ->
            Configuration.getInstance().userAgentValue = "Laburin"
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(14.0)
                controller.setCenter(GeoPoint(-34.6037, -58.3816))
            }
        },
        update = { mapView ->
            mapView.overlays.clear()
            offers.forEach { offer ->
                val marker = Marker(mapView).apply {
                    position = offer.location
                    title = offer.title
                    snippet = "${offer.category} - ${offer.address}"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                mapView.overlays.add(marker)
            }
            mapView.invalidate()
        }
    )
}
