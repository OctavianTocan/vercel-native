# Setup decisions

2026-11-11 — Tavi: Kotlin/Compose; public repo; mandatory updates; CI on mini-002-netcup; existing Inventory session; generic shared download page; independent publisher/artifacts/feed. No Vercel-specific screens or backend in the initial milestone.

The working name and minimal monochrome shell are provisional implementation choices, not approved dashboard design.

## November 22 phone review

The Projects title and sidebar button stay visible while the list scrolls. Use the approved row, dock, and sidebar dimensions with Android system insets. Open the project actions next to the tapped ellipsis, including on a scrolled row. [Projects](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=402-4957) · [Sidebar](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=228-2849) · [Project options](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=604-3584)

## Registered screenshots for the dashboard review

The 393 px Figma exports and 393 dp running app exposed a 35 px vertical offset in the light Project Options preview and a 1 px height error on each Recents row. The light menu now aligns its top with the ellipsis control as drawn. Recents uses the approved 14 px gap under the tabs and includes the divider in its 96 px row. The icon-only Projects frame uses its original tiles and chevrons. [Light menu](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=604-4911) · [Recents](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=225-2) · [Icon-only Projects](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=228-2750)

The follow-up verification confirmed that the light menu closes when tapped in its shadow. Favorite changes appear in the design preview after the action. The workspace menu uses the Android system bar and display cutout insets.
