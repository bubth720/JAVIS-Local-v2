#!/usr/bin/env bash
# Scoped tools/cache/CA for cloud builds; no models, APK installation or credential values.
set -euo pipefail
jarvis_tools="${JARVIS_TOOL_ROOT:-/workspace/.jarvis-core-tools}"
export JAVA_HOME="$jarvis_tools/jdk-17.0.16+8"
export ANDROID_HOME="$jarvis_tools/android-sdk"
export GRADLE_USER_HOME="$jarvis_tools/gradle-home"
export PATH="$JAVA_HOME/bin:$jarvis_tools/gradle-8.10.2/bin:$PATH"
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Duser.home=$jarvis_tools/user-home -Djavax.net.ssl.trustStore=$jarvis_tools/cacerts"
# Java (including Robolectric workers) does not use HTTPS_PROXY automatically.
jarvis_proxy_flags="$(python3 - <<'PY'
import os, urllib.parse
p=urllib.parse.urlparse(os.environ.get('HTTPS_PROXY') or os.environ.get('HTTP_PROXY',''))
if p.hostname and p.port:
 print(f'-Dhttp.proxyHost={p.hostname} -Dhttp.proxyPort={p.port} -Dhttps.proxyHost={p.hostname} -Dhttps.proxyPort={p.port}')
PY
)"
export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS $jarvis_proxy_flags"
exec "$@"
