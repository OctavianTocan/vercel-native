// url=https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=660-6832
// source=android/app/src/main/kotlin/com/octaviantocan/vercelnative/dashboard/DeploymentScreen.kt
// component=DeploymentSheetContent
import figma from 'figma'

const route = figma.selectedInstance.getEnum('State', {
  "Open preview in browser": "DashboardRoute(Destination.Preview, target = \"dpl-personal-1-1\", frozen = true)",
  "Protected preview": "DashboardRoute(Destination.Preview, target = \"dpl-personal-1-1\", variant = \"protected\", frozen = true)",
  "Preview unavailable": "DashboardRoute(Destination.Preview, target = \"dpl-personal-1-1\", variant = \"unavailable\", frozen = true)",
  "Toolbar unavailable": "DashboardRoute(Destination.Toolbar, target = \"dpl-personal-1-1\", frozen = true)",
})

export default {
  example: figma.code`DeploymentSheetContent(route = ${route}, navigation = navigation, onBack = onBack)`,
  id: 'vercel-660-6832',
}

