import Foundation
import UIKit
import UniformTypeIdentifiers
import shared

/// Bridges the shared `BackupBridge` Kotlin interface to UIKit's document picker.
final class IOSBackupBridge: NSObject, BackupBridge {

    weak var host: UIViewController?

    private var pendingExport: ((BackupBridgeOutcome) -> Void)?
    private var pendingImport: ((BackupBridgeOutcome, String?) -> Void)?

    func exportJson(
        suggestedName: String,
        json: String,
        onResult: @escaping (BackupBridgeOutcome) -> Void
    ) {
        do {
            let tmp = FileManager.default.temporaryDirectory.appendingPathComponent(suggestedName)
            try json.data(using: .utf8)?.write(to: tmp, options: .atomic)
            DispatchQueue.main.async {
                self.pendingExport = onResult
                let picker = UIDocumentPickerViewController(forExporting: [tmp])
                picker.delegate = self
                self.host?.present(picker, animated: true)
            }
        } catch {
            onResult(BackupBridgeOutcome.failure)
        }
    }

    func importJson(onResult: @escaping (BackupBridgeOutcome, String?) -> Void) {
        DispatchQueue.main.async {
            self.pendingImport = onResult
            let picker = UIDocumentPickerViewController(forOpeningContentTypes: [.json, .text])
            picker.allowsMultipleSelection = false
            picker.delegate = self
            self.host?.present(picker, animated: true)
        }
    }
}

extension IOSBackupBridge: UIDocumentPickerDelegate {

    func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
        if let cb = pendingExport {
            pendingExport = nil
            cb(urls.isEmpty ? BackupBridgeOutcome.cancelled : BackupBridgeOutcome.success)
            return
        }
        if let cb = pendingImport {
            pendingImport = nil
            guard let url = urls.first else {
                cb(BackupBridgeOutcome.cancelled, nil)
                return
            }
            let needsScope = url.startAccessingSecurityScopedResource()
            defer { if needsScope { url.stopAccessingSecurityScopedResource() } }
            do {
                let data = try Data(contentsOf: url)
                let text = String(data: data, encoding: .utf8)
                cb(BackupBridgeOutcome.success, text)
            } catch {
                cb(BackupBridgeOutcome.failure, nil)
            }
        }
    }

    func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
        if let cb = pendingExport {
            pendingExport = nil
            cb(BackupBridgeOutcome.cancelled)
        }
        if let cb = pendingImport {
            pendingImport = nil
            cb(BackupBridgeOutcome.cancelled, nil)
        }
    }
}
