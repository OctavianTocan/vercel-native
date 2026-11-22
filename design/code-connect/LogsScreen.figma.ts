// url=https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=661-8114
// source=android/app/src/main/kotlin/com/octaviantocan/vercelnative/dashboard/LogsScreen.kt
// component=LogsScreen
import figma from 'figma'

const route = figma.selectedInstance.getEnum('State', {
  "Project logs": "DashboardRoute(Destination.Logs, target = \"prj-personal-1\", frozen = true)",
  "Workspace logs": "DashboardRoute(Destination.Logs, frozen = true)",
})

export default {
  example: figma.code`LogsScreen(route = ${route}, navigation = navigation, onBack = onBack)`,
  id: 'vercel-661-8114',
}

