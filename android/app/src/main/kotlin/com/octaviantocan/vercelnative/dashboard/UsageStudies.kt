package com.octaviantocan.vercelnative.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.octaviantocan.vercelnative.R
import com.octaviantocan.vercelnative.dashboard.DashboardTokens as Tokens

private enum class GroupChrome { Plain, Band, Panel, Outline, RowCards }
private enum class RowLayout { UnitsUnderName, UnitsUnderCharge, Columns, Labels }
private enum class Trend { None, InlineBar, ChargeShare, Sparkline, QuietSparkline }

// The recorded samples use the same points in each of the four sparkline studies.
private data class RecordedTrend(val points: List<Pair<Int, Int>>, val color: Color, val mutedColor: Color)

private fun recordedTrend(color: Color, mutedColor: Color, vararg points: Pair<Int, Int>) =
    RecordedTrend(points.toList(), color, mutedColor)

private val recordedTrends = with(DashboardPalette.Dark) {
    mapOf(
        DashboardFixtures.edgeCpuDuration.name to recordedTrend(studySparklineBlue, studySparklineDelivery,
            0 to 11, 7 to 11, 10 to 0, 13 to 10, 19 to 11, 26 to 8, 32 to 11, 38 to 11),
        "Function Invocations" to recordedTrend(usageDailyGreen, studyFunctionsInk,
            0 to 0, 6 to 6, 11 to 8, 19 to 8, 27 to 7, 38 to 8),
        "Fluid Active CPU" to recordedTrend(studySparklinePurple, studyFunctionsInk,
            0 to 8, 8 to 8, 12 to 0, 18 to 6, 28 to 8, 38 to 7),
        "Fluid Provisioned Memory" to recordedTrend(studySparklinePink, studyFunctionsInk,
            0 to 0, 4 to 7, 12 to 10, 21 to 9, 30 to 8, 38 to 10),
        "Fast Origin Transfer" to recordedTrend(studySparklineRed, studyFunctionsInk,
            0 to 0, 5 to 6, 10 to 4, 15 to 8, 21 to 2, 27 to 6, 33 to 5, 38 to 7),
        "ISR Reads" to recordedTrend(secondary, studySparklineCaching,
            0 to 8, 7 to 8, 11 to 0, 17 to 8, 24 to 8, 30 to 5, 38 to 8),
        "Blob Advanced Operations" to recordedTrend(studySparklineGold, studySparklineCaching,
            0 to 10, 6 to 10, 11 to 3, 16 to 10, 25 to 10, 31 to 0, 38 to 10),
        "Speed Insights Data Points" to recordedTrend(studySparklineYellow, studySparklineObservability,
            0 to 11, 7 to 11, 12 to 0, 17 to 11, 24 to 11, 30 to 10, 38 to 11),
        "Web Analytics Events" to recordedTrend(studySparklineTeal, studySparklineObservability,
            0 to 0, 7 to 7, 13 to 7, 22 to 6, 30 to 7, 38 to 6),
    )
}

private data class UsageStudy(
    val chrome: GroupChrome,
    val layout: RowLayout,
    val rowHeight: Int,
    val edgeDurationHeight: Int,
    val nameStart: Int,
    val markSize: Int = 0,
    val markRadius: Int = markSize / 2,
    val headerHeight: Int = 31,
    val groupGap: Int = 10,
    val trend: Trend = Trend.None,
    val ruled: Boolean = false,
    val currencyColumn: Boolean = false,
    val centerCharge: Boolean = false,
    val largeTitle: Boolean = false,
    val coloredMarks: Boolean = false,
    val rowHeights: Map<String, Int> = emptyMap(),
)

private val wrappedColumnRows = mapOf("Fluid Provisioned Memory" to 55, "Blob Advanced Operations" to 55, "Speed Insights Data Points" to 55)

