/*
 * Copyright (C) 2025 O﻿ute﻿rTu﻿ne Project
 *
 * SPDX-License-Identifier: GPL-3.0
 *
 * For any other attributions, refer to the git commit history
 */

package com.dd3boh.outertune.utils.scanners

import android.os.ParcelFileDescriptor
import android.util.Log
import com.dd3boh.outertune.constants.DEBUG_SAVE_OUTPUT
import com.dd3boh.outertune.constants.EXTRACTOR_DEBUG
import com.dd3boh.outertune.constants.SCANNER_DEBUG
import com.dd3boh.outertune.db.entities.AlbumEntity
import com.dd3boh.outertune.db.entities.ArtistEntity
import com.dd3boh.outertune.db.entities.FormatEntity
import com.dd3boh.outertune.db.entities.GenreEntity
import com.dd3boh.outertune.db.entities.Song
import com.dd3boh.outertune.db.entities.SongEntity
import com.dd3boh.outertune.models.SongTempData
import com.dd3boh.outertune.ui.utils.ARTIST_SEPARATORS
import com.dd3boh.outertune.ui.utils.EXTRACTOR_TAG

import java.io.File
import java.lang.Integer.parseInt
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset


class TagLibScanner : MetadataScanner {

    /**
     * Given a path to a file, extract necessary metadata.
     *
     * @param file Full file path
     */
    override suspend fun getAllMetadataFromFile(file: File): SongTempData {
        if (EXTRACTOR_DEBUG)
            Log.v(EXTRACTOR_TAG, "Starting Full Extractor session on: ${file.absolutePath}")

        return SongTempData(
            Song(
                song = SongEntity(
                    id = SongEntity.generateSongId(),
                    title = "title",
                    thumbnailUrl = file.absolutePath,
                    isLocal = true,
                    inLibrary = LocalDateTime.now(),
                    localPath = file.absolutePath
                ),
                artists = emptyList(),

            ),
            null
        )
    }
}