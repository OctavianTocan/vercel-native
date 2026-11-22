package com.octaviantocan.vercelnative.dashboard

import java.util.Locale

internal data class Workspace(val id: String, val name: String)
internal data class Project(val id: String, val workspaceId: String, val name: String, val number: Int) {
    val domain get() = "$name.example"
    val repository get() = "sample-studio/$name"
}

internal enum class DeploymentStatus(val label: String) {
    Ready("Ready"), Building("Building"), Error("Error"), Canceled("Canceled"),
}

internal data class Deployment(
    val id: String,
    val projectId: String,
    val title: String,
    val branch: String,
    val sha: String,
    val author: String,
    val status: DeploymentStatus,
    val ageHours: Int,
    val pullRequest: Int?,
) {
    val environment get() = if (branch == "main") "Production" else "Preview"
    val url get() = "$id.example"
    val elapsed get() = if (ageHours < 24) "$ageHours h ago" else "${ageHours / 24} d ago"
}

internal enum class DomainStatus(val label: String) {
    Verified("Verified"), Unverified("Needs verification"), Restricted("Access required"),
}

internal data class ProjectDomain(
    val id: String, val projectId: String, val hostname: String, val status: DomainStatus,
)

internal enum class AlertKind(val label: String, val metric: String) {
    Function("Function errors", "Error rate"),
    Cdn("CDN requests", "Requests"),
    Monitor("Custom monitor", "Failing checks"),
}

internal data class DashboardAlert(
    val id: String, val projectId: String, val kind: AlertKind, val detail: String,
    val value: String, val threshold: String, val elapsed: String,
)

internal enum class LogLevel(val label: String) { Info("Info"), Warning("Warning"), Error("Error") }

internal data class LogEvent(
    val id: String, val deploymentId: String, val time: String,
    val level: LogLevel, val path: String, val message: String, val durationMs: Int,
)

internal data class UsageResource(val id: String, val name: String, val amount: Double, val unit: String, val cost: Double)

internal fun sampleMoney(amount: Double): String = String.format(Locale.US, "\$%,.2f", amount)
internal fun sampleNumber(amount: Double): String = String.format(Locale.US, "%,.1f", amount)

internal object DashboardFixtures {
    val workspaces = listOf(Workspace("personal", "Personal"), Workspace("studio", "Studio"))
    val primaryWorkspace = workspaces.first().id
    val studioWorkspace = workspaces.last().id

    private val projectNames = listOf(
        "atlas-catalog", "weekend-market", "harbor-notes", "lumen-docs", "field-guide",
        "greenhouse", "copper-shop", "studio-calendar", "northwind", "signal-board",
        "paper-route", "trailhead", "quartz-status", "tideline", "open-library",
        "orchard-admin", "patchwork", "waypoint", "garden-api", "roundtable",
        "meter-dashboard", "sunday-kitchen", "redwood", "ember-forms", "letterpress",
        "bridge-console", "sundial", "weather-window", "archive-search", "portal",
        "field-station", "ash-studio",
    )
    private val changes = listOf(
        "Refine product filters", "Update the docs sidebar", "Improve empty state copy",
        "Add mobile navigation", "Cache collection responses", "Tune checkout layout",
        "Add a status endpoint", "Fix the search count", "Update the landing page",
        "Render related products", "Restore the activity feed", "Tighten image loading",
    )
    private val branches = listOf("main", "preview/catalog", "main", "fix/navigation", "main", "preview/layout")
    private val authors = listOf("Jamie Example", "Riley Sample", "Morgan Demo", "Taylor Fixture")

    val projects = workspaces.flatMapIndexed { workspaceIndex, workspace ->
        projectNames.mapIndexed { index, name ->
            Project("prj-${workspace.id}-${index + 1}", workspace.id, if (workspaceIndex == 0) name else "$name-studio", index)
        }
    }
    val defaultProject = projects.first().id
    val favorites = projects.filter { it.number < 4 }.map(Project::id).toSet()

