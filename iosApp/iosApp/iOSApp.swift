import SwiftUI
import shared
import UIKit
import UniformTypeIdentifiers

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeRoot()
                .ignoresSafeArea(.keyboard) // let Compose handle keyboard insets
        }
    }
}

/// Hosts the shared Compose UI. Hands a UIKit-backed `BackupBridge` to Kotlin so the
/// Settings screen can open the system Files document picker for export / import.
struct ComposeRoot: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let bridge = IOSBackupBridge()
        let controller = IosEntryKt.MainViewController(backupBridge: bridge)
        bridge.host = controller
        return controller
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