// The order matches the Figma-linked picker cases in design/dashboard.json.
private val usageStudies = listOf(
    UsageStudy(GroupChrome.Band, RowLayout.UnitsUnderName, 68, 77, 53, 30, 7), // Neo Financial
    UsageStudy(GroupChrome.Outline, RowLayout.UnitsUnderName, 67, 77, 52, 29, groupGap = 13, ruled = true, centerCharge = true), // Rocket Money
    UsageStudy(GroupChrome.Plain, RowLayout.UnitsUnderCharge, 53, 64, 45, 22, ruled = true, coloredMarks = true), // Buddy
    UsageStudy(GroupChrome.Panel, RowLayout.UnitsUnderName, 69, 77, 54, 31, headerHeight = 38, groupGap = 13), // Revolut
    UsageStudy(GroupChrome.Plain, RowLayout.UnitsUnderCharge, 54, 64, 56, 33, coloredMarks = true,
        rowHeights = mapOf("Blob Advanced Operations" to 64, "Speed Insights Data Points" to 64)), // Glow
    UsageStudy(GroupChrome.Plain, RowLayout.Columns, 47, 72, 30, 7, trend = Trend.ChargeShare,
        rowHeights = wrappedColumnRows), // Copilot Categories
    UsageStudy(GroupChrome.Band, RowLayout.Columns, 53, 55, 12, ruled = true, currencyColumn = true,
        rowHeights = wrappedColumnRows), // Fidelity
    UsageStudy(GroupChrome.Plain, RowLayout.Columns, 51, 72, 41, 18, ruled = true,
        rowHeights = wrappedColumnRows + mapOf("Function Invocations" to 55, "Web Analytics Events" to 55)), // Public
    UsageStudy(GroupChrome.Outline, RowLayout.UnitsUnderName, 63, 77, 51, 28, 5, groupGap = 13), // CRED
    UsageStudy(GroupChrome.Plain, RowLayout.UnitsUnderName, 61, 77, 12), // eBay
    UsageStudy(GroupChrome.RowCards, RowLayout.UnitsUnderCharge, 63, 63, 49, 22, 7, trend = Trend.Sparkline), // Copilot Investments
    UsageStudy(GroupChrome.Plain, RowLayout.UnitsUnderCharge, 62, 62, 54, 31, trend = Trend.QuietSparkline, ruled = true), // Yahoo Finance
    UsageStudy(GroupChrome.Plain, RowLayout.UnitsUnderCharge, 73, 73, 52, 29, trend = Trend.Sparkline, coloredMarks = true), // Coinbase
    UsageStudy(GroupChrome.Plain, RowLayout.UnitsUnderName, 75, 86, 54, 31, trend = Trend.InlineBar), // Wise
    UsageStudy(GroupChrome.Plain, RowLayout.UnitsUnderName, 69, 86, 50, 27, 6, trend = Trend.InlineBar), // YouTube Studio
    UsageStudy(GroupChrome.Outline, RowLayout.UnitsUnderName, 67, 86, 42, 19, 0, groupGap = 13, trend = Trend.InlineBar, ruled = true), // YNAB
    UsageStudy(GroupChrome.Panel, RowLayout.UnitsUnderCharge, 75, 75, 58, 35, headerHeight = 38, groupGap = 13, coloredMarks = true), // Origin
    UsageStudy(GroupChrome.Plain, RowLayout.UnitsUnderCharge, 76, 83, 59, 36, ruled = true, largeTitle = true), // Acorns
    UsageStudy(GroupChrome.Panel, RowLayout.Labels, 91, 91, 13, headerHeight = 38, groupGap = 13, trend = Trend.Sparkline), // Shopify
    UsageStudy(GroupChrome.Plain, RowLayout.Labels, 86, 86, 13, ruled = true), // Binance
)

