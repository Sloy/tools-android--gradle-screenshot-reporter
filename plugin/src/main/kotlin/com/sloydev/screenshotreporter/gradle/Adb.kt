package com.sloydev.screenshotreporter.gradle

import java.io.File

class Adb(private val adbBinaryPath: File, private val logEnabled: Boolean = true) {

  fun devices(): List<DeviceId> {
    return executeAdbCommand("devices")
        .trim()
        .split('\n').drop(1)
        .filter { it.isNotBlank() }
        .map { line ->
          line.split('\t').first()
        }
        .map { DeviceId(it) }
  }

  fun getExternalStoragePath(device: DeviceId): RemoteFile {
    return device.executeCommandOnDevice("shell", "echo", "\$EXTERNAL_STORAGE")
        .trim()
        .let { RemoteFile(it) }
  }

  fun pullFolder(device: DeviceId, deviceFolder: RemoteFile, outputFolder: File) {
    device.executeCommandOnDevice("pull", deviceFolder.path, outputFolder.absolutePath)
  }

  fun pushFile(device: DeviceId, localFile: File, deviceFile: RemoteFile) {
    device.executeCommandOnDevice("push", localFile.absolutePath, deviceFile.path)
  }

  fun clearFolder(device: DeviceId, deviceFolder: RemoteFile) {
    device.executeCommandOnDevice("shell", "rm -rf", deviceFolder.path)
  }

  fun getApiLevel(device: DeviceId): Int {
    return device.executeCommandOnDevice("shell", "getprop ro.build.version.sdk").trim().toInt()
  }

  fun grantExternalStoragePermission(device: DeviceId, appPackage: String) {
    device.executeCommandOnDevice("shell", "pm", "grant", appPackage, "android.permission.READ_EXTERNAL_STORAGE")
    device.executeCommandOnDevice("shell", "pm", "grant", appPackage, "android.permission.WRITE_EXTERNAL_STORAGE")
  }

  private fun DeviceId.executeCommandOnDevice(vararg command: String): String {
    return executeAdbCommand("-s", this.serialNumber, *command)
  }

  private fun executeAdbCommand(vararg command: String): String {
    if (logEnabled) {
      println("$ adb ${command.joinToString(" ")}")
    }
    Runtime.getRuntime().exec(arrayOf(adbBinaryPath.absolutePath, *command)).inputStream.reader().use { reader ->
      val response = reader.readText().also { commandResponse ->
        println(commandResponse.trim().split("\n").joinToString("\n") { "> $it" } + "\n")
      }
      return parseResponse(response)
    }
  }

  private fun parseResponse(response: String): String {
    if (response.startsWith("adb: error:")) {
      val message = response.removePrefix("adb: error:").trim()
      if (message.endsWith("No such file or directory")) {
        throw AdbNoSuchFileOrDirectoryException(response)
      } else {
        throw AdbException(message)
      }
    } else {
      return response.trim()
    }
  }
}

open class AdbException(message: String) : RuntimeException(message)
class AdbNoSuchFileOrDirectoryException(message: String) : AdbException(message)

data class DeviceId(val serialNumber: String)
class RemoteFile(val path: String) {
  fun resolve(relative: String) = RemoteFile(path + File.separator + relative)
}
