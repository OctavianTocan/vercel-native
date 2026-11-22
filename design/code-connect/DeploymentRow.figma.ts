// url=https://www.figma.com/design/751Z0BSfstiZTBcJiB3teq?node-id=534-2685
// source=android/app/src/main/kotlin/com/octaviantocan/vercelnative/dashboard/DeploymentScreen.kt
// component=DeploymentRow
import figma from 'figma'

const row = figma.selectedInstance
const title = row.getString('Commit title')
const sha = row.getString('Commit SHA')
const pullRequest = Number(row.getString('Pull request')?.replace(/^#/, ''))
const options = row.getBoolean('Show menu')
const kotlinString = (text: string | undefined) => JSON.stringify(text ?? '').replaceAll('$', '\\$')

export default {
  example: figma.code`DeploymentRow(
    deployment = deployment.copy(
        title = ${kotlinString(title)},
        sha = ${kotlinString(sha)},
        pullRequest = ${pullRequest || 'null'},
    ),
    project = project,
    onOpen = onOpen,
    options = ${options ? 'deploymentOptions' : 'null'},
)`,
  imports: ['import com.octaviantocan.vercelnative.dashboard.DeploymentRow'],
  id: 'vercel-deployment-row',
}