@Composable
internal fun UsageStudyColumnHeader() {
    val ink = LocalDashboardPalette.current.secondary
    Row(Modifier.fillMaxWidth().heightIn(min = Tokens.Space20).offset(y = Tokens.DockInset).padding(horizontal = Tokens.Space12),
        horizontalArrangement = Arrangement.spacedBy(Tokens.Space6)) {
        Text("Product", Modifier.weight(1f), color = ink, fontSize = Tokens.Metadata, lineHeight = Tokens.MetadataLine)
        Text("Usage", Modifier.width(Tokens.UsageStudyQuantityColumn), color = ink,
            fontSize = Tokens.Metadata, lineHeight = Tokens.MetadataLine, textAlign = TextAlign.End)
        Text("Charge", Modifier.width(Tokens.BottomBar), color = ink,
            fontSize = Tokens.Metadata, lineHeight = Tokens.MetadataLine, textAlign = TextAlign.End)
    }
}

@Composable
internal fun UsageStudyGroup(
    index: Int, category: String, resources: List<UsageResource>, scale: Double,
    expanded: String?, toggle: (UsageResource) -> Unit,
) {
    val study = usageStudies[index]
    val palette = LocalDashboardPalette.current
    val panel = study.chrome == GroupChrome.Panel || study.chrome == GroupChrome.Outline
    val radius = if (index == 8) Tokens.DockInset else Tokens.ProjectOptionsLightRadius
    val panelShape = RoundedCornerShape(radius)
    Column(Modifier.fillMaxWidth().padding(bottom = study.groupGap.dp)) {
        Column(
            Modifier.fillMaxWidth()
                .then(if (panel) Modifier.clip(panelShape).background(palette.studyPanel) else Modifier)
                .then(if (study.chrome == GroupChrome.Outline) Modifier.border(Tokens.Stroke, palette.quietBorder, panelShape) else Modifier),
        ) {
            val headingInset = when {
                panel -> Tokens.ProjectOptionsInset
                study.chrome == GroupChrome.Band -> Tokens.PreviewStatusInset + Tokens.Stroke
                else -> Tokens.Space2
            }
            Text(
                category,
                Modifier.fillMaxWidth()
                    .then(if (study.chrome == GroupChrome.Band) Modifier.background(palette.studyCategory, RoundedCornerShape(Tokens.Space4)) else Modifier)
                    .heightIn(min = study.headerHeight.dp).padding(start = headingInset, top = if (study.headerHeight == 38) Tokens.Space12 else Tokens.Space6),
                color = if (panel) palette.muted else palette.secondary,
                fontSize = if (index == 8) Tokens.Caption else Tokens.Metadata,
                fontWeight = FontWeight.Medium, lineHeight = Tokens.MetadataLine,
            )
            resources.forEachIndexed { position, resource ->
                val quantity = resource.quantity(scale)
                val charge = sampleMoney(resource.cost * scale)
                StudyResourceRow(study, index, category, resource, quantity, charge, study.ruled && position < resources.lastIndex) { toggle(resource) }
                if (expanded == resource.id) {
                    KeyValueRow("Recorded usage", quantity)
                    KeyValueRow("Estimated charge", charge)
                }
                if (study.chrome == GroupChrome.RowCards && position < resources.lastIndex) {
                    Spacer(Modifier.height(Tokens.ProjectDetailGap))
                }
            }
            if (panel) Spacer(Modifier.height(Tokens.Space6))
            if (study.chrome == GroupChrome.RowCards) Spacer(Modifier.height(Tokens.ProjectDetailGap))
        }
    }
}

