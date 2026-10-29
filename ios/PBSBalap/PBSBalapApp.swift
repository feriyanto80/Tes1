import SwiftUI
import WebKit
import CoreLocation
import UIKit

@main
struct PBSBalapApp: App {
    var body: some Scene {
        WindowGroup {
            BalapScreen()
                .tint(Color(red: 0, green: 0.68, blue: 0.85))
        }
    }
}

final class BalapPage: ObservableObject {
    static let home = URL(string: "https://balap.pbsgroup.id/")!
    @Published var loading = false
    @Published var progress = 0.0
    @Published var failed = false
    weak var webView: WKWebView?

    func reload() {
        failed = false
        if let webView {
            if webView.url == nil { webView.load(URLRequest(url: Self.home)) }
            else { webView.reload() }
        }
    }
}

final class GPSStatus: NSObject, ObservableObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()
    @Published private(set) var status: CLAuthorizationStatus

    override init() {
        status = manager.authorizationStatus
        super.init()
        manager.delegate = self
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        DispatchQueue.main.async { self.status = manager.authorizationStatus }
    }

    func request() { manager.requestWhenInUseAuthorization() }

    func openSettings() {
        if let url = URL(string: UIApplication.openSettingsURLString) {
            UIApplication.shared.open(url)
        }
    }

    var diagnostic: String {
        let permission: String
        switch status {
        case .authorizedAlways, .authorizedWhenInUse: permission = "Diizinkan"
        case .denied: permission = "Ditolak"
        case .restricted: permission = "Dibatasi"
        case .notDetermined: permission = "Belum diminta"
        @unknown default: permission = "Tidak diketahui"
        }
        return "Izin lokasi PBS Balap: \(permission).\n" +
            "Layanan lokasi iPhone: \(CLLocationManager.locationServicesEnabled() ? "Aktif" : "Nonaktif").\n\n" +
            "Jika Google Apps Script berada dalam iframe tanpa izin geolocation, " +
            "GPS dapat tetap gagal meski izin iPhone aktif. Situs BALAP harus " +
            "memperbaiki izin iframe dan Permissions-Policy."
    }
}

struct BalapScreen: View {
    @StateObject private var page = BalapPage()
    @StateObject private var gps = GPSStatus()
    @State private var showGPS = false

    var body: some View {
        GeometryReader { geo in
            let compact = geo.size.width < 360 || geo.size.height < 380
            VStack(spacing: 0) {
                HStack(spacing: compact ? 8 : 12) {
                    Image("BrandLogo")
                        .resizable()
                        .scaledToFit()
                        .frame(width: compact ? 34 : 42, height: compact ? 34 : 42)
                        .clipShape(RoundedRectangle(cornerRadius: 9))
                        .accessibilityLabel("Logo PBS")

                    VStack(alignment: .leading, spacing: 2) {
                        Text("PBS Balap")
                            .font(.system(size: compact ? 16 : 19, weight: .bold))
                            .lineLimit(1)
                        if !compact {
                            Text("balap.pbsgroup.id")
                                .font(.system(size: 11))
                                .foregroundColor(.white.opacity(0.72))
                        }
                    }
                    Spacer(minLength: 3)
                    Button { showGPS = true } label: {
                        Image(systemName: "location.circle")
                            .font(.system(size: 24))
                            .frame(width: 44, height: 44)
                    }
                    .accessibilityLabel("Periksa GPS")
                    Button { page.reload() } label: {
                        Image(systemName: "arrow.clockwise")
                            .font(.system(size: 23))
                            .frame(width: 44, height: 44)
                    }
                    .accessibilityLabel("Muat ulang")
                }
                .foregroundStyle(.white)
                .padding(.horizontal, compact ? 8 : 14)
                .frame(height: compact ? 52 : 62)
                .background(Color(red: 6/255, green: 25/255, blue: 49/255))

                if page.loading {
                    ProgressView(value: page.progress, total: 1)
                        .tint(.cyan)
                        .progressViewStyle(.linear)
                }
                BalapWeb(model: page)
                    .overlay {
                        if page.failed {
                            VStack(spacing: 15) {
                                Image(systemName: "wifi.exclamationmark")
                                    .font(.system(size: 34))
                                    .foregroundColor(.orange)
                                Text("BALAP tidak dapat dibuka").font(.headline)
                                Text("Periksa internet atau balap.pbsgroup.id")
                                    .foregroundColor(.secondary)
                                    .multilineTextAlignment(.center)
                                Button("Coba lagi") { page.reload() }
                                    .buttonStyle(.borderedProminent)
                            }
                            .padding()
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                            .background(Color(uiColor: .systemBackground))
                        }
                    }
            }
            .background(Color(uiColor: .systemBackground))
            .alert("Informasi GPS", isPresented: $showGPS) {
                Button("Izinkan lokasi") { gps.request() }
                Button("Pengaturan") { gps.openSettings() }
                Button("Tutup", role: .cancel) {}
            } message: {
                Text(gps.diagnostic)
            }
        }
    }
}

struct BalapWeb: UIViewRepresentable {
    @ObservedObject var model: BalapPage

    func makeCoordinator() -> Coordinator { Coordinator(model) }

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.websiteDataStore = .default()
        configuration.defaultWebpagePreferences.allowsContentJavaScript = true
        configuration.preferences.javaScriptCanOpenWindowsAutomatically = false
        configuration.allowsInlineMediaPlayback = true

