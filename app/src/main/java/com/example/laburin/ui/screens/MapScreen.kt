package com.example.laburin.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import com.example.laburin.ui.components.LaburinNavigationBar

data class Offer(
    val title: String,
    val category: String,
    val distance: Int
)

@Composable
fun MapScreen() {
    var rubro by remember { mutableStateOf("Todos") }
    var radio by remember { mutableStateOf("2") }
    val offerList = listOf(Offer("Ayudante de taller", "Mecanica", 3000), Offer("Mozo", "Gastronomia", 700))
    val filteredOffersByRubro = offerList.filter { rubro == "Todos" || it.category == rubro }
    val filteredOffersByRadio = filteredOffersByRubro.filter { it.distance <= radio.toInt() * 1000 }
    var textToSearch by remember { mutableStateOf("") }
    val filteredOffersByText = filteredOffersByRadio.filter { it.title.contains(textToSearch, ignoreCase = true) }

    // Scaffold con bottomBar para que la barra quede fija abajo de todo y topBar arriba
    Scaffold(
        topBar = {
            Column {
                SearchBar(textToSearch, onTextChange = {textToSearch = it}, modifier = Modifier.statusBarsPadding())
                FilterButtons(
                    rubro = rubro, onRubroChange = { rubro = it },
                    radio = radio, onRadioChange = { radio = it }
                )
                PublishButton()
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
            Surface(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
                LazyColumn() {
                    item {
                        Text(
                            "${filteredOffersByText.size} búsquedas destacadas",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(15.dp)
                        )
                    }
                    items(filteredOffersByText) { offer ->
                        OfferItem(offer.title, offer.category, offer.distance)
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBar(textToSearch: String ,onTextChange: (String) -> Unit, modifier: Modifier = Modifier){
    OutlinedTextField(
        modifier = modifier.fillMaxWidth().padding(horizontal = 10.dp),
        shape = RoundedCornerShape(24.dp),
        value = textToSearch,
        onValueChange = onTextChange,
        label = { Text("Buscar Zona o Puesto") },
        placeholder = { Text("Buscar Zona o puesto") }
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
            .padding(horizontal = 10.dp, vertical = 6.dp),
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
                        Row(verticalAlignment = Alignment.CenterVertically){
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
                        Row(verticalAlignment = Alignment.CenterVertically){
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
    OutlinedButton(onClick = {}, modifier.fillMaxWidth().padding(horizontal = 10.dp), content = {Text("¿Buscás empleados? Publicá gratis")})
}

@Composable
fun OfferItem(title: String, category: String, distance: Int, modifier: Modifier = Modifier){
    Card(
        modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp).border(1.dp, Color.Gray,
            RoundedCornerShape(20.dp)),
    ){
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween){
            Column() {
                Text(title, fontWeight = FontWeight.Bold)
                Text(category)
            }
            Text("$distance metros")
        }
    }
}