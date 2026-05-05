package com.mobapps.nemt.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobapps.nemt.R
import com.mobapps.nemt.notifications.NemtNotificationType
import com.mobapps.nemt.notifications.NemtNotifications

private val GlassPageBackground = Color(0xFFF3F4F7)
private val GlassPanel = Color(0xDFFFFFFF)
private val GlassStroke = Color(0xCCFFFFFF)
private val GlassText = Color(0xFF111318)
private val GlassMuted = Color(0xFF6C7484)
private val GlassBlue = Color(0xFF2F8FFF)

@Composable
fun HelpSupportScreen(
    onBack: () -> Unit,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = GlassPageBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            GlassTopBar(
                title = "Help & support",
                onBack = onBack
            )

            Spacer(modifier = Modifier.height(20.dp))

            HeroGlassCard(
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                title = "Support for transport coordination",
                body = "Reach the care team for ride updates, booking questions, accessibility assistance and account support."
            )

            Spacer(modifier = Modifier.height(18.dp))

            InfoGlassCard(
                icon = Icons.Outlined.Email,
                title = "Email support",
                body = "support@nemt-care.com\nTypical response time: within 24 hours"
            )

            Spacer(modifier = Modifier.height(14.dp))

            SupportContactActions(
                onDial = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_DIAL).setData(
                                Uri.parse(context.getString(R.string.support_phone_uri))
                            )
                        )
                        NemtNotifications.notifyNow(
                            context = context,
                            type = NemtNotificationType.SUPPORT_CONTACT,
                            title = "Support opened",
                            body = "Calling support line."
                        )
                    }.onFailure {
                        if (it is ActivityNotFoundException) {
                            Toast.makeText(context, "No app can place calls on this device.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onSms = {
                    val uri = Uri.parse(context.getString(R.string.support_sms_uri))
                    val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                        putExtra("sms_body", context.getString(R.string.support_sms_body))
                    }
                    runCatching {
                        context.startActivity(intent)
                        NemtNotifications.notifyNow(
                            context = context,
                            type = NemtNotificationType.SUPPORT_CONTACT,
                            title = "Support opened",
                            body = "SMS support composer opened."
                        )
                    }.onFailure {
                        Toast.makeText(context, "Could not open SMS.", Toast.LENGTH_LONG).show()
                    }
                },
                onEmail = {
                    val email = context.getString(R.string.support_email)
                    val intent = Intent(Intent.ACTION_SENDTO).setData(
                        Uri.parse("mailto:${Uri.encode(email)}?subject=${Uri.encode(context.getString(R.string.support_email_subject))}")
                    )
                    runCatching {
                        context.startActivity(intent)
                        NemtNotifications.notifyNow(
                            context = context,
                            type = NemtNotificationType.SUPPORT_CONTACT,
                            title = "Support opened",
                            body = "Email support composer opened."
                        )
                    }.onFailure {
                        Toast.makeText(context, "No email app found.", Toast.LENGTH_LONG).show()
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            InfoGlassCard(
                icon = Icons.Outlined.Schedule,
                title = "Availability",
                body = "Monday to Friday\n08:00 - 19:00 local time"
            )

            Spacer(modifier = Modifier.height(14.dp))

            InfoGlassCard(
                icon = Icons.Outlined.Security,
                title = "Sensitive requests",
                body = "Please avoid sending medical records by email. For patient-specific needs, contact your transport coordinator directly."
            )
        }
    }
}

@Composable
fun TermsPrivacyScreen(
    onBack: () -> Unit,
    contentPadding: PaddingValues
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = GlassPageBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            GlassTopBar(
                title = "Terms & privacy",
                onBack = onBack
            )

            Spacer(modifier = Modifier.height(20.dp))

            HeroGlassCard(
                icon = Icons.Outlined.Description,
                title = "NEMT rider terms",
                body = "These terms describe how riders and caregivers use the app to manage non-emergency medical transportation."
            )

            Spacer(modifier = Modifier.height(18.dp))

            PolicySection(
                title = "Privacy policy",
                body = "NEMT stores account details, profile preferences and mobility support information to coordinate transport safely. We only use this information to operate the service, improve ride communication and support accessibility needs."
            )

            Spacer(modifier = Modifier.height(14.dp))

            PolicySection(
                title = "Data use",
                body = "Your profile data may be viewed by authorized staff involved in scheduling, dispatching and supporting your rides. We do not sell personal information to third parties."
            )

            Spacer(modifier = Modifier.height(14.dp))

            PolicySection(
                title = "Ride responsibilities",
                body = "Riders and caregivers are responsible for keeping pickup details, contact information and mobility support requirements accurate. Incorrect information may affect service timing or vehicle assignment."
            )

            Spacer(modifier = Modifier.height(14.dp))

            PolicySection(
                title = "Account security",
                body = "You are responsible for maintaining access to your verified email and protecting your login credentials. If you suspect unauthorized access, contact support immediately."
            )

            Spacer(modifier = Modifier.height(14.dp))

            PolicySection(
                title = "Service limitations",
                body = "NEMT is intended for non-emergency medical transport only. It must not be used to request emergency medical response or urgent life-saving intervention."
            )
        }
    }
}

@Composable
private fun GlassTopBar(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(GlassPanel)
                .border(1.dp, GlassStroke, RoundedCornerShape(14.dp))
                .clickable { onBack() }
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = GlassText,
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Transparent)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            color = GlassText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HeroGlassCard(
    icon: ImageVector,
    title: String,
    body: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(GlassPanel, Color(0xD5F4F8FF))
                )
            )
            .border(1.dp, GlassStroke, RoundedCornerShape(28.dp))
            .padding(20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.72f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GlassBlue,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            color = GlassText,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = body,
            color = GlassMuted,
            fontSize = 14.sp,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun InfoGlassCard(
    icon: ImageVector,
    title: String,
    body: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(GlassPanel)
            .border(1.dp, GlassStroke, RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GlassBlue,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                color = GlassText,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = body,
                color = GlassMuted,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
        }
    }
}

@Composable
private fun PolicySection(
    title: String,
    body: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(GlassPanel)
            .border(1.dp, GlassStroke, RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Text(
            text = title,
            color = GlassText,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = body,
            color = GlassMuted,
            fontSize = 14.sp,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun SupportContactActions(
    onDial: () -> Unit,
    onSms: () -> Unit,
    onEmail: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(GlassPanel)
            .border(1.dp, GlassStroke, RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Text(
            text = "Contact options",
            color = GlassText,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onDial,
                modifier = Modifier.weight(1f)
            ) {
                Text("Call", fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = onSms,
                modifier = Modifier.weight(1f)
            ) {
                Text("SMS", fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = onEmail,
                modifier = Modifier.weight(1f)
            ) {
                Text("Email", fontSize = 13.sp)
            }
        }
    }
}
