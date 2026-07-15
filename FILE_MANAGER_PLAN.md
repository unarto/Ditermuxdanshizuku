# Rencana Pengembangan File Manager (Termux & Shizuku Hybrid)

**Oleh:** Senior Systems Architect / Modular Code Specialist
**Target:** Modul File Manager Terintegrasi dengan Basic (Native) & Advanced (Termux Script) Operations
**Status:** DRAFT DESAIN (Menunggu Eksekusi)

---

## 🎯 1. KONSEP ARSITEKTUR HYBRID
File Manager ini akan memadukan kapabilitas:
1.  **Native File I/O (Basic):** Operasi dasar menggunakan Kotlin `java.io.File`.
2.  **Termux Scripting (Advanced):** Eksekusi shell (Shizuku/rish) untuk archive, git, dan recursive scripting di background.
3.  **Storage Access Framework (SAF) Bridge:** Jembatan (Interface JS) untuk aplikasi Acode/WebView agar dapat merender dan mengedit file eksternal (Android 11+ restricted dirs) dengan aman melalui `DocumentFile`.
4.  **Virtual File System (VFS):** Abstraksi (VFile) untuk menyatukan operasi antara Native I/O (Jalur Cepat) dan SAF (Jalur Aman).

---

## 📂 2. PENGELOMPOKAN FITUR (MODULAR LAYOUT)

### A. Basic Operations (Native Kotlin / Shizuku Privilege)
*   Move, Copy, Delete, Rename, Create.
*   Properties & Permission Management.

### B. Advanced Context Menu (Termux Script / Shell Execution)
*   **Archive Management:** Unpack (.zip, .tar, .7z) & Repack.
*   **Git Operations:** Clone, Commit & Push.
*   **Find & Replace (Recursive):** Eksekusi script Python di backend Termux untuk rename & edit isi teks file secara masif dan rekursif.

### C. SAF Bridge & VFS
*   Menghubungkan Web API (Promise-based) dengan VFS (VFile).
*   Abstraksi VFile yang memiliki dua implementasi: `NativeFile` dan `SafFile`.

---

## 🛠️ 3. RENCANA IMPLEMENTASI TEKNIS (ROADMAP)

### Fase 1: Persiapan UI & Komponen Dasar File Manager (SELESAI)
### Fase 2: Integrasi Context Menu & Bottom Sheet (SELESAI)
### Fase 3: Jembatan Eksekusi Termux (Script Executor) (SELESAI)
### Fase 4: Handling Dependensi Termux (SELESAI)
### Fase 5: Integrasi Storage Access Framework (SAF) Bridge (SELESAI)
### Fase 6: Abstraksi VFS & Perutean Cerdas (SELESAI)
### Fase 7: Integrasi UI SAF (Titik Pemasangan) (SELESAI)
1.  Menggunakan `ActivityResultContracts.OpenDocumentTree()` untuk meminta akses ke folder/memori eksternal (SD Card / USB).
2.  Menambahkan fungsi `addSafStorage` di ViewModel untuk menyimpan URI yang disetujui (via `SharedPreferences`) dan mengambil `PersistableUriPermission`.
3.  Menampilkan tombol "Tambah Penyimpanan" di layar beranda (home) File Manager menggunakan `ExtendedFloatingActionButton`.

### Fase 8: Integrasi Document Provider Lintas Aplikasi (Cross-App SAF)
1. ~~**Sistem Picker (SAF)**: Memanfaatkan `ACTION_OPEN_DOCUMENT_TREE` (System Picker) untuk memungkinkan pengguna memilih *Document Provider* dari aplikasi lain (seperti Google Drive, Acode, dll. sesuai screenshot).~~ (SELESAI)
2. ~~**Persistent Mounting**: Menyimpan URI dari aplikasi eksternal beserta `PersistableUriPermission` agar tidak perlu meminta izin berulang kali.~~ (SELESAI)
3. ~~**Representasi Home UI**: Menampilkan direktori/sumber data eksternal ini sebagai "Virtual Root" di menu utama File Manager, sejajar dengan Internal Storage.~~ (SELESAI)
4. ~~**VFS Seamless Routing**: Karena kita sudah membangun `SafFile` dan `SdcardBridge`, akses ke URI aplikasi lain akan otomatis di-handle secara transparan oleh VFS tanpa perlu merombak logic I/O.~~ (SELESAI)

---

## 📌 FASE 9: PENGEMBANGAN FITUR WAJIB (MANDATORY) FILE MANAGER
*(Status: Direncanakan - Implementasi Code Editor / Text Editor DITUNDA)*

Untuk mencapai standar File Manager yang matang (setara MT Manager/ZArchiver), fitur-fitur esensial berikut wajib diimplementasikan:

1. **Multi-Selection (Pilih Banyak File):**
   - Mendukung seleksi banyak file/folder sekaligus (Long press lalu tap item lain).
   - Operasi batch: Copy, Move, Delete, Archive (ZIP/7Z/TAR) untuk semua item yang dipilih.

2. **Sistem Pengurutan (Sorting):**
   - Mengurutkan berdasarkan: Nama, Ukuran, Tanggal Modifikasi, Jenis/Ekstensi.
   - Opsi urutan: Ascending (Menaik) & Descending (Menurun).

3. **Breadcrumb Navigation (Navigasi Path Interaktif):**
   - Mengganti TopAppBar teks statis dengan navigasi Breadcrumb yang bisa diklik (klik nama folder sebelumnya untuk langsung lompat/kembali ke path tersebut).

