// url=https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=660-9374
// source=android/app/src/main/kotlin/com/octaviantocan/vercelnative/dashboard/WorkspaceScreen.kt
// component=WorkspaceRail
import figma from 'figma'

const route = figma.selectedInstance.getEnum('State', {
  "More menu": "DashboardRoute(Destination.More, frozen = true)",
  "Workspace switcher": "DashboardRoute(Destination.More, variant = \"workspace\", frozen = true)",
})

export default {
  example: figma.code`WorkspaceRail(route = ${route}, navigation = navigation, onBack = onBack, backdrop = backdrop)`,
  id: 'vercel-660-9374',
}

