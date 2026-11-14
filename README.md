# 차담

주변 차량에 짧은 메시지를 보내는 **Pleos Connect 차량용 앱 프로토타입**입니다. 차량 화면에서 앞·뒤·왼쪽·오른쪽의 가상 차량을 고르고, “신호가 바뀌었습니다”처럼 미리 정해진 문구를 보냅니다. 받는 차량에는 자신을 기준으로 보낸 차량의 방향이 표시되며, 수락하면 메시지를 확인할 수 있습니다.

## 시연 화면

| 주변 차량 선택 | 대상과 문구 확인 |
| --- | --- |
| ![내 차를 중심으로 표시된 네 대 중 앞 차량을 선택한 화면](docs/screenshots/vehicle-picker.png) | ![앞 차량에 신호 변경 안내를 보내기 전 확인하는 화면](docs/screenshots/send-confirmation.png) |

| 수신 알림 | 수락 후 메시지 |
| --- | --- |
| ![뒤 차량에서 메시지가 도착했다는 알림과 수락 및 거절 버튼](docs/screenshots/incoming-message.png) | ![보낸 차량의 방향과 신호 변경 문구가 표시된 화면](docs/screenshots/accepted-message.png) |

## APK 다운로드

Pleos 에뮬레이터에서 시연할 수 있는 [차담 디버그 APK](apk/chadam-debug.apk)입니다.

## 실행 방법

1. Android Studio에서 이 저장소의 루트 폴더를 엽니다.
2. Gradle JDK를 **17 또는 21**로 지정하고 SDK Manager에서 **Android SDK Platform 34**를 설치합니다.
3. [Pleos Connect 에뮬레이터 설치 안내](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator)에 따라 에뮬레이터를 설정합니다.
4. 실행 대상으로 Pleos Connect 에뮬레이터를 선택하고 Android Studio에서 **Run app**을 누릅니다. 버튼 시연은 API 28 이상의 일반 Android 에뮬레이터에서도 실행할 수 있습니다.

PowerShell에서 디버그 APK를 빌드하려면 호환 JDK와 Android SDK를 설정한 상태에서 실행합니다.

```powershell
.\gradlew.bat assembleDebug
```

APK는 `app/build/outputs/apk/debug/app-debug.apk`에 생성됩니다.

## 사용 방법

1. **메시지 보내기**를 누르고 화면에서 **앞 차량** 모양을 선택합니다.
2. **신호가 바뀌었습니다.**를 선택하고 대상과 문구를 확인한 뒤 **보내기**를 누릅니다.
3. 가상 수신 화면에서 **뒤 차량에서 메시지가 왔습니다**라는 안내가 표시됩니다. **수락**하면 문구가 공개되고, **거절**하거나 20초 동안 응답하지 않으면 종료됩니다.

음성을 시험하려면 **음성 시연 켜기**를 누르고 마이크 권한을 허용합니다. 앱 화면에서 “메시지 전송 → 앞 차량 → 신호 바뀜 → 보내 → 확인”을 한 단계씩 말합니다. 이 모드에는 에뮬레이터의 PC 마이크 입력과 Android 한국어 음성 인식 서비스가 필요합니다. 한국어 음성 합성이 지원되면 앱이 안내 문구를 읽어줍니다.

## 구현 범위

- **환경** — Kotlin, Android 기본 View, Gradle Wrapper, Pleos Connect 에뮬레이터
- **시뮬레이션** — 한 앱 세션에서 가상 차량 목록과 송신·수신 화면을 처리합니다. 발신 방향은 시연 차량들이 같은 진행 방향을 바라보는 조건에서 계산합니다.
- **검증** — Pleos 에뮬레이터에서 버튼으로 차량 선택, 전송, 발신 방향 표시, 수락 후 문구 표시를 확인했습니다.
- **후속 개발** — 실차 ADAS 추적과 수신 차량 매칭, 차량 간 통신, 긴급차량 접근 알림, 차량 상태 제보용 사진 첨부를 검토합니다.
