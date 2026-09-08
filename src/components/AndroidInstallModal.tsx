import React, { useState, useEffect } from 'react';
import { Smartphone, Download, CheckCircle2, ArrowRight, Shield, QrCode, ExternalLink, X, Terminal, Cpu } from 'lucide-react';
import QRCode from 'qrcode';

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>;
}

interface AndroidInstallModalProps {
  isOpen: boolean;
  onClose: () => void;
  deferredPrompt: BeforeInstallPromptEvent | null;
  onInstalled?: () => void;
}

export const AndroidInstallModal: React.FC<AndroidInstallModalProps> = ({
  isOpen,
  onClose,
  deferredPrompt,
  onInstalled,
}) => {
  const [activeTab, setActiveTab] = useState<'instant' | 'native' | 'pwabuilder'>('instant');
  const [qrDataUrl, setQrDataUrl] = useState<string>('');
  const [isInstalling, setIsInstalling] = useState<boolean>(false);
  const [installSuccess, setInstallSuccess] = useState<boolean>(false);

  const currentUrl = typeof window !== 'undefined' ? window.location.href : '';

  useEffect(() => {
    if (isOpen && currentUrl) {
      QRCode.toDataURL(currentUrl, {
        width: 240,
        margin: 1,
        color: {
          dark: '#06B6D4',
          light: '#0F172A',
        },
      })
        .then((url) => setQrDataUrl(url))
        .catch(() => {});
    }
  }, [isOpen, currentUrl]);

  if (!isOpen) return null;

  const handleTriggerInstall = async () => {
    if (!deferredPrompt) return;
    try {
      setIsInstalling(true);
      await deferredPrompt.prompt();
      const choice = await deferredPrompt.userChoice;
      if (choice.outcome === 'accepted') {
        setInstallSuccess(true);
        if (onInstalled) onInstalled();
      }
    } catch {
      // Ignored
    } finally {
      setIsInstalling(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/85 backdrop-blur-sm animate-fade-in">
      <div className="relative w-full max-w-2xl rounded-2xl bg-slate-900 border border-cyan-500/40 shadow-2xl shadow-cyan-950/60 overflow-hidden flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="p-5 bg-gradient-to-r from-slate-950 via-slate-900 to-cyan-950/40 border-b border-cyan-500/20 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-cyan-500/15 border border-cyan-400/30 text-cyan-300">
              <Smartphone className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base md:text-lg font-bold text-white flex items-center gap-2">
                Install TrustPay on Android
                <span className="text-[11px] px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 font-semibold">
                  APK Ready
                </span>
              </h3>
              <p className="text-xs text-slate-300 mt-0.5">
                Two verified options: Instant WebAPK direct installation or Native Gradle APK build
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800/80 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tab switcher */}
        <div className="flex border-b border-slate-800 bg-slate-950/60 p-1.5 gap-1.5">
          <button
            onClick={() => setActiveTab('instant')}
            className={`flex-1 py-2 px-3 rounded-lg text-xs font-semibold flex items-center justify-center gap-1.5 transition-all ${
              activeTab === 'instant'
                ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40 shadow-sm'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'
            }`}
          >
            <Smartphone className="w-3.5 h-3.5" />
            1. Instant Install (WebAPK)
          </button>

          <button
            onClick={() => setActiveTab('pwabuilder')}
            className={`flex-1 py-2 px-3 rounded-lg text-xs font-semibold flex items-center justify-center gap-1.5 transition-all ${
              activeTab === 'pwabuilder'
                ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40 shadow-sm'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'
            }`}
          >
            <Download className="w-3.5 h-3.5" />
            2. Package Signed APK
          </button>

          <button
            onClick={() => setActiveTab('native')}
            className={`flex-1 py-2 px-3 rounded-lg text-xs font-semibold flex items-center justify-center gap-1.5 transition-all ${
              activeTab === 'native'
                ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40 shadow-sm'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'
            }`}
          >
            <Terminal className="w-3.5 h-3.5" />
            3. Native Android Project
          </button>
        </div>

        {/* Content body */}
        <div className="p-5 overflow-y-auto space-y-4 text-xs md:text-sm text-slate-200">
          {activeTab === 'instant' && (
            <div className="space-y-4">
              <div className="p-4 rounded-xl bg-slate-950/70 border border-slate-800 flex flex-col md:flex-row items-center gap-5">
                {qrDataUrl && (
                  <div className="flex flex-col items-center bg-slate-900 p-3 rounded-xl border border-cyan-500/30">
                    <img src={qrDataUrl} alt="TrustPay URL QR Code" className="w-36 h-36 rounded-lg" />
                    <span className="text-[10px] text-cyan-400 mt-2 font-mono flex items-center gap-1">
                      <QrCode className="w-3 h-3" /> Scan on Android
                    </span>
                  </div>
                )}

                <div className="space-y-2.5 flex-1">
                  <div className="flex items-center gap-2 text-cyan-300 font-bold text-sm">
                    <Shield className="w-4 h-4 text-cyan-400" />
                    Direct Android Home Screen Installation
                  </div>
                  <p className="text-xs text-slate-300 leading-relaxed">
                    TrustPay is fully Progressive Web App (PWA) compliant. Android Chrome automatically compiles it into a native <strong>WebAPK</strong> with its own app icon, splash screen, camera & hardware sensor permissions, and full offline caching.
                  </p>

                  {deferredPrompt ? (
                    <button
                      onClick={handleTriggerInstall}
                      disabled={isInstalling || installSuccess}
                      className="w-full mt-2 py-2.5 px-4 rounded-xl bg-gradient-to-r from-cyan-500 to-sky-600 hover:from-cyan-400 hover:to-sky-500 text-slate-950 font-bold text-xs flex items-center justify-center gap-2 shadow-lg shadow-cyan-500/20 transition-all"
                    >
                      {installSuccess ? (
                        <>
                          <CheckCircle2 className="w-4 h-4 text-emerald-950" />
                          Installed Successfully!
                        </>
                      ) : (
                        <>
                          <Smartphone className="w-4 h-4" />
                          {isInstalling ? 'Opening Installer...' : 'Install TrustPay to Android Now'}
                        </>
                      )}
                    </button>
                  ) : (
                    <div className="p-3 rounded-lg bg-slate-900 border border-slate-700/60 text-xs space-y-1.5">
                      <div className="font-semibold text-slate-200">How to install directly from your Android phone:</div>
                      <ol className="list-decimal list-inside space-y-1 text-slate-300">
                        <li>Open this URL in Chrome or your Android browser.</li>
                        <li>Tap the three dots (<strong className="text-white">⋮</strong>) in the top-right corner.</li>
                        <li>Tap <strong className="text-cyan-300">“Install app”</strong> or <strong className="text-cyan-300">“Add to Home screen”</strong>.</li>
                        <li>Android automatically packages and installs the TrustPay APK directly to your app launcher!</li>
                      </ol>
                    </div>
                  )}
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5 text-xs">
                <div className="p-3 rounded-xl bg-slate-950/40 border border-slate-800">
                  <div className="font-semibold text-cyan-300 mb-1 flex items-center gap-1.5">
                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                    Standalone App
                  </div>
                  <div className="text-slate-400 text-[11px]">Hides browser chrome and runs full-screen like a standard native app.</div>
                </div>

                <div className="p-3 rounded-xl bg-slate-950/40 border border-slate-800">
                  <div className="font-semibold text-cyan-300 mb-1 flex items-center gap-1.5">
                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                    Camera & Sensors
                  </div>
                  <div className="text-slate-400 text-[11px]">Full access to device camera for high-speed QR scanning and touch biometrics.</div>
                </div>

                <div className="p-3 rounded-xl bg-slate-950/40 border border-slate-800">
                  <div className="font-semibold text-cyan-300 mb-1 flex items-center gap-1.5">
                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                    Offline Ready
                  </div>
                  <div className="text-slate-400 text-[11px]">Service worker caches all security assets, crypto engines, and UI locally.</div>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'pwabuilder' && (
            <div className="space-y-3.5">
              <div className="p-4 rounded-xl bg-slate-950/70 border border-slate-800 space-y-3">
                <div className="flex items-center gap-2 text-cyan-300 font-bold text-sm">
                  <Cpu className="w-4 h-4 text-cyan-400" />
                  Generate Standalone .APK via PWABuilder (No Code / 1-Minute)
                </div>
                <p className="text-xs text-slate-300 leading-relaxed">
                  PWABuilder (maintained by Microsoft and Google) converts verified PWA manifests into signed, standalone Android APK and AAB packages for sideloading or Google Play Store publishing.
                </p>

                <div className="p-3 rounded-lg bg-slate-900 border border-cyan-500/20 font-mono text-xs text-cyan-300 break-all select-all">
                  {currentUrl}
                </div>

                <div className="space-y-2 text-xs text-slate-300">
                  <div className="font-semibold text-slate-200">Three quick steps:</div>
                  <div className="flex items-start gap-2">
                    <span className="w-5 h-5 rounded-full bg-cyan-500/20 text-cyan-300 flex items-center justify-center font-bold text-[11px] shrink-0">1</span>
                    <span>Copy your TrustPay application URL above.</span>
                  </div>
                  <div className="flex items-start gap-2">
                    <span className="w-5 h-5 rounded-full bg-cyan-500/20 text-cyan-300 flex items-center justify-center font-bold text-[11px] shrink-0">2</span>
                    <span>Visit <strong>PWABuilder.com</strong> and paste the URL.</span>
                  </div>
                  <div className="flex items-start gap-2">
                    <span className="w-5 h-5 rounded-full bg-cyan-500/20 text-cyan-300 flex items-center justify-center font-bold text-[11px] shrink-0">3</span>
                    <span>Click <strong>“Package for Android”</strong> and download your generated <code className="text-cyan-400">trustpay-debug.apk</code>.</span>
                  </div>
                </div>

                <a
                  href={`https://www.pwabuilder.com?url=${encodeURIComponent(currentUrl)}`}
                  target="_blank"
                  rel="noreferrer"
                  className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-bold text-xs shadow-md transition-colors"
                >
                  Open PWABuilder with TrustPay URL
                  <ExternalLink className="w-3.5 h-3.5" />
                </a>
              </div>
            </div>
          )}

          {activeTab === 'native' && (
            <div className="space-y-3.5">
              <div className="p-4 rounded-xl bg-slate-950/70 border border-slate-800 space-y-3">
                <div className="flex items-center gap-2 text-cyan-300 font-bold text-sm">
                  <Terminal className="w-4 h-4 text-cyan-400" />
                  Native Kotlin/Gradle Android Project (/app)
                </div>
                <p className="text-xs text-slate-300 leading-relaxed">
                  This repository already includes the native Android codebase in <code className="text-cyan-300">/app</code> featuring CameraX ML Kit Barcode Scanner, BiometricPrompt, and Kotlin Coroutines.
                </p>

                <div className="space-y-2">
                  <div className="text-xs font-semibold text-slate-300">Build with Android Studio or Terminal:</div>
                  <div className="p-3 rounded-lg bg-slate-950 font-mono text-[11px] text-slate-300 space-y-2 border border-slate-800">
                    <div className="text-slate-400"># 1. Export project via AI Studio Settings &gt; Export to ZIP or GitHub</div>
                    <div className="text-cyan-300">$ cd Trust-Pay-Ai</div>
                    <div className="text-slate-400"># 2. Compile debug APK directly using Gradle Wrapper</div>
                    <div className="text-emerald-400">$ ./gradlew assembleDebug</div>
                    <div className="text-slate-400"># 3. Output APK location:</div>
                    <div className="text-cyan-400">app/build/outputs/apk/debug/app-debug.apk</div>
                  </div>
                </div>

                <div className="p-3 rounded-lg bg-slate-900 border border-slate-700/60 text-xs text-slate-300">
                  <div className="font-semibold text-slate-200 mb-1">To sideload the APK onto your Android phone:</div>
                  <div className="font-mono text-[11px] text-cyan-300">$ adb install app/build/outputs/apk/debug/app-debug.apk</div>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="p-4 bg-slate-950 border-t border-slate-800 flex items-center justify-between">
          <div className="text-[11px] text-slate-400 flex items-center gap-1.5">
            <Shield className="w-3.5 h-3.5 text-cyan-400" />
            <span>SHA-256 payload integrity &amp; biometric protection active</span>
          </div>
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 font-semibold text-xs transition-colors"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};