@Composable
private fun StudyResourceRow(
    study: UsageStudy, index: Int, category: String, resource: UsageResource, quantity: String, charge: String,
    divided: Boolean, onOpen: () -> Unit,
) {
    val palette = LocalDashboardPalette.current
    val cards = study.chrome == GroupChrome.RowCards
    val sparkline = study.trend == Trend.Sparkline || study.trend == Trend.QuietSparkline
    val valueStart = (study.nameStart - if (cards) 4 else 0).dp
    val nameSize = when {
        study.largeTitle -> Tokens.Body
        study.layout == RowLayout.Columns || (sparkline && study.layout != RowLayout.Labels) -> Tokens.SearchDetailSize
        else -> Tokens.ListSubtitle
    }
    val height = study.rowHeights[resource.name] ?: if (resource.id == DashboardFixtures.edgeCpuDuration.id) {
        study.edgeDurationHeight
    } else study.rowHeight
    val scaledHeight = height.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
    val recorded = recordedTrends[resource.name]
    val shape = RoundedCornerShape(Tokens.ProjectOptionsInset)
    BoxWithConstraints(
        Modifier.fillMaxWidth().padding(horizontal = if (cards) Tokens.Space4 else Dp.Hairline)
            .then(if (cards) Modifier.clip(shape).background(palette.studyPanel).border(Tokens.Stroke, palette.quietBorder, shape) else Modifier)
            .clickable(onClick = onOpen).heightIn(min = scaledHeight),
    ) {
        if (study.markSize > 0) {
            Box(Modifier.align(Alignment.CenterStart).padding(start = Tokens.Space12 - Tokens.Stroke)) {
                StudyResourceMark(study, index, category)
            }
        }
        val trendInk = if (category == "Vercel Delivery Network") palette.studyDeliveryInk else when (resource.name) {
            "Fluid Active CPU" -> palette.studyCpuInk
            "Fluid Provisioned Memory" -> palette.studyMemoryInk
            else -> palette.studyFunctionsInk
        }
        when (study.layout) {
            RowLayout.UnitsUnderName -> {
                Column(Modifier.align(Alignment.TopStart).padding(start = valueStart, top = Tokens.PreviewStatusInset + Tokens.Stroke,
                    end = Tokens.UsageStudyChargeColumn + Tokens.Space8),
                    verticalArrangement = Arrangement.spacedBy(Tokens.Space4)) {
                    Text(resource.name, fontSize = nameSize, lineHeight = Tokens.ProjectSectionLine, fontWeight = FontWeight.Medium)
                    Text(quantity, color = palette.secondary, fontSize = Tokens.Metadata, lineHeight = Tokens.SearchDetailLine)
                    if (study.trend == Trend.InlineBar) StudyChargeBar(resource, Tokens.UsageStudyInlineBar, trendInk)
                }
                Text(charge,
                    Modifier.align(if (study.centerCharge) Alignment.CenterEnd else Alignment.TopEnd)
                        .padding(end = Tokens.Space12, top = if (study.centerCharge) Dp.Hairline else Tokens.PreviewStatusInset + Tokens.Stroke),
                    fontSize = Tokens.ListSubtitle, lineHeight = Tokens.ProjectSectionLine, fontWeight = FontWeight.SemiBold)
            }
            RowLayout.UnitsUnderCharge -> {
                Text(resource.name, Modifier.align(if (sparkline) Alignment.TopStart else Alignment.CenterStart)
                    .padding(start = valueStart, end = if (sparkline) Tokens.UsageStudyChargeColumn else Tokens.UsageStudyChargeStack,
                        top = if (sparkline) Tokens.PreviewStatusInset + Tokens.Stroke else Dp.Hairline)
                    .widthIn(max = if (sparkline) (maxWidth - Tokens.UsageStudyChartColumn - valueStart).coerceAtLeast(Tokens.Touch) else maxWidth),
                    fontSize = nameSize, lineHeight = if (sparkline) Tokens.UsageStudyValueLine else Tokens.ProjectSectionLine, fontWeight = FontWeight.Medium)
                if (sparkline) {
                    Box(Modifier.align(Alignment.CenterEnd).padding(end = Tokens.UsageStudyChargeColumn + Tokens.Space8 + Tokens.Stroke)) {
                        if (recorded != null) RecordedUsageTrend(recorded, colorful = index == 10 || study.trend == Trend.QuietSparkline)
                        else if (study.trend == Trend.QuietSparkline) {
                            Box(Modifier.width(Tokens.UsageStudyChartWidth).height(Tokens.Stroke).background(palette.studyRule))
                        }
                    }
                }
                Column(Modifier.align(Alignment.CenterEnd).padding(end = Tokens.Space12),
                    horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Tokens.Space4)) {
                    Text(charge, fontSize = if (study.largeTitle) Tokens.Body else Tokens.ListSubtitle,
                        lineHeight = Tokens.ProjectSectionLine, fontWeight = FontWeight.SemiBold)
                    Text(quantity, color = palette.secondary, fontSize = if (study.trend == Trend.None) Tokens.Metadata else Tokens.Caption,
                        lineHeight = Tokens.SearchDetailLine)
                }
            }
            RowLayout.Columns -> {
                Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().heightIn(min = scaledHeight).padding(start = valueStart, end = Tokens.Space12),
                    horizontalArrangement = Arrangement.spacedBy(Tokens.Space8), verticalAlignment = Alignment.CenterVertically) {
                    Text(resource.name, Modifier.weight(1f).align(Alignment.Top).padding(top = Tokens.PreviewStatusInset + Tokens.Stroke), fontSize = Tokens.SearchDetailSize,
                        lineHeight = Tokens.SearchDetailLine, fontWeight = FontWeight.Medium)
                    Text(quantity, Modifier.width(Tokens.UsageStudyQuantityColumn), fontFamily = GeistMono, fontSize = Tokens.Caption,
                        color = palette.secondary, textAlign = TextAlign.End)
                    Text(charge, Modifier.width(Tokens.BottomBar), fontFamily = GeistMono, fontSize = Tokens.SearchDetailSize,
                        textAlign = TextAlign.End)
                }
                if (study.currencyColumn) {
                    Box(Modifier.align(Alignment.Center).offset(x = -(Tokens.Space8 + Tokens.Space4))
                        .height(scaledHeight).width(Tokens.Stroke).background(palette.studyRule))
                }
                if (study.trend == Trend.ChargeShare) {
                    Box(Modifier.align(Alignment.BottomStart).offset(x = maxWidth * .445f).padding(bottom = Tokens.Space4)) {
                        StudyChargeBar(resource, Tokens.Touch, trendInk)
                    }
                }
            }
            RowLayout.Labels -> {
                Column(Modifier.fillMaxWidth().padding(horizontal = Tokens.ProjectOptionsInset, vertical = Tokens.Space8)) {
                    Text(resource.name, fontSize = nameSize, lineHeight = Tokens.ProjectSectionLine, fontWeight = FontWeight.Medium)
                    Row(Modifier.fillMaxWidth().padding(top = Tokens.Space4), verticalAlignment = Alignment.CenterVertically) {
                        Text("Usage", Modifier.width(Tokens.UsageStudySparklineLabel), color = palette.secondary,
                            fontSize = Tokens.Metadata, lineHeight = Tokens.ListSubtitleLine)
                        if (study.trend == Trend.Sparkline && recorded != null) {
                            Box(Modifier.width(Tokens.UsageStudySparklineSlot), contentAlignment = Alignment.Center) {
                                RecordedUsageTrend(recorded, fixedHeight = true)
                            }
                        }
                        Text(quantity, Modifier.weight(1f), fontFamily = GeistMono, fontSize = Tokens.SearchDetailSize, textAlign = TextAlign.End)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Charge", color = palette.secondary, fontSize = Tokens.Metadata, lineHeight = Tokens.ListSubtitleLine)
                        Text(charge, fontFamily = GeistMono, fontSize = Tokens.SearchDetailSize)
                    }
                }
            }
        }
        if (divided) DividerLine(Modifier.align(Alignment.BottomCenter).padding(horizontal = Tokens.Space12), palette.studyRule)
    }
}

