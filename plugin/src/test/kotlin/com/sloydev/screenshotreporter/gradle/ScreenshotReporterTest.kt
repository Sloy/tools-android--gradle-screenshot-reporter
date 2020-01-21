package com.sloydev.screenshotreporter.gradle

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.sloydev.screenshotreporter.gradle.ScreenshotReporter.Companion.DEVICE_SCREENSHOT_DIR
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ScreenshotReporterTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()
//    val permanentFolder = File("test-files")

    lateinit var outputFolder: File
    lateinit var inputTestFolder: File
    lateinit var nonExistentOutputFolder: File

    val screenshotReporter = ScreenshotReporter("com.sloydev.screenshotreporter.testapp", File(System.getenv("ANDROID_HOME")))

    @Before
    fun setUp() {
        outputFolder = temporaryFolder.newFolder("outputs")
//        outputFolder = permanentFolder.resolve("outputs").also { it.mkdirs() }
        inputTestFolder = temporaryFolder.newFolder("inputs")
//        inputTestFolder = permanentFolder.resolve("inputs").also { it.mkdirs() }
        nonExistentOutputFolder = temporaryFolder.root.resolve("another_output")

        screenshotReporter.cleanScreenshotsFromDevice()
    }

    @Test
    fun `returns devices`() {
        val devices = screenshotReporter.adb.devices()

        assertWithMessage("No devices found")
                .that(devices)
                .isNotEmpty()

        devices.forEach { println(it) }
    }

    @Test
    fun `pull files when device has files`() {
        givenDeviceHasReportFiles()

        screenshotReporter.pullScreenshots(outputFolder)
        val exportedFiles = outputFolder.listFiles()
        val exportedFileNames = exportedFiles.map { it.name }

        assertThat(exportedFileNames)
                .containsAllOf("screenshot 1.png", "screenshot2.png")
    }

    @Test
    fun `pull nothing when screenshots have been cleaned`() {
        givenDeviceHasReportFiles()
        screenshotReporter.cleanScreenshotsFromDevice()

        screenshotReporter.pullScreenshots(outputFolder)
        val exportedFiles = outputFolder.listFiles()

        assertThat(exportedFiles)
                .isEmpty()
    }

    @Test
    fun `delete files from output folder before reporting`() {
        givenOutputFolderHasOldFiles()

        screenshotReporter.cleanScreenshotsFromDevice()
        screenshotReporter.pullScreenshots(outputFolder)
        val exportedFiles = outputFolder.listFiles()

        assertThat(exportedFiles)
                .isEmpty()
    }

    @Test
    fun `creates output folder when doesn't exist`() {
        assertWithMessage("Output folder already existed")
                .that(nonExistentOutputFolder.exists())
                .isFalse()

        screenshotReporter.pullScreenshots(nonExistentOutputFolder)

        assertWithMessage("Output folder has not been created")
                .that(nonExistentOutputFolder.exists())
                .isTrue()
    }

    @Test
    fun `grant permission on marshmallow`() {
        screenshotReporter.grantPermissions()
        //WARNING: This test will never fail :( How can we check the behavior?
    }

    private fun givenOutputFolderHasOldFiles() {
        arrayOf("old_file1.png", "old_file.png")
                .map { outputFolder.resolve(DEVICE_SCREENSHOT_DIR).resolve(it) }
                .forEach {
                    it.mkdirs()
                    it.createNewFile()
                }
    }

    private fun givenDeviceHasReportFiles() {
        pushFilesToDevice(arrayOf("screenshot 1.png", "screenshot2.png"))
    }

    private fun pushFilesToDevice(files: Array<String>) {
        val device = screenshotReporter.currentDevice
        files.map {  inputTestFolder.resolve(it) }
            .forEach { localTestFile -> localTestFile.createNewFile() }
        val deviceFile = screenshotReporter.adb.getExternalStoragePath(device)
            .resolve(DEVICE_SCREENSHOT_DIR)
        println("Pushing \"${inputTestFolder.absolutePath}\" to device [${device.serialNumber}] on path \"${deviceFile}\"")
        screenshotReporter.adb.pushFile(device, inputTestFolder, deviceFile)
    }
}