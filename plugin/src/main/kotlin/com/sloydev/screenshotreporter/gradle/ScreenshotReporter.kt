package com.sloydev.screenshotreporter.gradle

import java.io.File

class ScreenshotReporter(val appPackage: String, sdkDirectory: File) {

  private val adbPath = sdkDirectory.resolve("platform-tools").resolve("adb")
  val adb = Adb(adbPath)

  val currentDevice: DeviceId by lazy {
    val devices = adb.devices()
    check(devices.isNotEmpty()) { "No devices found" }
    check(devices.size == 1) { "More than one device, not supported for now :(" }
    devices[0]
  }

  companion object {
    val DEVICE_SCREENSHOT_DIR = "app_spoon-screenshots"
    val MARSHMALLOW_API_LEVEL = 23
  }

  fun pullScreenshots(outputDir: File) {
    outputDir.deleteRecursively()
    outputDir.mkdirs()

    pullExternalDirectory(currentDevice, DEVICE_SCREENSHOT_DIR, outputDir)
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
    val screenshotsFolder = adb.getExternalStoragePath(currentDevice).resolve(DEVICE_SCREENSHOT_DIR)
    println("Cleaning existing screenshots on \"${screenshotsFolder.path}\" from device [${currentDevice.serialNumber}]...")
    adb.clearFolder(currentDevice, screenshotsFolder)
  }

  fun grantPermissions() {
    val apiLevel = adb.getApiLevel(currentDevice)
    if (apiLevel >= MARSHMALLOW_API_LEVEL) {
      println("Granting read/write storage permission to device [${currentDevice.serialNumber}]...")
      adb.grantExternalStoragePermission(currentDevice, appPackage)
    }
  }

  private fun pullExternalDirectory(device: DeviceId, directoryName: String, outputDir: File) {
    // Output path on public external storage, for Lollipop and above.
    val externalDir: RemoteFile = adb.getExternalStoragePath(device).resolve(directoryName)
    println("Pulling files from \"${externalDir.path}\" on device [${device.serialNumber}]...")
    try {
      adb.pullFolder(device, externalDir, outputDir)
    } catch (noDirectoryException: AdbNoSuchFileOrDirectoryException) {
      println("Warning: Directory not found on device, no screenshots were pulled.")
    }
  }
}