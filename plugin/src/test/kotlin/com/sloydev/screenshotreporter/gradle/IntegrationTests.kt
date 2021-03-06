package com.sloydev.screenshotreporter.gradle

import com.google.common.truth.Truth.assertThat
import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class IntegrationTests {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var projectDir: File
    private val pullTaskName = ":${PullScreenshotsTask.TASK_NAME}"
    private val setupTaskName = ":${SetupScreenshotsTask.TASK_NAME}"

    @Before
    fun setUp() {
        projectDir = temporaryFolder.newFolder("vanilla").also {
            FileUtils.copyDirectory(File(javaClass.classLoader.getResource("vanilla").path), it)
            File(it, "local.properties").writeText("sdk.dir=${System.getenv("HOME")}/Library/Android/sdk", Charsets.UTF_8)
            File(it, "libs").also {
                it.mkdir()
                FileUtils.copyFileToDirectory(File(".", "build/libs/plugin.jar"), it)
            }
        }
    }

    @Test
    fun task_runs() {
        val result = gradleRunner()
                .withArguments(pullTaskName)
                .build()

        val task = result.task(pullTaskName)
        val taskOutcome = task?.outcome
        assertThat(taskOutcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun task_generates_output_files() {
        gradleRunner()
                .withArguments(pullTaskName)
                .build()

        val outputReportDirectory = projectDir.resolve("build")
                .resolve(PullScreenshotsTask.REPORTS_FOLDER)
                .resolve(PullScreenshotsTask.REPORTS_SUBFOLDER)
        assertThat(outputReportDirectory.exists())
                .isTrue()
    }

    @Test
    fun custom_task_runs_all() {
        val result = gradleRunner()
                .withArguments("generateScreenshots")
                .build()

        val pullTask = result.task(pullTaskName)
        val setupTask = result.task(setupTaskName)
        val customTask = result.task(":dummyTask")

        assertThat(pullTask?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(setupTask?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(customTask?.outcome).isNotNull()
    }

    private fun gradleRunner(): GradleRunner {
        return GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .forwardOutput()
    }
}