    private val deploymentsByProject = projects.associate { project ->
        project.id to List(26) { index ->
            val revision = project.number * 29 + index + 1
            Deployment(
                id = "dpl-${project.id.removePrefix("prj-")}-${index + 1}",
                projectId = project.id,
                title = changes[(project.number + index) % changes.size],
                branch = branches[index % branches.size],
                sha = (0x80c000 + revision * 473).toString(16).padStart(7, '0'),
                author = authors[(project.number + index) % authors.size],
                status = when (index % 11) {
                    3 -> DeploymentStatus.Building
                    5 -> DeploymentStatus.Error
                    9 -> DeploymentStatus.Canceled
                    else -> DeploymentStatus.Ready
                },
                ageHours = index * 3 + project.number + 1,
                pullRequest = if (index % 3 == 0) null else 57 + revision,
            )
        }
    }
    private val deploymentsById = deploymentsByProject.values.flatten().associateBy(Deployment::id)
    val defaultDeployment = deployments(defaultProject).first().id

    private val domainsByProject = projects.associate { project ->
        project.id to List(23) { index ->
            val prefix = when (index) {
                0 -> ""
                1 -> "www."
                2 -> "api."
                else -> "preview-$index."
            }
            ProjectDomain(
                "dom-${project.id}-$index", project.id, prefix + project.domain,
                when (index % 7) {
                    1 -> DomainStatus.Unverified
                    2 -> DomainStatus.Restricted
                    else -> DomainStatus.Verified
                },
            )
        }
    }
    private val domainsById = domainsByProject.values.flatten().associateBy(ProjectDomain::id)

    val alerts = workspaces.flatMap { workspace ->
        val candidates = projects.filter { it.workspaceId == workspace.id }
        List(36) { index ->
            val project = candidates[index % candidates.size]
            val kind = AlertKind.entries[index % AlertKind.entries.size]
            val (detail, value, threshold) = when (kind) {
                AlertKind.Function -> Triple("/api/catalog returned errors", "${index + 3}.2%", "2.0%")
                AlertKind.Cdn -> Triple("Traffic above the expected range", "${index + 18}.4k", "15.0k")
                AlertKind.Monitor -> Triple("Checkout monitor missed a check", "${index + 2} checks", "1 check")
            }
            DashboardAlert("alt-${workspace.id}-$index", project.id, kind, detail, value, threshold, "${index + 1} h ago")
        }
    }
    private val alertsById = alerts.associateBy(DashboardAlert::id)

    private val logsByProject = projects.associate { project ->
        project.id to List(45) { index ->
            val deployment = deployments(project.id)[index % 8]
            val path = listOf("/api/catalog", "/checkout", "/api/search", "/health", "/api/items")[index % 5]
            val time = String.format(Locale.US, "14:%02d:%02d", 59 - index, (index * 13 + project.number) % 60)
            val level = LogLevel.entries[(index + project.number) % 6 / 2]
            val message = when (level) {
                LogLevel.Info -> "Request completed"
                LogLevel.Warning -> "Slow response from upstream"
                LogLevel.Error -> "Request timed out"
            }
            LogEvent("log-${project.id}-$index", deployment.id, time, level, path, message, 48 + index * 36 + project.number)
        }
    }
    private val logsById = logsByProject.values.flatten().associateBy(LogEvent::id)

    private val resourceNames = listOf(
        "Edge Requests", "Fast Data Transfer", "Function Invocations", "Active CPU", "Provisioned Memory",
        "Fast Origin Transfer", "Image Transformations", "ISR Reads", "ISR Writes", "Web Analytics Events",
        "Speed Insights Data Points", "Build Execution", "Cache Reads", "Cache Writes", "Blob Storage",
        "Blob Data Transfer", "Blob Operations", "Monitoring Events", "Workflow Runs", "Workflow Steps",
        "Log Drains", "Observability Events", "Queues", "Sandbox Execution",
    )
    val resources = resourceNames.mapIndexed { index, name ->
        val units = when (index % 4) {
            1 -> "GB"
            2 -> "min"
            else -> "k"
        }
        UsageResource("usage-$index", name, 7.6 + index * 12.15, units, 0.13 + (index * 11 % 93) * 0.07)
    }

    fun workspace(id: String) = workspaces.first { it.id == id }
    fun project(id: String) = projects.first { it.id == id }
    fun deployments(projectId: String) = deploymentsByProject.getValue(projectId)
    fun deployment(id: String) = deploymentsById.getValue(id)
    fun domains(projectId: String) = domainsByProject.getValue(projectId)
    fun domain(id: String) = domainsById.getValue(id)
    fun alert(id: String) = alertsById.getValue(id)
    fun logs(projectId: String) = logsByProject.getValue(projectId)
    fun log(id: String) = logsById.getValue(id)
}