        let web = WKWebView(frame: .zero, configuration: configuration)
        web.navigationDelegate = context.coordinator
        web.uiDelegate = context.coordinator
        web.allowsBackForwardNavigationGestures = true
        web.scrollView.keyboardDismissMode = .interactive
        web.backgroundColor = .white
        web.isOpaque = false
        context.coordinator.web = web
        model.webView = web
        web.load(URLRequest(url: BalapPage.home))
        return web
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {
        // Preserve one WebView instance when device orientation or size changes.
    }

    final class Coordinator: NSObject, WKNavigationDelegate, WKUIDelegate {
        private let model: BalapPage
        weak var web: WKWebView?
        private var progressObserver: NSKeyValueObservation?

        init(_ model: BalapPage) { self.model = model }

        private func observeProgress(_ view: WKWebView) {
            if progressObserver != nil { return }
            progressObserver = view.observe(\.estimatedProgress, options: [.new]) { [weak self] page, _ in
                DispatchQueue.main.async { self?.model.progress = page.estimatedProgress }
            }
        }

        private func internalURL(_ url: URL) -> Bool {
            guard url.scheme?.lowercased() == "https",
                  let host = url.host?.lowercased() else { return false }
            return host == "balap.pbsgroup.id"
                || host == "script.google.com"
                || host == "script.googleusercontent.com"
                || host.hasSuffix(".script.googleusercontent.com")
                || host.hasSuffix("-script.googleusercontent.com")
        }

        func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction,
                     decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
            guard let url = navigationAction.request.url else {
                decisionHandler(.cancel); return
            }
            if let frame = navigationAction.targetFrame, !frame.isMainFrame {
                decisionHandler(.allow)
                return
            }
            if internalURL(url) || url.scheme == "about" {
                decisionHandler(.allow)
            } else {
                decisionHandler(.cancel)
                if ["https","http","tel","mailto"].contains(url.scheme?.lowercased() ?? "") {
                    DispatchQueue.main.async { UIApplication.shared.open(url) }
                }
            }
        }

        func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
            observeProgress(webView)
            DispatchQueue.main.async { self.model.loading = true; self.model.failed = false }
        }

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            DispatchQueue.main.async { self.model.loading = false }
        }

        func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!,
                     withError error: Error) {
            if (error as NSError).code == NSURLErrorCancelled { return }
            DispatchQueue.main.async { self.model.loading = false; self.model.failed = true }
        }

        func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
            DispatchQueue.main.async { self.model.loading = false }
        }

        func webView(_ webView: WKWebView, createWebViewWith configuration: WKWebViewConfiguration,
                     for navigationAction: WKNavigationAction,
                     windowFeatures: WKWindowFeatures) -> WKWebView? {
            if let url = navigationAction.request.url {
                if internalURL(url) {
                    webView.load(navigationAction.request)
                } else if ["https", "http"].contains(url.scheme?.lowercased() ?? "") {
                    UIApplication.shared.open(url)
                }
            }
            return nil
        }

        func webView(_ webView: WKWebView,
                     runJavaScriptAlertPanelWithMessage message: String,
                     initiatedByFrame frame: WKFrameInfo,
                     completionHandler: @escaping () -> Void) {
            guard let vc = UIApplication.shared.topController else { completionHandler(); return }
            let dialog = UIAlertController(title: frame.securityOrigin.host, message: message,
                                           preferredStyle: .alert)
            dialog.addAction(UIAlertAction(title: "OK", style: .default) { _ in completionHandler() })
            vc.present(dialog, animated: true)
        }

        func webView(_ webView: WKWebView,
                     runJavaScriptConfirmPanelWithMessage message: String,
                     initiatedByFrame frame: WKFrameInfo,
                     completionHandler: @escaping (Bool) -> Void) {
            guard let vc = UIApplication.shared.topController else { completionHandler(false); return }
            let dialog = UIAlertController(title: frame.securityOrigin.host, message: message,
                                           preferredStyle: .alert)
            dialog.addAction(UIAlertAction(title: "Batal", style: .cancel) { _ in completionHandler(false) })
            dialog.addAction(UIAlertAction(title: "OK", style: .default) { _ in completionHandler(true) })
            vc.present(dialog, animated: true)
        }

        func webView(_ webView: WKWebView,
                     runJavaScriptTextInputPanelWithPrompt prompt: String,
                     defaultText: String?, initiatedByFrame frame: WKFrameInfo,
                     completionHandler: @escaping (String?) -> Void) {
            guard let vc = UIApplication.shared.topController else { completionHandler(nil); return }
            let dialog = UIAlertController(title: frame.securityOrigin.host, message: prompt,
                                           preferredStyle: .alert)
            dialog.addTextField { $0.text = defaultText }
            dialog.addAction(UIAlertAction(title: "Batal", style: .cancel) { _ in completionHandler(nil) })
            dialog.addAction(UIAlertAction(title: "OK", style: .default) { _ in
                completionHandler(dialog.textFields?.first?.text)
            })
            vc.present(dialog, animated: true)
        }
    }
}

private extension UIApplication {
    var topController: UIViewController? {
        let scene = connectedScenes.compactMap { $0 as? UIWindowScene }.first
        var current = scene?.windows.first(where: { $0.isKeyWindow })?.rootViewController
        while let next = current?.presentedViewController { current = next }
        return current
    }
}
