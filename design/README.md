# Vercel Native designs in Compose

In a debug build, open **Browse design states** to find a screen by its Figma frame ID. The manifest links each case to its Compose function and the reusable Figma layout. Approved frames also carry a Kotlin source link in their annotations.

## Review a new Figma change

1. Load `tools/design/figma_snapshot.js` into Figma MCP's `use_figma` tool. Append a call to `snapshotDashboard` with `figmaFileKey` and `approvedPage` from the manifest. Pass `cases[].id` as `frameIds`, `components[].figmaId` as `componentIds`, and the `figmaId` values from the colors, dimensions and type tokens as `tokenIds`.
2. Save the returned JSON and compare it with the approved snapshot. If you read the frames in batches, merge the `frames`, `components` and `variables` objects before comparing.

   ```sh
   python3 -B tools/design/check_dashboard_design.py --compare /path/to/figma-snapshot.json
   ```

3. Review the reported Figma frames and Android cases. Update the Compose values and the saved snapshot once you accept the changes. Regenerate the Kotlin tokens and state picker with `python3 -B tools/design/check_dashboard_design.py --generate`.

The Android verification job checks the committed mappings and token values. A fresh Figma snapshot supplies the live design data for comparison.

## Publish the Compose templates

`figma.config.json` and `design/code-connect/` contain the Code Connect configuration and templates. The approved frames' Kotlin annotations are available while the templates await publication.
