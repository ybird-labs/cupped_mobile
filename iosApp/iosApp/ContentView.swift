import SwiftUI
import Shared

struct ContentView: View {
    @State private var showContent = false
    var body: some View {
        VStack {
            Button("Click me!") {
                withAnimation {
                    showContent = !showContent
                }
            }

            if showContent {
                VStack(spacing: Spacing.base) {
                    Image(systemName: "swift")
                        .font(.system(size: 200))
                        .foregroundStyle(Color.cuppedActionPrimary)
                    Text("SwiftUI: \(Greeting().greet())")
                }
                .transition(.move(edge: .top).combined(with: .opacity))
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .padding(Spacing.base)
        .background(Color.cuppedSurfaceApp)
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
