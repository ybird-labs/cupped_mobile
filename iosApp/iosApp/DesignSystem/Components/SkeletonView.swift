import SwiftUI

struct SkeletonView: View {
    var width: CGFloat? = nil
    var height: CGFloat = 20
    var cornerRadius: CGFloat = Radius.sm

    @State private var phase: CGFloat = 0
    @Environment(\.accessibilityReduceMotion) var reduceMotion

    var body: some View {
        RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
            .fill(Color.cuppedMuted.opacity(0.15))
            .frame(width: width, height: height)
            .overlay {
                if !reduceMotion {
                    RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                        .fill(shimmerGradient)
                }
            }
            .clipped()
            .onAppear {
                guard !reduceMotion else { return }
                withAnimation(
                    .linear(duration: Motion.shimmerDuration)
                    .repeatForever(autoreverses: false)
                ) {
                    phase = 1
                }
            }
    }

    private var shimmerGradient: LinearGradient {
        LinearGradient(
            colors: [
                .clear,
                Color.white.opacity(0.4),
                .clear
            ],
            startPoint: UnitPoint(x: phase - 1, y: 0.5),
            endPoint: UnitPoint(x: phase, y: 0.5)
        )
    }
}

#Preview("Skeleton Loading") {
    VStack(alignment: .leading, spacing: Spacing.md) {
        SkeletonView(width: 200, height: 24)
        SkeletonView(height: 16)
        SkeletonView(width: 160, height: 16)
        SkeletonView(height: 120, cornerRadius: Radius.md)
    }
    .padding()
}
