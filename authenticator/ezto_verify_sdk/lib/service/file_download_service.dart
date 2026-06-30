// // import 'package:file_saver/file_saver.dart';

// import 'dart:io';

// import 'package:background_downloader/background_downloader.dart';

// class FileDownloadService {
//   Future<String> download({
//     required String url,
//     required String fileName,
//     required String mimeType,
//   }) async {
//     final task = DownloadTask(
//       url: url,
//       filename: DownloadTask.suggestedFilename,
//       updates: Updates.statusAndProgress,
//       retries: 5,
//       allowPause: false,
//       requiresWiFi: false,
//       priority: 0,
//     );
//     final downloader = FileDownloader();
//     downloader.configureNotification(
//       running: const TaskNotification('Downloading File', 'file: {filename}'),
//       complete: const TaskNotification('File Downloaded', 'file: {filename}'),
//       error: const TaskNotification('Download Failed', 'file: {filename}'),
//       progressBar: true,
//       tapOpensFile: false,
//     );
//     final result = await downloader.download(
//       task,
//     );
//     if (!Platform.isIOS) {
//       await downloader.moveFileToSharedStorage(
//         await result.task.filePath(),
//         SharedStorage.downloads,
//       );
//     }
//     return result.status.name;
//     // return FileSaver.instance.saveFile(
//     //   name: fileName,
//     //   customMimeType: mimeType,
//     //   link: LinkDetails(
//     //     link: url,
//     //   ),
//     // );
//   }
// }
