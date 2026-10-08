# CI/CD Pipeline Setup Guide

## Overview

This project includes a complete CI/CD pipeline for automated build, test, and deployment. The pipeline uses GitHub Actions to orchestrate the workflow across three main jobs: build-and-test, build-docker, and deploy.

## Pipeline Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    GitHub Actions Workflow                  │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────────────┐                               │
│  │   build-and-test (Job)   │                               │
│  │  ✓ Checkout              │                               │
│  │  ✓ Setup Java 17         │                               │
│  │  ✓ Compile Source        │                               │
│  │  ✓ Run Tests + Coverage  │                               │
│  │  ✓ Upload Artifacts      │                               │
│  └──────────────┬───────────┘                               │
│                 │                                            │
│                 ▼                                            │
│  ┌──────────────────────────┐                               │
│  │   build-docker (Job)     │  (Requires: build-and-test)   │
│  │  ✓ Setup Docker Buildx   │                               │
│  │  ✓ Login to Registry     │                               │
│  │  ✓ Build Docker Image    │                               │
│  │  ✓ Push to Registry      │                               │
│  └──────────────┬───────────┘                               │
│                 │                                            │
│                 ▼                                            │
│  ┌──────────────────────────┐                               │
│  │   deploy (Job)           │  (Only on main, Requires: build-docker)
│  │  ✓ SSH to Server         │                               │
│  │  ✓ Pull Docker Image     │                               │
│  │  ✓ Start with docker-compose │                          │
│  │  ✓ Health Check          │                               │
│  └──────────────────────────┘                               │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## Triggers

The pipeline runs on:
- **Push to main or develop branches** → Full build, test, and deploy (if main)
- **Pull Requests to main or develop** → Build and test only (no deploy)

## Jobs Breakdown

### 1. `build-and-test`
- **Runs on:** ubuntu-latest
- **Purpose:** Compile source code and run unit tests with code coverage
- **Key Steps:**
  - Compiles `Bank.java`, `BankAccount.java`, `BankingWebServer.java`
  - Downloads JUnit Platform and JaCoCo
  - Compiles test files in `test/` directory
  - Runs tests with JaCoCo code coverage agent
  - Generates coverage reports (HTML + CSV)
  - Uploads artifacts for inspection

### 2. `build-docker`
- **Runs on:** ubuntu-latest
- **Depends on:** `build-and-test` (success required)
- **Purpose:** Build Docker image and push to GitHub Container Registry
- **Key Steps:**
  - Multi-stage Docker build (builder + runtime)
  - Uses `eclipse-temurin:17-jre-alpine` for small footprint
  - Runs only on **push to main** (not on PRs)
  - Pushes to `ghcr.io/aatheya18/bankapplication-ai-testing`
  - Tags images by branch, commit SHA, and version

### 3. `deploy`
- **Runs on:** ubuntu-latest
- **Depends on:** `build-docker` (success required)
- **Condition:** Only on **push to main** branch
- **Purpose:** Deploy Docker container to production server
- **Key Steps:**
  - Uses SSH to connect to deployment host
  - Pulls latest Docker image from registry
  - Restarts application using docker-compose
  - Runs health check on `/api/health` endpoint

## Configuration Requirements

### Secrets (Required for Deployment)

Add these secrets to your GitHub repository settings (`Settings > Secrets and variables > Actions`):

| Secret Name | Value | Example |
|---|---|---|
| `DEPLOY_HOST` | Hostname or IP of deployment server | `example.com` |
| `DEPLOY_USER` | SSH username | `deploy` |
| `DEPLOY_KEY` | SSH private key (PEM format) | (contents of `~/.ssh/id_rsa`) |
| `DEPLOY_PORT` | SSH port (optional, default: 22) | `2222` |

### Generating SSH Key for Deployment

```bash
# Generate SSH key pair
ssh-keygen -t rsa -b 4096 -f deploy_key -N ""

# Add public key to server's ~/.ssh/authorized_keys
cat deploy_key.pub | ssh user@host "cat >> ~/.ssh/authorized_keys"

# Copy private key content to GitHub Secrets as DEPLOY_KEY
cat deploy_key
```

### Server-Side Setup

