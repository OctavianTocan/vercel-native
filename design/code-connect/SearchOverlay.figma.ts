// url=https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=660-8860
// source=android/app/src/main/kotlin/com/octaviantocan/vercelnative/dashboard/SearchOverlay.kt
// component=SearchOverlay
import figma from 'figma'

const route = figma.selectedInstance.getEnum('State', {
  "Search suggestions": "DashboardRoute(Destination.Search, variant = \"suggestions\", frozen = true)",
  "Typed search results": "DashboardRoute(Destination.Search, variant = \"typed\", frozen = true)",
  "Search fallback": "DashboardRoute(Destination.Search, variant = \"fallback\", frozen = true)",
  "Navigation Assistant after Send": "DashboardRoute(Destination.Search, variant = \"submitted\", frozen = true)",
})

export default {
  example: figma.code`SearchOverlay(route = ${route}, navigation = navigation, onBack = onBack, backdrop = content, active = true)`,
  id: 'vercel-660-8860',
}

