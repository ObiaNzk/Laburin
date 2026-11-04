package com.example.laburin.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OfferItem(
    title: String,
    subtitle: String,
    distance: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    address: String = "",
    salary: String? = null,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF222222))
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, color = Color(0xFF777777), fontSize = 13.sp)
                if (salary != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = salary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF2E7D32))
                }
                if (address.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = address, fontSize = 12.sp, color = Color(0xFF555555))
                }
            }
            Text(text = distance, fontWeight = FontWeight.Bold, color = Color(0xFF444444), fontSize = 14.sp)
        }
    }
}
