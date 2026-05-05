package com.mobapps.nemt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CardColor = Color(0xFFFFFFFF)
private val BorderSubtle = Color(0xFFE7E8EE)
private val TextPrimary = Color(0xFF111318)
private val TextSecondary = Color(0xFF7A7F8C)
private val BrandBlue = Color(0xFF2F8FFF)
private val BrandRed = Color(0xFFFF5A5F)
private val SuccessGreen = Color(0xFF30D158)

@Composable
fun RideCard(
    status: String,
    dateTime: String,
    from: String,
    to: String,
    patientName: String,
    vehicle: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryActionClick: (() -> Unit)? = null
) {
    val statusColor = when (status) {
        "In progress" -> BrandBlue
        "Requested" -> TextSecondary
        "Accepted" -> BrandBlue
        "Assigned" -> BrandBlue
        "Arrived" -> BrandBlue
        "Scheduled" -> TextSecondary
        "Completed" -> SuccessGreen
        "Cancelled" -> BrandRed
        else -> TextSecondary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .background(CardColor, RoundedCornerShape(18.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        // Top row: status + datetime
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor, CircleShape)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = status,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W600,
                    color = statusColor
                )
            }

            Text(
                text = dateTime,
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // From / to
        Row(
            verticalAlignment = Alignment.Top
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(BrandBlue, CircleShape)
                )

                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .height(20.dp)
                        .padding(vertical = 3.dp)
                        .background(BorderSubtle)
                )

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(BrandRed, CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = from,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.W500,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = to,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.W500,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom row: patient + vehicle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = patientName,
                fontSize = 14.sp,
                color = TextSecondary
            )

            if (vehicle != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DirectionsCar,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = vehicle,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        if ((actionLabel != null && onActionClick != null) ||
            (secondaryActionLabel != null && onSecondaryActionClick != null)
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (secondaryActionLabel != null && onSecondaryActionClick != null) {
                    Text(
                        text = secondaryActionLabel,
                        color = BrandBlue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W600,
                        modifier = Modifier
                            .background(Color(0x112F8FFF), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .border(1.dp, Color(0x332F8FFF), RoundedCornerShape(10.dp))
                            .clickable { onSecondaryActionClick() }
                            .padding(horizontal = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (actionLabel != null && onActionClick != null) {
                    Text(
                        text = actionLabel,
                        color = BrandRed,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W600,
                        modifier = Modifier
                            .background(Color(0x11FF5A5F), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .border(1.dp, Color(0x33FF5A5F), RoundedCornerShape(10.dp))
                            .clickable { onActionClick() }
                            .padding(horizontal = 2.dp)
                    )
                }
            }
        }
    }
}