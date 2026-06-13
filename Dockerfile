FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# 1. Salin file konfigurasi Maven Wrapper agar Docker bisa mengunduh dependensi
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# 2. Jalankan perintah ini agar Docker mengunduh semua library pom.xml di awal
# Ini adalah trik Caching Layer agar proses berikutnya berjalan sangat cepat
RUN ./mvnw dependency:go-offline

# Catatan: Kita sengaja TIDAK menyalin folder src/ di sini.
# Folder src/ akan kita masukkan secara real-time lewat jembatan BIND MOUNT saat run.

EXPOSE 8080

# 3. Perintah untuk menyalakan Spring Boot langsung dari source code (Mode Development)
CMD ["./mvnw", "spring-boot:run"]