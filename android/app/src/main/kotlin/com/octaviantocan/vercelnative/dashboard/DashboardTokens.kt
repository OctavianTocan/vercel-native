// Generated from design/dashboard.json by tools/design/check_dashboard_design.py.
package com.octaviantocan.vercelnative.dashboard

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object DashboardTokens {
    val Space2 = 2.dp
    val Space4 = 4.dp
    val Space6 = 6.dp
    val Space8 = 8.dp
    val Space12 = 12.dp
    val Inset = 16.dp
    val Space20 = 20.dp
    val Space24 = 24.dp
    val Space32 = 32.dp
    val Icon = 22.dp
    val IconSlot = 32.dp
    val RowIcon = 36.dp
    val Filter = 44.dp
    val Touch = 48.dp
    val SearchRow = 54.dp
    val MenuRow = 44.dp
    val ProjectListRow = 76.dp
    val RecentDeploymentRow = 96.dp
    val ProjectDeploymentRow = 78.dp
    val MetadataIcon = 14.dp
    val TopBar = 56.dp
    val BottomBar = 64.dp
    val CompactBar = 56.dp
    val Stroke = 1.dp
    val SelectedUnderline = 2.dp
    val SmallRadius = 8.dp
    val ControlRadius = 12.dp
    val CardRadius = 18.dp
    val SheetRadius = 28.dp
    val PillRadius = 32.dp
    val ChartHeight = 126.dp
    val PreviewHeight = 158.dp
    val AvatarSize = 80.dp
    val IllustrationWidth = 156.dp
    val IllustrationHeight = 120.dp
    val SearchResultsHeight = 428.dp
    val SheetMaxHeight = 734.dp
    val CompactSheetMaxHeight = 490.dp
    val DialogWidth = 340.dp
    val ProjectOptionsRadius = 17.dp
    val ProjectOptionsLightRadius = 16.dp
    val WorkspaceSelectionRadius = 1.5.dp
    val ProjectSectionHeight = 38.dp
    val ProjectBadgeGap = 14.dp
    val DockIcon = 18.dp
    val DockInset = 5.dp
    val DockBottomGap = 10.dp
    val DockScrollClearance = 90.dp
    val DockLabelGap = 3.dp
    val ProjectOptionsWidth = 247.dp
    val ProjectOptionsIcon = 18.dp
    val ProjectOptionsInset = 13.dp
    val ProjectOptionsLightWidth = 236.dp
    val ProjectOptionsLightRow = 46.dp
    val WorkspaceRailWidth = 81.dp
    val WorkspacePanelWidth = 260.dp
    val WorkspacePanelTop = 72.dp
    val WorkspaceRowInset = 14.dp
    val WorkspaceBadgeSize = 49.dp
    val WorkspaceBadgeTop = 23.dp
    val WorkspaceBadgeGap = 21.dp
    val WorkspaceSelectionWidth = 3.dp
    val WorkspaceSelectionHeight = 25.dp
    val Caption = 11.sp
    val Metadata = 12.sp
    val Body = 15.sp
    val Title = 17.sp
    val Heading = 22.sp
    val Metric = 26.sp
    val Large = 29.sp
    val ListTitle = 16.sp
    val ListSubtitle = 14.sp
    val RootTitle = 18.sp
    val ListTitleLine = 24.sp
    val ListSubtitleLine = 20.sp
    val ProjectSectionLine = 19.sp
    val ProjectOptionsLine = 21.sp
    const val ScreenFadeMillis = 150
    const val ScrimOpacity = 0.62f
    const val ProjectSheetFraction = 0.98f
    const val SidebarScrimOpacity = 0.55f
}

