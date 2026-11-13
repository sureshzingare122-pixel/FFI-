package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.TrainingSession
import com.example.data.model.TrainingStatistics
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AnalyticsExporter {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun buildCsvContent(sessions: List<TrainingSession>): String {
        val header = "id,session_key,mode,sub_mode,profile_name,timestamp_iso,duration_ms," +
            "total_attempts,successful_hits,missed_targets,accuracy_percent," +
            "center_zone_accuracy_percent,avg_reaction_time_ms,flick_accuracy_percent,score,performance_ratio"

        val rows = sessions.joinToString("\n") { s ->
            val ts = escapeCsv(isoFormat.format(Date(s.timestampMs)))
            listOf(
                s.id.toString(),
                escapeCsv(s.sessionKey),
                escapeCsv(s.mode),
                escapeCsv(s.subMode),
                escapeCsv(s.profileName),
                ts,
                s.durationMs.toString(),
                s.totalAttempts.toString(),
                s.successfulHits.toString(),
                s.missedTargets.toString(),
                String.format(Locale.US, "%.2f", s.accuracyPercent),
                String.format(Locale.US, "%.2f", s.centerZoneAccuracyPercent),
                String.format(Locale.US, "%.1f", s.avgReactionTimeMs),
                String.format(Locale.US, "%.2f", s.flickAccuracyPercent),
                s.score.toString(),
                String.format(Locale.US, "%.2f", s.performanceRatio)
            ).joinToString(",")
        }

        return if (rows.isEmpty()) header else "$header\n$rows"
    }

    fun exportCsvFile(context: Context, sessions: List<TrainingSession>): Uri {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = "ffi_trainer_pro_analytics_${System.currentTimeMillis()}.csv"
        val file = File(exportDir, fileName)
        file.writeText(buildCsvContent(sessions))
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun shareCsv(context: Context, sessions: List<TrainingSession>): Result<File> {
        return runCatching {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val fileName = "ffi_trainer_pro_analytics_${System.currentTimeMillis()}.csv"
            val file = File(exportDir, fileName)
            file.writeText(buildCsvContent(sessions))
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "FFI Trainer Pro — Training Analytics CSV")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Export Training CSV").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            file
        }
    }

    fun buildSummaryText(stats: TrainingStatistics): String {
        val totalMin = stats.totalTrainingDurationMs / 60_000L
        val totalSec = (stats.totalTrainingDurationMs % 60_000L) / 1000L
        return buildString {
            appendLine("=== FFI TRAINER PRO — TRAINING REPORT ===")
            appendLine("Sessions Completed: ${stats.totalSessions}")
            appendLine("Best Score: ${stats.bestScore}")
            appendLine("Average Score: ${String.format(Locale.US, "%.1f", stats.averageScore)}")
            appendLine("Overall Accuracy: ${String.format(Locale.US, "%.1f%%", stats.overallAccuracyPercent)}")
            appendLine("Center-Zone Accuracy: ${String.format(Locale.US, "%.1f%%", stats.centerZoneAccuracyPercent)}")
            appendLine("Avg Reaction Time: ${String.format(Locale.US, "%.0f ms", stats.averageReactionTimeMs)}")
            appendLine("Simulated Performance Ratio: ${String.format(Locale.US, "%.2f", stats.simulatedPerformanceRatio)}")
            appendLine("Total Training Duration: ${totalMin}m ${totalSec}s")
            appendLine("Generated offline by FFI Trainer Pro")
        }
    }

    fun shareSummary(context: Context, stats: TrainingStatistics): Result<Unit> {
        return runCatching {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "FFI Trainer Pro — Performance Summary")
                putExtra(Intent.EXTRA_TEXT, buildSummaryText(stats))
            }
            val chooser = Intent.createChooser(intent, "Share Performance Summary").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    private fun escapeCsv(value: String): String {
        val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n')
        return if (needsQuotes) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