@Composable
internal fun UsageSparkline(resource: UsageResource) {
    val palette = LocalDashboardPalette.current
    val trend = recordedTrends[resource.name] ?: RecordedTrend(
        listOf(0 to 0, 38 to 0),
        when (resource.name) {
            "Fast Data Transfer" -> palette.studySparklinePink
            "Edge Requests", "Pro" -> palette.studySparklineBlue
            else -> palette.secondary
        },
        palette.secondary,
    )
    Box(Modifier.width(Tokens.UsageSparklineWidth), contentAlignment = Alignment.Center) {
        RecordedUsageTrend(trend, colorful = true, width = Tokens.UsageTrendWidth)
    }
}

@Composable
private fun RecordedUsageTrend(
    trend: RecordedTrend, colorful: Boolean = false, fixedHeight: Boolean = false, width: Dp = Tokens.UsageStudyChartWidth,
) {
    val peak = trend.points.maxOf { it.second }
    val unit = if (fixedHeight) Tokens.ProjectOptionsInset / peak else width / 38
    val palette = LocalDashboardPalette.current
    val ink = if (palette == DashboardPalette.Light) palette.link else if (colorful) trend.color else trend.mutedColor
    Canvas(Modifier.size(unit * 38, (unit * peak).coerceAtLeast(Tokens.Space2))) {
        val scale = unit.toPx()
        val top = (size.height - peak * scale) / 2
        val path = Path()
        trend.points.forEachIndexed { index, (x, y) ->
            if (index == 0) path.moveTo(x * scale, top + y * scale) else path.lineTo(x * scale, top + y * scale)
        }
        drawPath(path, ink, style = Stroke(scale * 1.2f))
    }
}

