# Vercel Native designs in Compose

In a debug build, open **Browse design states** to find a screen by its Figma frame ID. The manifest links each case to its Compose function and the reusable Figma layout. Approved frames also carry a Kotlin source link in their annotations.

Rotating the phone preserves the open preview and its sample changes. A transferred project's alerts follow it in the inbox and search. Each workspace has its own Agent setup, and Retry runs setup even when the failure screen was opened from the design picker.

The Projects title and sidebar button stay above the list while it scrolls, following the November 22 phone review. The layout uses the approved row geometry, dock, and sidebar. The app applies Android system bar insets and uses fictional project and workspace identities. [Projects](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=402-4957) · [Sidebar](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=228-2849) · [Workspace switcher](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=513-2132)

The project popover follows its ellipsis and fits on either side of the trigger when space is tight. The dark menu uses leading icons, and the light menu uses the trailing favorite glyph from the approved light frame. [Dark menu](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=604-3584) · [Light menu](https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=604-4911)

Dashboard icons come from the linked frames. The Geist font and its license are bundled with the app. Where Figma sets a value directly on a layer, `figmaFrame` in the token manifest points to the frame to review. [Geist license](https://github.com/google/fonts/blob/main/ofl/geist/OFL.txt)

## Review a new Figma change

1. Load `tools/design/figma_snapshot.js` into Figma MCP's `use_figma` tool. Append a call to `snapshotDashboard` with `figmaFileKey` and `approvedPage` from the manifest. Pass `cases[].id` as `frameIds`, `components[].figmaId` as `componentIds`, and the `figmaId` values from the colors, dimensions and type tokens as `tokenIds`.
2. Save the returned JSON and compare it with the approved snapshot. If you read the frames in batches, merge the `frames`, `components` and `variables` objects before comparing.

   ```sh
   python3 -B tools/design/check_dashboard_design.py --compare /path/to/figma-snapshot.json
   ```

3. Review the reported Figma frames and Android cases. Update the Compose values and the saved snapshot once you accept the changes. Regenerate the Kotlin tokens and state picker with `python3 -B tools/design/check_dashboard_design.py --generate`.

The Android verification job checks the committed mappings and token values. A fresh Figma snapshot supplies the live design data for comparison, including Compose tokens with no references in the reviewed frames.

## Publish the Compose templates

`figma.config.json` and `design/code-connect/` contain the Code Connect configuration and templates. The approved frames' Kotlin annotations are available while the templates await publication.
