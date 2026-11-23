FROM gradle:8.5-jdk17 AS builder
WORKDIR /app

COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src

RUN gradle war --no-daemon -Dorg.gradle.internal.http.socketTimeout=120000 -Dorg.gradle.internal.http.connectionTimeout=120000

FROM quay.io/wildfly/wildfly:27.0.1.Final-jdk17
COPY --from=builder /app/build/libs/ROOT.war /opt/jboss/wildfly/standalone/deployments/ROOT.war

# лимит оперативы в 300мб, чтобы не кушало слишком много (ибо и не надо много)
ENV JAVA_OPTS="-server -Xms64m -Xmx300m -XX:MetaspaceSize=64M -XX:MaxMetaspaceSize=128m -Djava.net.preferIPv4Stack=true -Djboss.modules.system.pkgs=org.jboss.byteman -Djava.awt.headless=true"

CMD ["/bin/sh", "-c", "/opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0 -Djboss.http.port=${PORT:-8080}"]

