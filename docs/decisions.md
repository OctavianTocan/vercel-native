# Setup decisions

2026-11-11 — Tavi: Kotlin/Compose; public repo; mandatory updates; CI on mini-002-netcup; existing Inventory session; generic shared download page; independent publisher/artifacts/feed. No Vercel-specific screens or backend in the initial milestone.

The working name and minimal monochrome shell are provisional implementation choices, not approved dashboard design.

## November 22 phone review

The Projects title and sidebar button stay visible while the list scrolls. Use the approved row, dock, and sidebar dimensions with Android system insets. Open the project actions next to the tapped ellipsis, including on a scrolled row. [Projects](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=402-4957) · [Sidebar](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=228-2849) · [Project options](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=604-3584)

## Registered screenshots for the dashboard review

The 393 px Figma exports and 393 dp running app exposed a 35 px vertical offset in the light Project Options preview and a 1 px height error on each Recents row. The light menu now aligns its top with the ellipsis control as drawn. Recents uses the approved 14 px gap under the tabs and includes the divider in its 96 px row. The icon-only Projects frame uses its original tiles and chevrons. [Light menu](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=604-4911) · [Recents](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=225-2) · [Icon-only Projects](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=228-2750)

The follow-up verification confirmed that the light menu closes when tapped in its shadow. Favorite changes appear in the design preview after the action. The workspace menu uses the Android system bar and display cutout insets.

## November 23 visual review

Use Inventory’s progressive scroll-edge blur and motion durations and easing in the dashboard. The dock and Search controls use a live Android shader for curved refraction, color dispersion, and directional highlights. Android 12L uses live blur and tint because it predates Android’s runtime shader API.

The dashboard uses Geist and Geist Mono, with native vector paths exported from approved Figma nodes. Search opens its focused composer over the native keyboard and uses the approved six-row result layout. Alerts show the three-line inbox rows and their timestamps, the skeleton inbox, and the approved art for empty and plan states. The shared 64 dp dock uses the approved inset, optical glyph placement, and gap to Search.

[Alerts inbox](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=225-257) · [Search](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=513-1112) · [Profile](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=344-494) · [Project overview](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=535-3158)

## November 23 preview behavior

Use the alert’s project subtitle to open its project and place Visit with Toolbar in the preview opened from Project Overview. Keep the Search composer and Assistant reachable when the keyboard reduces the viewport, and show an unavailable status for Environment Variables and Storage. Usage chart periods group the same billing-cycle charges.

[Function alert](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=587-1267) · [Toolbar unavailable](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=552-2311) · [Global Search](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=513-1112) · [Weekly Usage](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=587-484)

## November 23 dashboard verification

Use the approved outline gear, Ready badge and project details groups on the Project Sheet. Keep Android’s status bar clear on short displays and retain the parent sheet underneath Domains and GitHub sync. Use the prototype’s dissolve between root destinations, push for details, and directional entries for Search and the workspace rail.

The Usage studies open at the resource table in the debug picker. Each uses the group, icon and chart treatment in its approved frame. The same fictional records appear across the studies so the layout differences are easy to compare.

[Project Sheet](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=535-3158) · [Project Domains](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=604-938) · [Usage studies](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=606-535)

Keep the DNS, deployment preview and transfer actions visible above Android’s navigation area while their sheet bodies scroll. Use the approved 41 dp grip and apply the sheet’s grouped row spacing across each stage. The Profile form uses 46 dp fields and 14 dp between cards.

The dashboard’s glass lens uses the Figma control depth, refraction and rim. The Ready badge uses its own shallower optics and samples the deployment preview beneath it. Search leaves in 220 ms and detail Back uses a 240 ms push.

[DNS verification](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=604-1084) · [Protected preview](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=586-274) · [Review transfer](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=535-1457) · [Profile Settings](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=344-494)

Reuse the Recents row for removal confirmation, including the paired artwork, one-line title and GitHub metadata. A transfer review opens with a 240 ms push and returns to the selected team.

[Recents removal confirmation](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=604-3882) · [Transfer review](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=535-1457)

## November 23 gesture review

Use Pully's row gesture for Recents. Swipe right to Preview and left to open Remove from Recents. Keep both actions below a divider in the menu. The Projects ellipsis lights up when its menu opens, and the workspace sidebar follows horizontal drags.

Keep the Activity tabs visible. Page content and the indicator follow the same horizontal drag on Activity and on the Project Sheet. A drag on a Project Sheet deployment row moves the tabs. The dock starts with icons, reveals the tapped label for 1.5 seconds, and lets an Activity flick or held lean step through the three tabs.

Use Inventory's sheet position and nested scroll behavior. Keep Project Overview tall on arrival and give it a halfway stop. Other sheets settle to their content up to halfway, with room to drag higher. The previous screen scales and dims as a sheet opens. Keep the approved sheet surfaces, row proportions, and nesting. Use the Menu dropdown recipe for popover motion. [transitions.dev library](https://transitions.dev/library.html)
