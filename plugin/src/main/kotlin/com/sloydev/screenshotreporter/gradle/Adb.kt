package com.sloydev.screenshotreporter.gradle

import java.io.File
import java.util.regex.Pattern

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
    return executeCommandOnDevice(device, "shell echo \$EXTERNAL_STORAGE")
        .trim()
        .let { RemoteFile(it) }
  }

  fun pullFolder(device: DeviceId, deviceFolder: RemoteFile, outputFolder: File) {
    executeCommandOnDevice(device, "pull ${deviceFolder.escapedPath} ${outputFolder.escapedPath}")
  }

  fun pushFile(device: DeviceId, localFile: File, deviceFile: RemoteFile) {
    executeCommandOnDevice(device, "push ${localFile.escapedPath} ${deviceFile.escapedPath}")
  }

  fun clearFolder(device: DeviceId, deviceFolder: RemoteFile) {
    executeCommandOnDevice(device, "shell rm -rf ${deviceFolder.escapedPath}")
  }

  fun getApiLevel(device: DeviceId): Int {
    return executeCommandOnDevice(device, "shell getprop ro.build.version.sdk").trim().toInt()
  }

  fun grantExternalStoragePermission(device: DeviceId, appPackage: String) {
    executeCommandOnDevice(device, "pm grant $appPackage android.permission.READ_EXTERNAL_STORAGE")
    executeCommandOnDevice(device, "pm grant $appPackage android.permission.WRITE_EXTERNAL_STORAGE")
  }

  private fun executeCommandOnDevice(device: DeviceId, command: String): String {
    return executeAdbCommand("-s ${device.serialNumber} $command")
  }

  private fun executeAdbCommand(command: String): String {
    if (logEnabled) {
      println("$ adb $command")
    }
    Runtime.getRuntime().exec("${adbBinaryPath.absolutePath} $command").inputStream.reader().use { reader ->
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
class RemoteFile(private val path: String) {
  val escapedPath: String
    get() = fileEscapePattern.matcher(path).replaceAll("\\\\$1")

  fun resolve(relative: String) = RemoteFile(path + File.separator + relative)
}

private val fileEscapePattern = Pattern.compile("([\\\\()*+?\"'&#\\s])")

private val File.escapedPath
  get() = fileEscapePattern.matcher(absolutePath).replaceAll("\\\\$1")