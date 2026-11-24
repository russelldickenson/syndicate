package com.zigzag.rssreader.core.database.migrations

import androidx.room3.migration.Migration
import androidx.sqlite.execSQL

/**
 * Tags (many-to-many) become folders (one per feed). Each feed keeps the
 * first of its former tags by sort order as its folder; the rest are dropped.
 */
val MIGRATION_1_2 = Migration(1, 2) { connection ->
    connection.execSQL("ALTER TABLE feeds ADD COLUMN folderId TEXT")
    connection.execSQL(
        "CREATE TABLE IF NOT EXISTS `folders` (" +
            "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, " +
            "PRIMARY KEY(`id`))",
    )
    connection.execSQL(
        "INSERT INTO `folders` (`id`, `name`, `sortOrder`) " +
            "SELECT `id`, `name`, `sortOrder` FROM `tags`",
    )
    connection.execSQL(
        "UPDATE `feeds` SET `folderId` = (" +
            "SELECT ft.`tagId` FROM `feed_tags` ft " +
            "INNER JOIN `tags` t ON t.`id` = ft.`tagId` " +
            "WHERE ft.`feedId` = `feeds`.`id` " +
            "ORDER BY t.`sortOrder` ASC, t.`name` ASC, ft.`tagId` ASC LIMIT 1)",
    )
    connection.execSQL("DROP TABLE `feed_tags`")
    connection.execSQL("DROP TABLE `tags`")
}
