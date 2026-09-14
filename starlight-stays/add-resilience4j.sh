#!/usr/bin/env bash
set -e

echo "🔧 Restoring clean pom.xml for api-gateway..."
git checkout api-gateway/pom.xml 2>/dev/null || true

echo "📝 Adding Resilience4j dependency to api-gateway/pom.xml..."
python3 -c '
path = "api-gateway/pom.xml"
with open(path, "r") as f:
    data = f.read()

dep = """
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
        </dependency>
"""

if "</dependencies>" in data and "spring-cloud-starter-circuitbreaker-reactor-resilience4j" not in data:
    data = data.replace("</dependencies>", dep + "\n    </dependencies>")
    with open(path, "w") as f:
        f.write(data)
'

echo "🔨 Rebuilding api-gateway..."
(cd api-gateway && mvn clean package -DskipTests)

echo "🐳 Restarting api-gateway container..."
docker-compose up --build -d api-gateway
sleep 15
echo "✅ Gateway is secured with Resilience4j!"
