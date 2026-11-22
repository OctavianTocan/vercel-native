// url=https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=660-7350
// source=android/app/src/main/kotlin/com/octaviantocan/vercelnative/dashboard/ActivityScreen.kt
// component=AlertScreen
import figma from 'figma'

const route = figma.selectedInstance.getEnum('State', {
  "Function error alert": "DashboardRoute(Destination.Alert, target = \"alt-personal-0\", frozen = true)",
  "CDN requests alert": "DashboardRoute(Destination.Alert, target = \"alt-personal-1\", frozen = true)",
  "Custom monitor alert": "DashboardRoute(Destination.Alert, target = \"alt-personal-2\", frozen = true)",
})

export default {
  example: figma.code`AlertScreen(route = ${route}, navigation = navigation, onBack = onBack)`,
  id: 'vercel-660-7350',
}

