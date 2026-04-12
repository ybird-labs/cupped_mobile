// ProfileView.swift
// Cupped - cafe.cupped.app
//
// Settings & profile screen with logout.
// Header section is a placeholder for future profile info.
// Security section currently contains sign-out with
// confirmation alert.

import SwiftUI

struct ProfileView: View {

    @Environment(AuthCoordinator.self) private var authCoordinator

    // MARK: - State

    /// Whether the sign-out confirmation alert is showing.
    @State private var showLogoutAlert = false

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
                        AppIconView(icon: .profileActive, size: 40, color: Color.cuppedPrimary)
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
            logoutRow
        } header: {
            Text("Security")
                .font(.cuppedSubheadline)
                .foregroundStyle(Color.cuppedSecondary)
        }
        .listRowBackground(Color.cuppedCard)
    }

    /// Sign-out row with confirmation alert.
    private var logoutRow: some View {
        Button {
            showLogoutAlert = true
        } label: {
            HStack(spacing: Spacing.md) {
                AppIconView(icon: .logout, size: 18, color: Color.cuppedError)
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
