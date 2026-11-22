// url=https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=659-6159
// source=android/app/src/main/kotlin/com/octaviantocan/vercelnative/dashboard/ProjectsScreen.kt
// component=ProjectsScreen
import figma from 'figma'

const route = figma.selectedInstance.getEnum('State', {
  "Projects with Favorites": "DashboardRoute(Destination.Projects, frozen = true)",
  "Loading projects": "DashboardRoute(Destination.Projects, variant = \"loading\", frozen = true)",
  "Empty Projects": "DashboardRoute(Destination.Projects, variant = \"empty\", frozen = true)",
  "Projects could not load": "DashboardRoute(Destination.Projects, variant = \"error\", frozen = true)",
})

export default {
  example: figma.code`ProjectsScreen(route = ${route}, navigation = navigation)`,
  id: 'vercel-659-6159',
}

