# syntax=docker/dockerfile:1@sha256:ecfaec9ed6d810b56388c508f4121597bfbba70d41a6dfeee4d8cad5f295fc32
FROM gradle:8.10.2-jdk17@sha256:c2900027f3f0681c2cbfb09d527813851ad67aeafbb409997297efa2df20e748

USER root
ARG ANDROID_SDK_LICENSES_ACCEPTED=false
ARG ANDROID_CMDLINE_TOOLS_URL=https://dl.google.com/android/repository/commandlinetools-linux-15859902_latest.zip
ARG ANDROID_CMDLINE_TOOLS_SHA256=4e4c464f145a7512b57d088ac6c278c03c9eea610886b35a5e0804e74eedf583
ENV ANDROID_SDK_ROOT=/opt/android-sdk \
    ANDROID_HOME=/opt/android-sdk \
    PATH=/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools:/opt/android-sdk/build-tools/35.0.0:$PATH \
    GRADLE_USER_HOME=/opt/gradle-cache

RUN apt-get update && apt-get install -y --no-install-recommends curl unzip ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /opt/android-sdk/cmdline-tools /opt/gradle-cache \
    && curl -fsSL "$ANDROID_CMDLINE_TOOLS_URL" -o /tmp/tools.zip \
    && echo "$ANDROID_CMDLINE_TOOLS_SHA256  /tmp/tools.zip" | sha256sum -c - \
    && unzip -q /tmp/tools.zip -d /opt/android-sdk/cmdline-tools \
    && mv /opt/android-sdk/cmdline-tools/cmdline-tools /opt/android-sdk/cmdline-tools/latest \
    && rm /tmp/tools.zip \
    && test "$ANDROID_SDK_LICENSES_ACCEPTED" = "true" \
    && yes | sdkmanager --licenses >/dev/null \
    && sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0" \
    && grep -qx 'Pkg.Revision=37.0.1' /opt/android-sdk/platform-tools/source.properties \
    && grep -qx 'Pkg.Revision=2' /opt/android-sdk/platforms/android-35/source.properties \
    && grep -qx 'Pkg.Revision=35.0.0' /opt/android-sdk/build-tools/35.0.0/source.properties \
    && mkdir -p /opt/benchmark \
    && keytool -genkeypair -noprompt -keystore /opt/benchmark/debug.keystore \
       -storepass android -keypass android -alias benchmark -keyalg RSA \
       -keysize 2048 -validity 36500 -dname "CN=BlackBoxBench Android,O=Benchmark,C=ZZ" \
    && keytool -exportcert -keystore /opt/benchmark/debug.keystore \
       -storepass android -alias benchmark -file /opt/benchmark/cert.der \
    && sha256sum /opt/benchmark/cert.der | cut -d ' ' -f 1 > /opt/benchmark/cert.sha256 \
    && chmod -R a+rX /opt/benchmark

COPY scaffold /opt/bootstrap
RUN cd /opt/bootstrap \
    && for attempt in 1 2 3; do \
         gradle --no-daemon --max-workers=2 assembleDebug && break; \
         test "$attempt" -lt 3 || exit 1; \
         echo "Gradle dependency warm-up failed; retrying ($attempt/3)" >&2; \
         sleep 3; \
       done \
    && rm -rf /opt/bootstrap/app/build /root/.gradle \
    && chmod -R a+rX /opt/android-sdk /opt/gradle-cache

RUN useradd -m -u 1000 agent || true \
    && chown -R 1000:1000 /opt/gradle-cache
USER 1000:1000
WORKDIR /workspace
CMD ["sh", "-lc", "while true; do sleep 3600; done"]
