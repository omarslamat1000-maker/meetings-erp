# ============================================================
#  Dockerfile - منصة متابعة منظومة اجتماعات نائب الأمين
#  بناء متعدد المراحل: Maven للبناء ثم JRE لتشغيل الحزمة
# ============================================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/meetings-erp.jar app.jar
# منصات الاستضافة تمرّر المنفذ عبر المتغيّر PORT
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["sh","-c","java -jar app.jar --server.port=${PORT}"]
