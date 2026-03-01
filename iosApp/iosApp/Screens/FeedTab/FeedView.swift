import SwiftUI
import Shared
import KMPObservableViewModelSwiftUI

struct FeedView: View {
    @StateViewModel var viewModel = KoinHelper.shared.makeSmokeTestViewModel()

    var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                Text(viewModel.greetingValue)
                    .font(.cuppedTitle3)

                Text(viewModel.statusValue)
                    .font(.cuppedBody)
                    .foregroundStyle(Color.cuppedMuted)

                CuppedButton(title: "Check Server Health", style: .primary) {
                    viewModel.checkHealth()
                }
            }
            .padding()
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.cuppedCanvas)
            .navigationTitle("Feed")
        }
    }
}
