// url=https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=661-9150
// source=android/app/src/main/kotlin/com/octaviantocan/vercelnative/dashboard/AccountScreen.kt
// component=AgentScreen
import figma from 'figma'

const route = figma.selectedInstance.getEnum('State', {
  "Enabling Agent": "DashboardRoute(Destination.Agent, variant = \"enabling\", frozen = true)",
  "First Agent conversation": "DashboardRoute(Destination.Agent, variant = \"conversation\", frozen = true)",
  "Agent could not enable": "DashboardRoute(Destination.Agent, variant = \"failed\", frozen = true)",
  "Agent usage limit": "DashboardRoute(Destination.Agent, variant = \"limit\", frozen = true)",
})

export default {
  example: figma.code`AgentScreen(route = ${route}, navigation = navigation, onBack = onBack)`,
  id: 'vercel-661-9150',
}

