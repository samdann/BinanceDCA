#!/bin/bash

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}🧪 Testing CloudWatch Alarm${NC}"
echo ""

# Check if EC2_HOST is set
if [ -z "$EC2_HOST" ]; then
    read -p "Enter EC2 Elastic IP: " EC2_HOST
fi

echo -e "${YELLOW}This will:${NC}"
echo "1. SSH to your EC2 instance"
echo "2. Create logs directory if needed"
echo "3. Write a test ERROR to the log file"
echo "4. Trigger the CloudWatch alarm"
echo "5. Send an email alert to your address"
echo ""

read -p "Continue? (yes/no): " CONFIRM
if [ "$CONFIRM" != "yes" ]; then
    echo "Test cancelled."
    exit 0
fi

echo ""
echo -e "${GREEN}Writing test error to log...${NC}"

ssh -i ~/Documents/workspace/backend/BinanceDCA/binance-dca-key.pem ec2-user@$EC2_HOST << 'EOF'
# Create logs directory if it does not exist
mkdir -p ~/binance-dca/logs
chmod 755 ~/binance-dca/logs

# Create or append to error log
touch ~/binance-dca/logs/error.log
chmod 644 ~/binance-dca/logs/error.log

# Write test error
echo "$(date '+%Y-%m-%d %H:%M:%S') [ERROR] TEST: This is a test error message to trigger CloudWatch alarm" >> ~/binance-dca/logs/error.log

echo "✅ Test error written to ~/binance-dca/logs/error.log"
echo ""
echo "Last log entry:"
tail -1 ~/binance-dca/logs/error.log
EOF

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✅ Test error logged successfully!${NC}"
    echo ""
    echo -e "${YELLOW}What happens next:${NC}"
    echo "1. CloudWatch Agent picks up the error log (within 1-2 minutes)"
    echo "2. Metric filter counts the ERROR keyword"
    echo "3. Alarm triggers if threshold is met (within 5-10 minutes)"
    echo "4. SNS sends email notification to all subscribed addresses"
    echo ""
    echo -e "${GREEN}Verification:${NC}"
    echo "• Check CloudWatch Logs: /aws/ec2/binance-dca"
    echo "• Check CloudWatch Alarms: binance-dca-error-alarm"
    echo "• Check your email inbox (including spam folder)"
    echo ""
    echo "View live logs:"
    echo "  ssh -i ~/Documents/workspace/backend/BinanceDCA/binance-dca-key.pem ec2-user@$EC2_HOST 'tail -f ~/binance-dca/logs/error.log'"
else
    echo -e "${RED}❌ Failed to write test error${NC}"
    exit 1
fi

