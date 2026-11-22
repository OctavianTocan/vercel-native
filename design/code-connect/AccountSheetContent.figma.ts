// url=https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=661-9048
// source=android/app/src/main/kotlin/com/octaviantocan/vercelnative/dashboard/AccountScreen.kt
// component=AccountSheetContent
import figma from 'figma'

const route = figma.selectedInstance.getEnum('State', {
  "Profile photo preview": "DashboardRoute(Destination.Photo, frozen = true)",
  "Default team picker": "DashboardRoute(Destination.DefaultScope, frozen = true)",
  "Theme picker": "DashboardRoute(Destination.Theme, frozen = true)",
})

export default {
  example: figma.code`AccountSheetContent(route = ${route}, navigation = navigation, onBack = onBack)`,
  id: 'vercel-661-9048',
}

