// url=https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=213-259
// source=android/app/src/main/kotlin/com/octaviantocan/vercelnative/dashboard/ActivityScreen.kt
// component=ActivityTabs
import figma from 'figma'

const selected = figma.selectedInstance.getEnum('Selected', {
  Recents: 'ActivityTab.Recents',
  Usage: 'ActivityTab.Usage',
  Alerts: 'ActivityTab.Alerts',
})

export default {
  example: figma.code`ActivityTabs(selected = ${selected}, onSelect = onTabSelected)`,
  imports: [
    'import com.octaviantocan.vercelnative.dashboard.ActivityTab',
    'import com.octaviantocan.vercelnative.dashboard.ActivityTabs',
  ],
  id: 'vercel-activity-tabs',
}
