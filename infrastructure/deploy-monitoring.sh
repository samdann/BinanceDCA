#!/bin/bash
# File: infrastructure/deploy-monitoring.sh
# Deploy CloudWatch monitoring stack

set -e

STACK_NAME="binance-dca-monitoring"
TEMPLATE_FILE="cloudwatch-monitoring.yaml"
REGION="eu-central-1"

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "🚀 Deploying CloudWatch Monitoring Stack"
echo ""

# Extract email addresses from YAML comment
EMAIL_LINE=$(grep "^# EMAIL_ADDRESSES:" $TEMPLATE_FILE)
if [ -z "$EMAIL_LINE" ]; then
    echo "${RED}Error: EMAIL_ADDRESSES not found in $TEMPLATE_FILE${NC}"
    echo ""
    echo "Edit $TEMPLATE_FILE and add this line near the top:"
    echo "# EMAIL_ADDRESSES: your-email@example.com,teammate@example.com"
    exit 1
fi

# Parse emails from the comment line
EMAIL_ADDRESSES=$(echo "$EMAIL_LINE" | sed 's/^# EMAIL_ADDRESSES: *//')

# Convert to array
IFS=',' read -ra EMAILS <<< "$EMAIL_ADDRESSES"

# Trim whitespace
for i in "${!EMAILS[@]}"; do
    EMAILS[$i]=$(echo "${EMAILS[$i]}" | xargs)
done

# Validate we have at least one email
if [ ${#EMAILS[@]} -eq 0 ] || [ "${EMAILS[0]}" == "your-email@example.com" ]; then
    echo -e "${RED}Error: Please configure email addresses in $TEMPLATE_FILE${NC}"
    echo ""
    echo "Edit this line in $TEMPLATE_FILE:"
    echo "# EMAIL_ADDRESSES: your@email.com,teammate@email.com"
    exit 1
fi

echo -e "${YELLOW}Configuration:${NC}"
echo "  Stack Name: $STACK_NAME"
echo "  Region: $REGION"
echo "  Email Addresses:"
for email in "${EMAILS[@]}"; do
    echo "    - $email"
done
echo "  Log Group: /aws/ec2/binance-dca"
echo ""

echo ""
echo -e "${GREEN}📦 Deploying stack...${NC}"


# Deploy with first email
aws cloudformation deploy \
    --template-file $TEMPLATE_FILE \
    --stack-name $STACK_NAME \
    --region $REGION \
    --parameter-overrides \
        PrimaryEmailAddress="${EMAILS[0]}" \
        LogGroupName=/aws/ec2/binance-dca \
        AlarmThreshold=1 \
    --capabilities CAPABILITY_IAM


if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✅ Stack deployed successfully!${NC}"

    # Subscribe additional emails
    if [ ${#EMAILS[@]} -gt 1 ]; then
        echo ""
        echo -e "${GREEN}📧 Subscribing additional email addresses...${NC}"

        TOPIC_ARN=$(aws cloudformation describe-stacks \
            --stack-name $STACK_NAME \
            --region $REGION \
            --query 'Stacks[0].Outputs[?OutputKey==`SNSTopicArn`].OutputValue' \
            --output text)

        for i in $(seq 1 $((${#EMAILS[@]} - 1))); do
            email="${EMAILS[$i]}"
            echo "  Subscribing: $email"
            aws sns subscribe \
                --topic-arn "$TOPIC_ARN" \
                --protocol email \
                --notification-endpoint "$email" \
                --region $REGION > /dev/null
        done

        echo -e "${GREEN}✅ Additional emails subscribed!${NC}"
    fi

    echo ""
    echo -e "${YELLOW}📧 IMPORTANT: Check email inboxes!${NC}"
    echo "All recipients should receive a subscription confirmation email from AWS."
    echo "Click the 'Confirm subscription' link to activate alerts."
    echo ""

    echo -e "${GREEN}Stack Outputs:${NC}"
    aws cloudformation describe-stacks \
        --stack-name $STACK_NAME \
        --region $REGION \
        --query 'Stacks[0].Outputs[*].[OutputKey,OutputValue]' \
        --output table

    echo ""
    echo -e "${GREEN}Next steps:${NC}"
    echo "1. Confirm SNS subscriptions in all email inboxes"
    echo "2. Test the alarm: ./test-alarm.sh"
    echo "3. View metrics: AWS Console → CloudWatch → Metrics → BinanceDCA"
else
    echo -e "${RED}❌ Stack deployment failed!${NC}"
    exit 1
fi