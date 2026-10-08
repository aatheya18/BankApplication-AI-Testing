# Multi-stage build
FROM eclipse-temurin:17-jdk-alpine as builder

WORKDIR /build

# Copy source files
COPY Bank.java .
COPY BankAccount.java .
COPY BankingWebServer.java .

# Compile
RUN javac -d classes Bank.java BankAccount.java BankingWebServer.java

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy compiled classes from builder
COPY --from=builder /build/classes .

# Copy frontend
COPY index.html .

# Create non-root user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/api/health || exit 1

# Start application
CMD ["java", "-Dbank.port=8080", "com.nirma.banking.BankingWebServer"]
