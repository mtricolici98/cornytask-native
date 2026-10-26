package com.nobadhabbits.cornytask.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * True on tablets and phones in landscape (at least 600dp wide). Calendars use it to put
 * the month and the selected day's details side by side instead of stacking them.
 */
@Composable
fun isWideScreen(): Boolean = LocalConfiguration.current.screenWidthDp >= 600
