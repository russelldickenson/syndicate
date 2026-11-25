package com.zigzag.syndicate.core.database.migrations

import androidx.room3.migration.Migration
import androidx.sqlite.execSQL

/** Adds the per-article favourite flag used by the Favourites category. */
val MIGRATION_2_3 = Migration(2, 3) { connection ->
    connection.execSQL("ALTER TABLE articles ADD COLUMN isFavourite INTEGER NOT NULL DEFAULT 0")
    connection.execSQL(
        "CREATE INDEX IF NOT EXISTS index_articles_isFavourite ON articles (isFavourite)",
    )
}
