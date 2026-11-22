// url=https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=660-7651
// source=android/app/src/main/kotlin/com/octaviantocan/vercelnative/dashboard/ActivityScreen.kt
// component=UsageScreen
import figma from 'figma'

const route = figma.selectedInstance.getEnum('State', {
  "Daily Usage": "DashboardRoute(Destination.Usage, frozen = true)",
  "No recorded usage": "DashboardRoute(Destination.Usage, variant = \"empty\", frozen = true)",
})

export default {
  example: figma.code`UsageScreen(route = ${route}, navigation = navigation)`,
  id: 'vercel-660-7651',
}