4. **Grid & List View Toggle:**
   - Opsi untuk mengubah mode tampilan item dari List (Baris detail) ke Grid (Kotak thumbnail besar).

5. **Storage Info & Stats (Kapasitas Penyimpanan):**
   - Menampilkan indikator (progress bar) ruang kosong vs terpakai di layar "Home" untuk Internal Storage & SAF External.

6. **Properties & Hash Checker (Detail Ekstensif):**
   - Dialog "Detail" yang dapat menghitung ukuran folder (rekursif) secara asinkron.
   - Perhitungan Hash (MD5, SHA-1, SHA-256) untuk verifikasi integritas file (berguna bagi modder/developer).

7. **Bookmark / Pinned Folders:**
   - Pengguna dapat mem-pin folder tertentu ke layar "Home" agar dapat diakses dengan cepat.

8. **Thumbnail & Media Preview:**
   - Menampilkan thumbnail asli untuk Gambar, Video, dan mengekstrak ikon untuk file `.apk`.

9. **Penanganan Symlink (Symbolic Links):**
   - Indikator visual khusus untuk file/folder symlink (penting di environment Termux/Linux).

---

## 📌 STATE SUMMARY

- **Current Task:** Implementasi ekstensi Fase 9 (Multi-selection, Sorting, Breadcrumbs) selesai.
- **Active Variables/Modules:** `FILE_MANAGER_PLAN.md`
- **Pending Tasks:** 
  1. Mulai implementasi Fase 9 secara bertahap (Multi-selection, Sorting, Breadcrumbs).
  2. (DITUNDA) Integrasi dengan Code Editor / Text Editor.
- **Status:** KONSISTEN & TERDOKUMENTASI. Blueprint untuk fitur standar file manager telah disiapkan.

## 📝 HASIL AUDIT CONTEXT MENU (KEKURANGAN FITUR)
Berdasarkan audit pada `FileManagerScreen.kt` (Bottom Sheet Context Menu), berikut adalah fitur-fitur yang masih kurang atau belum diimplementasikan sepenuhnya sesuai spesifikasi dan standar File Manager:

1. ~~**Buka / Buka Dengan (Open With):** Belum ada opsi untuk membuka file (ACTION_VIEW) ke aplikasi eksternal (misal: Text Editor, Image Viewer).~~ (SELESAI - Ditambahkan di FileContextMenuBottomSheet)
2. ~~**Bagikan (Share):** Belum ada opsi untuk membagikan file (ACTION_SEND).~~ (SELESAI)
3. ~~**Salin Path (Copy Path):** Tidak ada opsi untuk menyalin path absolut direktori/file ke clipboard.~~ (SELESAI)
4. ~~**Git Commit & Push:** Di plan (Bagian 2.B) dijanjikan fitur Git Clone, Commit, dan Push, namun di UI baru tersedia "Git Clone di sini".~~ (SELESAI)
5. ~~**Manajemen Izin (Custom chmod):** Opsi ubah izin saat ini hanya *hardcoded* `chmod 777`. Perlu dialog kustom untuk centang izin R/W/X.~~ (SELESAI - Menggunakan ChmodDialog terpisah)
6. ~~**Buka di Terminal (Open in Terminal):** Jika item adalah folder, seharusnya ada opsi cepat membuka sesi Rish/Terminal langsung di path tersebut.~~ (SELESAI)
7. ~~**Kompres ke format lain (7z):** Baru tersedia kompresi ke ZIP dan TAR.GZ.~~ (SELESAI - Tambah 7Z via Termux command)
8. ~~**Repack .apk (jika relevan):** Berkaitan dengan Termux script / reverse engineering.~~ (SELESAI - via apktool b)

## Update: 2026-07-14
- [x] Refactor Dropdown Context Menu file manager (Modular). Logika Bottom Sheet dikeluarkan dari `FileManagerScreen.kt` ke `components/FileContextMenuBottomSheet.kt`.
- [x] Menyelesaikan semua kekurangan Context Menu (Open With, Share, Copy Path, Git Commit & Push, Custom Chmod Dialog, Open in Terminal, Compress to 7z, Repack APK).
- [x] Refactor `SdcardBridge` untuk menggunakan abstraksi `VFile` (`SafFile`) alih-alih mengandalkan `DocumentFile` secara langsung, menyeragamkan pola akses untuk SAF. 
- [x] Implementasi Exception-based error handling untuk operasi I/O SAF (openInputStream/openOutputStream) di VFS.
- [x] Menghapus cache `.cxx` sesuai permintaan.
- [x] Implementasi UI & Logika untuk trigger System Picker SAF (Cross-App) via `ExtendedFloatingActionButton`.
- [x] Refinement tampilan VFS SafURI di `FileItemRow` (icon package) dan nama (tanpa "SAF:").
- [x] Menambahkan jalur data Termux ke Virtual Root di Home File Manager.
- [x] Menambahkan tombol ikon pengaturan khusus Termux di TopAppBar (`FileManagerScreen`).
- [x] Membuat kelas `TermuxSettingsDialog` secara terpisah agar mudah dikelola. (Implementasi isi pengaturannya DITUNDA).
- [x] Implementasi fitur Multi-selection (Batch operations: Copy, Move, Delete).
- [x] Implementasi fitur Sorting (Nama, Ukuran, Tanggal, Ekstensi dengan urutan Menaik/Menurun).
- [x] Implementasi fitur Breadcrumb Navigation Interaktif di TopAppBar.
