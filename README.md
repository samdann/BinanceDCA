# EC2 Deployment for Binance DCA

This module deploys a daily DCA (Dollar Cost Averaging) service on EC2.

## Architecture

- **EC2 Instance**: t4g.nano (ARM-based, ~$3/month)
- **Elastic IP**: Static IP for Binance whitelist
- **Systemd Timer**: Runs daily at 9:00 AM UTC
- **Java 17**: Runtime environment

## Initial Setup

### 1. Launch EC2 Instance

```bash
# Using AWS CLI
aws ec2 run-instances \
  --image-id ami-xxxxxxxxx \
  --instance-type t4g.nano \
  --key-name your-key-name \
  --security-group-ids sg-xxxxxxxxx \
  --subnet-id subnet-xxxxxxxxx
```

Or use the AWS Console:

- AMI: Amazon Linux 2023 (ARM64)
- Instance type: t4g.nano
- Storage: 8GB (default)

### 2. Allocate and Attach Elastic IP

```bash
# Allocate Elastic IP
aws ec2 allocate-address --domain vpc

# Associate with instance
aws ec2 associate-address \
  --instance-id i-xxxxxxxxx \
  --allocation-id eipalloc-xxxxxxxxx
```

### 3. Note the Elastic IP

```bash
# Get your Elastic IP
aws ec2 describe-addresses --allocation-ids eipalloc-xxxxxxxxx
```

**Whitelist this IP on Binance!**

### 4. Setup EC2 Instance

SSH to your instance:

```bash
ssh -i your-key.pem ec2-user@your-elastic-ip
```

Run setup script:

```bash
curl -O https://raw.githubusercontent.com/your-repo/ec2/setup-ec2.sh
chmod +x setup-ec2.sh
./setup-ec2.sh
```

### 5. Configure Environment

Edit the .env file:

```bash
nano ~/binance-dca/.env
```

Add your Binance API credentials:

```
BINANCE_API_KEY=your_actual_api_key
BINANCE_API_SECRET=your_actual_api_secret
```

## Deployment

From your local machine:

```bash
# Set EC2 connection details
export EC2_HOST=your-elastic-ip
export EC2_USER=ec2-user

# Deploy
cd binance-order-service
./ec2/deploy.sh
```

## Management Commands

### Check timer status

```bash
ssh ec2-user@your-elastic-ip 'sudo systemctl status binance-dca.timer'
```

### View logs

```bash
ssh ec2-user@your-elastic-ip 'sudo journalctl -u binance-dca -f'
```

or could be streamed to CloudWatch

nan### Run manually (test)

```bash
ssh ec2-user@your-elastic-ip 'sudo systemctl start binance-dca'
```

### Check next scheduled run

```bash
ssh ec2-user@your-elastic-ip 'sudo systemctl list-timers binance-dca.timer'
```

### Disable timer

```bash
ssh ec2-user@your-elastic-ip 'sudo systemctl stop binance-dca.timer'
ssh ec2-user@your-elastic-ip 'sudo systemctl disable binance-dca.timer'
```

## Changing Schedule

Edit the timer file:

```bash
ssh ec2-user@your-elastic-ip
sudo nano /etc/systemd/system/binance-dca.timer
```

Examples:

```
# Every 12 hours
OnCalendar=*-*-* 00,12:00:00

# Weekdays only at 9 AM
OnCalendar=Mon-Fri *-*-* 09:00:00

# Every 6 hours
OnCalendar=*-*-* 00,06,12,18:00:00
```

Then reload:

```bash
sudo systemctl daemon-reload
sudo systemctl restart binance-dca.timer
```

## Troubleshooting

### Check service logs

```bash
sudo journalctl -u binance-dca --since "1 hour ago"
```

### Test JAR manually

```bash
cd ~/binance-dca
java -jar binance-ec2-dca-1.0.0-all.jar
```

### Verify environment variables

```bash
cat ~/binance-dca/.env
```

## Cost Estimate

- **t4g.nano**: ~$3/month (730 hours × $0.0042/hour)
- **Elastic IP**: Free while attached to running instance
- **Data transfer**: Minimal (~$0.01/month)

**Total: ~$3-5/month**

## Security

- Elastic IP whitelisted on Binance
- API keys stored in .env file (readable only by ec2-user)
- No inbound ports needed (except SSH)
- Systemd runs as ec2-user (not root)

-------------------------------------------
File: PROJECT_STRUCTURE.md
-------------------------------------------

# Project Structure

## Overview

This is a monorepo containing both Lambda and EC2 deployments for Binance order management.

```
binance-order-service/
├── core/                    # Shared business logic
│   ├── src/main/kotlin/
│   │   └── com/binance/core/
│   │       ├── BinanceService.kt
│   │       ├── Models.kt
│   │       └── Utils.kt
│   └── build.gradle.kts
│
├── lambda/                  # AWS Lambda deployment
│   ├── src/main/kotlin/
│   │   ├── BinanceOrderHandler.kt
│   │   └── DCAOrderHandler.kt
│   ├── serverless.yml
│   └── build.gradle.kts
│
├── ec2/                     # EC2 cron deployment
│   ├── src/main/kotlin/
│   │   └── Main.kt
│   ├── systemd/
│   ├── deploy.sh
│   └── build.gradle.kts
│
├── settings.gradle.kts      # Gradle multi-module config
├── build.gradle.kts         # Root build config
├── .env                     # Local environment variables
└── README.md
```

## Modules

### Core Module

Shared code used by both Lambda and EC2:

- Binance API integration
- Order creation logic
- Signature generation
- Data models

### Lambda Module

Serverless API for:

- Manual order creation (POST /orders)
- Order retrieval (GET /orders)
- Scheduled DCA (EventBridge)

**Deploy:** `cd lambda && serverless deploy --stage prod`

### EC2 Module

Cron-based DCA execution:

- Daily automated orders
- Static IP (Elastic IP)
- Systemd timer

**Deploy:** `./ec2/deploy.sh`

## Building

### Build everything

```bash
./gradlew build
```

### Build specific module

```bash
./gradlew :core:build
./gradlew :lambda:shadowJar
./gradlew :ec2:shadowJar
```

### Run tests

```bash
./gradlew test
```

## Dependencies

Shared dependencies are defined in `core/build.gradle.kts`:

- Http4k
- Result4k
- Jackson
- dotenv-kotlin

Module-specific dependencies:

- Lambda: AWS Lambda SDK
- EC2: Application plugin

## When to Use Each

| Use Case               | Choose |
|------------------------|--------|
| API for manual trading | Lambda |
| Automated daily DCA    | EC2    |
| Multiple requests/day  | Lambda |
| One scheduled task     | EC2    |
| Need webhooks          | Lambda |
| Budget-conscious       | EC2    |