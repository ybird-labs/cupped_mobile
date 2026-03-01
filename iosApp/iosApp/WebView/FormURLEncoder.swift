// FormURLEncoder.swift
// Cupped - cafe.cupped.app
//
// Correct application/x-www-form-urlencoded encoding per
// the WHATWG URL Standard and RFC 3986 §2.3.
//
// Why not .urlQueryAllowed?
//   CharacterSet.urlQueryAllowed is designed for the query
//   component of a URL (RFC 3986 §3.4), NOT for form bodies.
//   It passes &, =, and + through unencoded, which corrupts
//   form data structure. For example, a token containing "+"
//   would be misinterpreted as a space by the server.
//
// Why not URLComponents.queryItems?
//   Apple's URLComponents has a known bug where literal "+"
//   in values is not percent-encoded, causing silent data
//   corruption when the server decodes it as a space.

import Foundation

/// Encodes key-value pairs for
/// `application/x-www-form-urlencoded` request bodies.
///
/// Implements the WHATWG URL Standard serialization algorithm:
/// only unreserved characters (RFC 3986 §2.3) plus `*`
/// pass through unencoded. Everything else — including
/// `&`, `=`, `+`, and `@` — is percent-encoded. Spaces
/// are encoded as `+` per the form encoding spec.
///
/// ## Usage
/// ```swift
/// request.httpBody = FormURLEncoder.encodeToData([
///     ("token", bearerToken),
///     ("redirect_to", "/feed")
/// ])
/// ```
enum FormURLEncoder {

    /// The unreserved character set per RFC 3986 §2.3,
    /// plus `*` per the WHATWG URL Standard.
    ///
    /// Built from scratch rather than subtracting from
    /// `.urlQueryAllowed` to avoid accidentally passing
    /// dangerous characters through.
    private static let allowed: CharacterSet = {
        var cs = CharacterSet()
        cs.insert(charactersIn:
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "abcdefghijklmnopqrstuvwxyz"
            + "0123456789"
            + "-._~*"
        )
        return cs
    }()

    /// Percent-encodes a single string for use as a form
    /// key or value.
    ///
    /// Characters outside ``allowed`` are percent-encoded.
    /// Spaces become `+` (per the WHATWG form encoding
    /// spec, not `%20`).
    ///
    /// - Parameter string: The raw string to encode.
    /// - Returns: The form-encoded string.
    static func encode(_ string: String) -> String {
        let percentEncoded = string
            .addingPercentEncoding(
                withAllowedCharacters: allowed
            ) ?? string
        // WHATWG spec requires space -> "+" in form data,
        // but addingPercentEncoding produces "%20".
        return percentEncoded.replacingOccurrences(
            of: "%20", with: "+"
        )
    }

    /// Encodes an ordered list of key-value pairs into a
    /// form body string: `key1=val1&key2=val2`.
    ///
    /// Uses a tuple array (not a dictionary) to preserve
    /// parameter ordering.
    ///
    /// - Parameter parameters: Key-value pairs to encode.
    /// - Returns: The encoded form body string.
    static func encode(
        _ parameters: [(String, String)]
    ) -> String {
        parameters
            .map { "\(encode($0.0))=\(encode($0.1))" }
            .joined(separator: "&")
    }

    /// Encodes parameters and returns UTF-8 `Data` ready
    /// for `URLRequest.httpBody`.
    ///
    /// - Parameter parameters: Key-value pairs to encode.
    /// - Returns: UTF-8 encoded form body data.
    static func encodeToData(
        _ parameters: [(String, String)]
    ) -> Data {
        Data(encode(parameters).utf8)
    }
}