@Composable
private fun StudyResourceMark(study: UsageStudy, index: Int, category: String) {
    val palette = LocalDashboardPalette.current
    val size = study.markSize.dp
    val (artwork, paper) = when (category) {
        "Vercel Delivery Network" -> R.drawable.dashboard_figma_usage_delivery to palette.studyDeliveryPaper
        "Vercel Functions" -> R.drawable.dashboard_figma_usage_functions to palette.studyFunctionsPaper
        "Content, Caching & Optimization" -> Glyph.Clock.artwork to palette.studyCachingPaper
        "Observability" -> R.drawable.dashboard_figma_usage_chart to palette.studyObservabilityPaper
        "Build & Deploy" -> Glyph.Clock.artwork to palette.studyBuildPaper
        else -> R.drawable.dashboard_figma_scope_team to palette.studySubscriptionPaper
    }
    val ink = if (!study.coloredMarks) palette.studyDeliveryInk else when (category) {
        "Vercel Delivery Network" -> palette.studySparklineDelivery
        "Vercel Functions" -> palette.studyFunctionsInk
        "Content, Caching & Optimization" -> palette.studySparklineCaching
        "Observability" -> palette.studySparklineObservability
        "Build & Deploy" -> palette.studyBuildInk
        else -> palette.studySubscriptionInk
    }
    if (study.markSize == 7) {
        Box(Modifier.size(size).background(ink, CircleShape))
        return
    }
    val shape = RoundedCornerShape(study.markRadius.dp)
    Box(
        Modifier.size(size)
            .then(when (index) {
                8 -> Modifier.background(palette.accountCanvas, shape).border(Tokens.Stroke, palette.insetSurface, shape)
                15 -> Modifier
                else -> Modifier.background(paper, shape)
            }),
        contentAlignment = Alignment.Center,
    ) {
        val artworkSize = when (index) {
            7 -> Tokens.Space12
            15 -> size
            else -> if (study.markSize <= 28) Tokens.MetadataIcon else Tokens.DockIcon
        }
        DashboardArtwork(artwork, artworkSize, ink)
    }
}

@Composable
private fun StudyChargeBar(resource: UsageResource, width: Dp, ink: Color) {
    Box(Modifier.width(width).height(Tokens.Space4).background(LocalDashboardPalette.current.studyTrack, CircleShape)) {
        if (resource.cost > 0) {
            Box(Modifier.fillMaxWidth((resource.cost / .65).toFloat().coerceIn(.10f, 1f)).fillMaxHeight().background(ink, CircleShape))
        }
    }
}
