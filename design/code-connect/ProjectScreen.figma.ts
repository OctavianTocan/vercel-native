// url=https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=659-8097
// source=android/app/src/main/kotlin/com/octaviantocan/vercelnative/dashboard/ProjectScreen.kt
// component=ProjectScreen
import figma from 'figma'

const route = figma.selectedInstance.getEnum('State', {
  "Project overview": "DashboardRoute(Destination.Project, target = \"prj-personal-1\", frozen = true)",
  "Project Deployments": "DashboardRoute(Destination.Project, target = \"prj-personal-1\", variant = \"deployments\", frozen = true)",
})

export default {
  example: figma.code`ProjectScreen(route = ${route}, navigation = navigation, onBack = onBack)`,
  id: 'vercel-659-8097',
}

