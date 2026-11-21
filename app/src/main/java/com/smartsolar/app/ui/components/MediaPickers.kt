package com.smartsolar.app.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.smartsolar.app.ui.theme.SolarBorderLight
import com.smartsolar.app.ui.theme.SolarGreenLight
import com.smartsolar.app.ui.theme.SolarGreenPrimary
import com.smartsolar.app.ui.theme.SolarSurfaceSubtle
import com.smartsolar.app.ui.theme.SolarTextPrimary
import com.smartsolar.app.ui.theme.SolarTextSecondary

/**
 * Zero-permission Android Photo Picker field for single image selection
 * (complies with Google Play storage/media permissions policy).
 */
@Composable
fun SingleImagePickerField(
    label: String,
    selectedUri: Uri?,
    onImageSelected: (Uri?) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onImageSelected(uri)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = SolarSurfaceSubtle,
        border = BorderStroke(1.dp, SolarBorderLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (selectedUri != null) {
                    AsyncImage(
                        model = selectedUri,
                        contentDescription = label,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SolarGreenLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = SolarGreenPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        color = SolarTextPrimary
                    )
                    Text(
                        text = if (selectedUri != null) "تم اختيار الصورة بنجاح" else "لم يتم اختيار صورة بعد",
                        style = MaterialTheme.typography.bodySmall,
                        color = SolarTextSecondary
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectedUri != null) {
                    IconButton(onClick = { onImageSelected(null) }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إزالة الصورة",
                            tint = SolarTextSecondary
                        )
                    }
                }
                OutlinedButton(
                    onClick = {
                        launcher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.testTag(testTag)
                ) {
                    Text(if (selectedUri == null) "اختيار" else "تغيير")
                }
            }
        }
    }
}

/**
 * Zero-permission Android Photo Picker field for selecting multiple portfolio or certificate images.
 */
@Composable
fun MultiImagePickerField(
    label: String,
    selectedUris: List<Uri>,
    onImagesChanged: (List<Uri>) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImagesChanged((selectedUris + uris).distinct().take(5))
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = SolarSurfaceSubtle,
        border = BorderStroke(1.dp, SolarBorderLight)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        color = SolarTextPrimary
                    )
                    Text(
                        text = "${selectedUris.size} / 5 صور مختارة",
                        style = MaterialTheme.typography.bodySmall,
                        color = SolarTextSecondary
                    )
                }
                OutlinedButton(
                    onClick = {
                        launcher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.testTag(testTag)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة صور")
                }
            }

            if (selectedUris.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(selectedUris) { uri ->
                        Box {
                            AsyncImage(
                                model = uri,
                                contentDescription = label,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { onImagesChanged(selectedUris - uri) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "حذف",
                                    tint = SolarTextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Zero-permission document & image picker for certificates and attachments (images or PDF).
 */
@Composable
fun MultiAttachmentPickerField(
    label: String,
    selectedUris: List<Uri>,
    onAttachmentsChanged: (List<Uri>) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onAttachmentsChanged((selectedUris + uris).distinct().take(5))
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = SolarSurfaceSubtle,
        border = BorderStroke(1.dp, SolarBorderLight)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        color = SolarTextPrimary
                    )
                    Text(
                        text = "${selectedUris.size} / 5 مرفقات مختارة (صور أو PDF)",
                        style = MaterialTheme.typography.bodySmall,
                        color = SolarTextSecondary
                    )
                }
                OutlinedButton(
                    onClick = {
                        launcher.launch(arrayOf("image/*", "application/pdf"))
                    },
                    modifier = Modifier.testTag(testTag)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة مرفق")
                }
            }

            if (selectedUris.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    selectedUris.forEachIndexed { index, uri ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "مرفق #${index + 1}: ${uri.lastPathSegment ?: "ملف"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SolarTextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onAttachmentsChanged(selectedUris - uri) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "حذف المرفق",
                                    tint = SolarTextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
