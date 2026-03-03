// ProfileView.swift
// Cupped - cafe.cupped.app
//
// Settings & profile screen with biometric toggle and logout.
// Header section is a placeholder for future profile info.
// Security section contains the biometric opt-in toggle
// (only visible when biometrics are available) and sign-out
// button with confirmation alert.

import SwiftUI

struct ProfileView: View {

    @Environment(AuthCoordinator.self) private var authCoordinator

    // MARK: - State

    /// Whether the sign-out confirmation alert is showing.
    @State private var showLogoutAlert = false

    // MARK: - Computed

    /// Whether biometric auth is available on this device.
    private var isBiometricAvailable: Bool {
        BiometricService.shared.isAvailable
    }

    /// Human-readable biometric name ("Face ID" / "Touch ID").
    private var biometricName: String {
        BiometricService.shared.biometricName
    }

    /// SF Symbol for the current biometric type.
    private var biometricIcon: String {
        switch BiometricService.shared.availableType {
        case .faceID: "faceid"
        case .touchID: "touchid"
        case .none: "lock.shield"
        }
    }

    /// App version from Info.plist.
    private var appVersion: String {
        Bundle.main.infoDictionary?[
            "CFBundleShortVersionString"
        ] as? String ?? "—"
    }

    // MARK: - Body

    var body: some View {
        NavigationStack {
            List {
                headerSection
                securitySection
                appInfoSection
            }
            .listStyle(.insetGrouped)
            .scrollContentBackground(.hidden)
            .background(Color.cuppedCanvas)
            .navigationTitle("Profile")
            .alert(
                "Sign Out",
                isPresented: $showLogoutAlert
            ) {
                Button("Cancel", role: .cancel) {}
                Button("Sign Out", role: .destructive) {
                    Task {
                        await authCoordinator.logout()
                    }
                }
            } message: {
                Text(
                    "Are you sure you want to sign out? "
                        + "You'll need a new magic link to "
                        + "sign back in."
                )
            }
        }
    }

    // MARK: - Header Section

    private var headerSection: some View {
        Section {
            VStack(spacing: Spacing.md) {
                Circle()
                    .fill(Color.cuppedPrimary.opacity(0.1))
                    .frame(width: 80, height: 80)
                    .overlay {
                        Image(
                            systemName:
                                "person.crop.circle.fill"
                        )
                        .font(.system(size: 40))
                        .foregroundStyle(Color.cuppedPrimary)
                    }

                Text("Your Profile")
                    .font(.cuppedBody)
                    .foregroundStyle(Color.cuppedSecondary)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, Spacing.md)
            .listRowBackground(Color.cuppedCanvas)
        }
    }

    // MARK: - Security Section

    private var securitySection: some View {
        Section {
            if isBiometricAvailable {
                biometricRow
            }
            logoutRow
        } header: {
            Text("Security")
                .font(.cuppedSubheadline)
                .foregroundStyle(Color.cuppedSecondary)
        }
        .listRowBackground(Color.cuppedCard)
    }

    /// Biometric toggle row — only rendered when device
    /// supports Face ID or Touch ID.
    private var biometricRow: some View {
        HStack(spacing: Spacing.md) {
            Image(systemName: biometricIcon)
                .font(.system(size: 18))
                .foregroundStyle(Color.cuppedPrimary)
                .frame(width: 24, alignment: .center)

            Text("Use \(biometricName)")
                .font(.cuppedBody)
                .foregroundStyle(Color.cuppedInk)

            Spacer()

            Toggle(
                "",
                isOn: Binding(
                    get: {
                        BiometricService.shared.isEnabled
                    },
                    set: {
                        BiometricService.shared.isEnabled
                            = $0
                    }
                )
            )
            .labelsHidden()
        }
    }

    /// Sign-out row with confirmation alert.
    private var logoutRow: some View {
        Button {
            showLogoutAlert = true
        } label: {
            HStack(spacing: Spacing.md) {
                Image(
                    systemName:
                        "rectangle.portrait.and.arrow.right"
                )
                .font(.system(size: 18))
                .foregroundStyle(Color.cuppedError)
                .frame(width: 24, alignment: .center)

                Text("Sign Out")
                    .font(.cuppedBody)
                    .foregroundStyle(Color.cuppedError)
            }
        }
    }

    // MARK: - App Info Section

    private var appInfoSection: some View {
        Section {
            HStack {
                Text("Version")
                    .font(.cuppedBody)
                    .foregroundStyle(Color.cuppedInk)
                Spacer()
                Text(appVersion)
                    .font(.cuppedBody)
                    .foregroundStyle(Color.cuppedMuted)
            }
        }
        .listRowBackground(Color.cuppedCard)
    }
}

// MARK: - Previews

#Preview {
    ProfileView()
        .environment(AuthCoordinator())
}