internal data class DashboardPalette(
    val canvas: Color,
    val event: Color,
    val input: Color,
    val control: Color,
    val sheet: Color,
    val popover: Color,
    val group: Color,
    val selected: Color,
    val border: Color,
    val quietBorder: Color,
    val ink: Color,
    val secondary: Color,
    val muted: Color,
    val disabled: Color,
    val link: Color,
    val error: Color,
    val errorSurface: Color,
    val warning: Color,
    val warningSurface: Color,
    val ready: Color,
    val readySurface: Color,
    val chart: Color,
    val badge: Color,
    val glassTint: Color,
    val glassInk: Color,
    val glassSelection: Color,
    val sidebarPanel: Color,
    val insetSurface: Color,
    val projectBadge: Color,
    val projectEllipsis: Color,
    val projectOptionsSurface: Color,
    val projectOptionsInk: Color,
    val projectOptionsGlyph: Color,
    val projectOptionsBorder: Color,
    val workspaceRail: Color,
) {
    companion object {
        val Dark = DashboardPalette(
            canvas = Color(0xFF0A0A0A),
            event = Color(0xFF0E0E0E),
            input = Color(0xFF141414),
            control = Color(0xFF151515),
            sheet = Color(0xFF171717),
            popover = Color(0xFF1B1B1B),
            group = Color(0xFF1F1F1F),
            selected = Color(0xFF252525),
            border = Color(0xFF343434),
            quietBorder = Color(0xFF242424),
            ink = Color(0xFFEDEDED),
            secondary = Color(0xFFA1A1A1),
            muted = Color(0xFFB8B8B8),
            disabled = Color(0xFF777777),
            link = Color(0xFFA2C7FF),
            error = Color(0xFFFF797F),
            errorSurface = Color(0xFF241517),
            warning = Color(0xFFEEB764),
            warningSurface = Color(0xFF221A10),
            ready = Color(0xFFA7DD96),
            readySurface = Color(0xFF294637),
            chart = Color(0xFF9AB5DB),
            badge = Color(0xFF263953),
            glassTint = Color(0x57171718),
            glassInk = Color(0xFFF5F5F0),
            glassSelection = Color(0x26FFFFFF),
            sidebarPanel = Color(0xF00A0A0A),
            insetSurface = Color(0xFF303030),
            projectBadge = Color(0xFF202020),
            projectEllipsis = Color(0xFFCFCFCF),
            projectOptionsSurface = Color(0xFF181818),
            projectOptionsInk = Color(0xFFF1F1F1),
            projectOptionsGlyph = Color(0xFFACACAC),
            projectOptionsBorder = Color(0xFF303030),
            workspaceRail = Color(0xDC131313),
        )

        val Light = DashboardPalette(
            canvas = Color(0xFFFFFFFF),
            event = Color(0xFFF5F5F5),
            input = Color(0xFFF5F5F5),
            control = Color(0xFFEDEDED),
            sheet = Color(0xFFFFFFFF),
            popover = Color(0xFFFFFFFF),
            group = Color(0xFFF4F4F4),
            selected = Color(0xFFEAEAEA),
            border = Color(0xFFD6D6D6),
            quietBorder = Color(0xFFE7E7E7),
            ink = Color(0xFF171717),
            secondary = Color(0xFF525252),
            muted = Color(0xFF606060),
            disabled = Color(0xFF757575),
            link = Color(0xFF24548D),
            error = Color(0xFFB51E28),
            errorSurface = Color(0xFFFCE9E8),
            warning = Color(0xFF825108),
            warningSurface = Color(0xFFFDF2DE),
            ready = Color(0xFF22663A),
            readySurface = Color(0xFFE5F4E8),
            chart = Color(0xFF415E8A),
            badge = Color(0xFFDDE8F5),
            glassTint = Color(0x6BFFFFFF),
            glassInk = Color(0xFF121213),
            glassSelection = Color(0x12000000),
            sidebarPanel = Color(0xF0FFFFFF),
            insetSurface = Color(0xFFE8E8E8),
            projectBadge = Color(0xFFEDEDED),
            projectEllipsis = Color(0xFF666666),
            projectOptionsSurface = Color(0xFFFCFCFC),
            projectOptionsInk = Color(0xFF171717),
            projectOptionsGlyph = Color(0xFF666666),
            projectOptionsBorder = Color(0xFFE5E5E5),
            workspaceRail = Color(0xDCF5F5F5),
        )
    }
}
