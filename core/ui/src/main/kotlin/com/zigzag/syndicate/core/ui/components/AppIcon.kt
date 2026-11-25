package com.zigzag.syndicate.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.zigzag.syndicate.core.ui.R

/** The app's icon, shown next to the app name in the navigation drawer header. */
@Composable
fun AppIcon(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.core_ui_app_icon),
        contentDescription = "Syndicate",
        modifier = modifier
            .size(24.dp)
            .clip(RoundedCornerShape(6.dp)),
        contentScale = ContentScale.Crop,
    )
}