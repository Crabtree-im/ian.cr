import SwiftUI

@main
struct EditorApp: App {
    var body: some Scene {
        WindowGroup {
            EditorRootView(viewModel: EditorViewModel())
        }
        .windowResizability(.contentSize)
    }
}