On your deployment server:

```bash
# Create deployment directory
mkdir -p /opt/bank-app
cd /opt/bank-app

# Create docker-compose.yml (copy from repository)
# Make sure Docker and Docker Compose are installed
docker --version
docker-compose --version

# Test access
ssh user@host "echo 'SSH connection successful'"
```

## Local Testing

### Build and Test Locally

```bash
# Compile
javac -d out/classes Bank.java BankAccount.java BankingWebServer.java

# Run tests (requires PowerShell or bash with jq)
pwsh run_tests.ps1
# or
bash -c "chmod +x run_tests.ps1 && pwsh run_tests.ps1"

# View coverage report
open report_jacoco/index.html
```

### Run with Docker Locally

```bash
# Build Docker image
docker build -t bank-app:latest .

# Run container
docker run -p 8080:8080 bank-app:latest

# Test health endpoint
curl http://localhost:8080/api/health

# Or use docker-compose
docker-compose up -d
docker-compose logs -f
docker-compose down
```

## Artifact Outputs

The pipeline generates and uploads the following artifacts:

### Test Results Artifact: `test-results`
- `report_jacoco/` — HTML coverage report
- `jacoco.exec` — JaCoCo execution data
- `report_jacoco.csv` — CSV coverage metrics

Access artifacts:
1. Go to **Actions** tab in GitHub
2. Click on the workflow run
3. Download "test-results" or "coverage-report" artifact
4. Extract and open `report_jacoco/index.html` in browser

## Monitoring & Debugging

### View Workflow Logs

1. Navigate to **Actions** tab
2. Click on the workflow run
3. Expand each job to see detailed logs
4. Check for compilation errors, test failures, or deployment issues

### Common Issues

#### Docker login fails
- Check `GITHUB_TOKEN` permissions include `packages:write`
- Verify username format (usually your GitHub username)

#### Deployment fails (SSH connection)
- Verify `DEPLOY_HOST`, `DEPLOY_USER`, and `DEPLOY_KEY` are correct
- Test SSH manually: `ssh -i deploy_key user@host "echo test"`
- Ensure server has Docker and docker-compose installed

#### Tests fail in CI but pass locally
- Check Java version: CI uses Java 17
- PowerShell commands may differ on Linux (use `pwsh`)
- Ensure all source files are included in compile step

#### Docker image push fails
- Check GitHub Container Registry permissions
- Verify PAT has `write:packages` scope

## Customization

### Changing Deployment Target
Edit `deploy` job in `.github/workflows/build-test-deploy.yml`:
```yaml
deploy:
  runs-on: ubuntu-latest
  if: github.event_name == 'push' && github.ref == 'refs/heads/main'
  # ... modify SSH commands as needed
```

### Adding Notifications
Add Slack, email, or Teams notifications:
```yaml
- name: Notify deployment success
  if: success()
  uses: slackapi/slack-github-action@v1
  with:
    webhook-url: ${{ secrets.SLACK_WEBHOOK }}
```

### Modifying Coverage Thresholds
Edit `report_jacoco` generation in workflow:
```bash
java -jar $jacocoCli report jacoco.exec \
  --classfiles out/classes \
  --sourcefiles . \
  --html report_jacoco \
  --csv report_jacoco.csv \
  --xml report_jacoco.xml
```

## Security Best Practices

1. ✅ Use GitHub Secrets for sensitive data (no hardcoded passwords)
2. ✅ Restrict deployments to main branch only
3. ✅ Use SSH key-based authentication for server access
4. ✅ Run containers as non-root user (appuser)
5. ✅ Enable HEALTHCHECK in Dockerfile
6. ✅ Rotate SSH keys periodically
7. ✅ Review workflow logs for suspicious activity

## Next Steps

1. **Add the secrets** to your repository (see Configuration Requirements)
2. **Commit and push** the workflow files to main
3. **Verify the workflow** runs in the Actions tab
4. **Check test results** and coverage reports
5. **Configure deployment** secrets for production use

## Support

For issues or questions:
- Check GitHub Actions documentation: https://docs.github.com/actions
- Review workflow logs in the Actions tab
- Verify all secrets and environment variables are set correctly
