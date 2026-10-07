# Noor Maktab — Android App

Duaen (57), lafz-ba-lafz highlight, Hindi talaffuz aur tarjuma, phone ki Arabic awaaz, qari ki recordings.

## APK banane ka tareeqa 1: sirf mobile se (GitHub, muft)

1. github.com par account banayein aur **New repository** banayein (naam: `NoorMaktab`).
2. **Add file → Upload files** dabayein aur is zip ke andar ki saari files aur folders upload karein
   (`.github` folder bhi zaroor upload hona chahiye).
3. Upload hote hi **Actions** tab mein "Build APK" apne aap chalega (5–8 minute).
4. Hara ✓ aane par us run ko kholein, neeche **Artifacts → NoorMaktab-APK** download karein.
5. Zip kholkar `app-debug.apk` install karein. Phone "Unknown apps" ki ijaazat maangega, de dein.

## Tareeqa 2: computer par Android Studio se

1. Android Studio mein **File → Open** karke `NoorMaktab` folder kholein.
2. Sync khatam hone par **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
3. APK `app/build/outputs/apk/debug/` mein milegi.

## Qari ki recordings app ke andar daalna

`app/src/main/assets/audio/` folder mein recordings rakhein, naam dua number se:
`01.mp3`, `02.mp3`, `03.mp3` … (app mein har dua ke aage uska number likha hai).
Phir APK dobara banayein. Ye recordings bina internet ke, apne aap har dua ke saath bajengi.

App ke andar se bhi recording lagayi ja sakti hai (Audio card). Woh phone mein save rehti hai.

## Arabic awaaz

App phone ka text-to-speech engine istemal karti hai. Saaf awaaz ke liye:
Settings → "Text-to-speech" → Google speech engine → Install voice data → **Arabic** download karein.
App mein Audio card se "Natural" wali awaaz chunein.
