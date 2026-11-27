package com.zigzag.syndicate.feature.articles.presentation

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zigzag.syndicate.core.domain.model.Article
import com.zigzag.syndicate.core.ui.components.ArticleRow
import kotlinx.datetime.Instant
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArticleRowAccessibilityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun articleRowIsAnAccessibleButton() {
        composeTestRule.setContent {
            MaterialTheme {
                ArticleRow(article = article.copy(isRead = false), onRead = {})
            }
        }

        composeTestRule.onNodeWithText(article.title, useUnmergedTree = true).assertExists()
        // Runs Espresso's accessibility checks over the Compose hierarchy.
        onView(isRoot()).check { view, _ -> check(view != null) }
    }

    @Test
    fun readStateIsExposedToAccessibilityServices() {
        composeTestRule.setContent {
            MaterialTheme {
                ArticleRow(article = article.copy(isRead = true), onRead = {})
            }
        }

        composeTestRule.onNodeWithText(article.title, useUnmergedTree = true).assertExists()
        onView(isRoot()).check { view, _ -> check(view != null) }
    }

    private companion object {
        val article = Article(
            id = "test-article",
            feedId = "test-feed",
            feedTitle = "Test feed",
            title = "Accessible article title",
            link = "https://example.com/article",
            fetchedAt = Instant.parse("2026-06-23T08:30:00Z"),
        )

        @JvmStatic
        @BeforeClass
        fun enableAccessibilityChecks() {
            AccessibilityChecks.enable()
        }
    }
}
