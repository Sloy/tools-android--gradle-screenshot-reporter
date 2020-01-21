package com.sloydev.screenshotreporter.gradle

import java.io.File

class ScreenshotReporter(val appPackage: String, sdkDirectory: File) {

  private val adbPath = sdkDirectory.resolve("platform-tools").resolve("adb")
  val adb = Adb(adbPath)

  companion object {
    val DEVICE_SCREENSHOT_DIR = "app_spoon-screenshots"
    val MARSHMALLOW_API_LEVEL = 23
  }

  fun pullScreenshots(outputDir: File) {
    outputDir.deleteRecursively()
    outputDir.mkdirs()

    val singleDevice = getRunningDevice()
    pullExternalDirectory(singleDevice, DEVICE_SCREENSHOT_DIR, outputDir)
    simplifyDirectoryStructure(outputDir)

    println("Wrote screenshots report to file://${outputDir.absolutePath}")
  }

  private fun simplifyDirectoryStructure(outputDir: File) {
    outputDir.resolve(DEVICE_SCREENSHOT_DIR).listFiles().orEmpty()
        .forEach { subDir ->
          subDir.renameTo(outputDir.resolve(subDir.name))
        }
    outputDir.resolve(DEVICE_SCREENSHOT_DIR).delete()
  }

  fun cleanScreenshotsFromDevice() {
    val device = getRunningDevice()
    val screenshotsFolder = adb.getExternalStoragePath(device).resolve(DEVICE_SCREENSHOT_DIR)
    println("Cleaning existing screenshots on \"${screenshotsFolder.escapedPath}\" from device [${device.serialNumber}]...")
    adb.clearFolder(device, screenshotsFolder)
  }

  fun grantPermissions() {
    val device = getRunningDevice()
    val apiLevel = adb.getApiLevel(device)
    if (apiLevel >= MARSHMALLOW_API_LEVEL) {
      println("Granting read/write storage permission to device [${device.serialNumber}]...")
      adb.grantExternalStoragePermission(device, appPackage)
    }
  }

  private fun getRunningDevice(): DeviceId {
    val devices = adb.devices()
    check(devices.isNotEmpty()) { "No devices found" }
    check(devices.size == 1) { "More than one device, not supported for now :(" }
    return devices[0]
  }

  private fun pullExternalDirectory(device: DeviceId, directoryName: String, outputDir: File) {
    // Output path on public external storage, for Lollipop and above.
    val externalDir: RemoteFile = adb.getExternalStoragePath(device).resolve(directoryName)
    println("Pulling files from \"${externalDir.escapedPath}\" on device [${device.serialNumber}]...")
    try {
      adb.pullFolder(device, externalDir, outputDir)
    } catch (noDirectoryException: AdbNoSuchFileOrDirectoryException) {
      println("Warning: Directory not found on device, no screenshots were pulled.")
    }
  }
}