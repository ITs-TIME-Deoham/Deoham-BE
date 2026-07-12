#!/bin/sh
set -e

# Ensure logs directory exists with proper permissions
mkdir -p /app/logs || { echo "Failed to create logs directory"; exit 1; }
chown -R app:app /app/logs || { echo "Failed to set logs directory ownership"; exit 1; }
chmod 755 /app/logs || { echo "Failed to set logs directory permissions"; exit 1; }

# Switch to app user and run Java
exec su-exec app:app java ${JAVA_OPTS} -jar /app/app.jar
