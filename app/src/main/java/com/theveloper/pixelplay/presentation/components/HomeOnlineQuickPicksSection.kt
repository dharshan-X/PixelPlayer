package com.theveloper.pixelplay.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.theveloper.pixelplay.R
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import com.theveloper.pixelplay.ui.theme.GoogleSansRounded
import moe.rukamori.archivetune.innertube.models.AlbumItem
import moe.rukamori.archivetune.innertube.models.SongItem
import moe.rukamori.archivetune.innertube.models.YTItem
import moe.rukamori.archivetune.innertube.pages.HomePage

@Composable
fun HomeOnlineQuickPicksSection(
    sections: List<HomePage.Section>,
    isLoading: Boolean,
    resolvingSongId: String?,
    onSongClick: (SongItem, List<SongItem>) -> Unit,
    onNavigateToExplore: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (sections.isEmpty() && !isLoading) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_youtube_music),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = stringResource(R.string.archivetune_trending),
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            TextButton(
                onClick = onNavigateToExplore,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Explore",
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (isLoading && sections.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        } else {
            // Render first 2-3 sections as carousels
            sections.take(3).forEach { section ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    val songsInSection = section.items.filterIsInstance<SongItem>()

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(section.items, key = { it.id }) { item ->
                            HomeOnlineCard(
                                item = item,
                                isResolving = resolvingSongId == item.id,
                                onClick = {
                                    when (item) {
                                        is SongItem -> onSongClick(item, songsInSection)
                                        else -> onNavigateToExplore()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeOnlineCard(
    item: YTItem,
    isResolving: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = AbsoluteSmoothCornerShape(18.dp, 60)
    val artistText = when (item) {
        is SongItem -> item.artists.firstOrNull()?.name ?: ""
        is AlbumItem -> item.artists?.firstOrNull()?.name ?: ""
        else -> ""
    }

    Column(
        modifier = modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            AsyncImage(
                model = item.thumbnail,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dark gradient overlay on card
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f))
            )

            // Play / loading overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                if (isResolving) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (artistText.isNotBlank()) {
                Text(
                    text = artistText,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = GoogleSansRounded,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
