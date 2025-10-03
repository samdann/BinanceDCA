#!/bin/bash
# Run this script ON the EC2 instance to set it up

set -e

echo "🔧 Setting up EC2 instance for Binance DCA..."

# Update system
echo "📦 Updating system packages..."
sudo yum update -y

# Install Java 17
echo "☕ Installing Java 17..."
sudo yum install -y java-17-amazon-corretto

# Verify Java installation
java -version

# Create app directory
echo "📁 Creating application directory..."
mkdir -p ~/binance-dca
mkdir -p ~/binance-dca/systemd

# Create .env file template
cat > ~/binance-dca/.env << 'EOF'
BINANCE_API_KEY=your_api_key_here
BINANCE_API_SECRET=your_api_secret_here
DCA_SYMBOL=BTCUSDT
DCA_AMOUNT=10.50
EOF

echo "✅ EC2 setup complete!"
echo ""
echo "Next steps:"
echo "1. Edit ~/binance-dca/.env with your Binance API credentials"
echo "2. Run the deploy.sh script from your local machine"
echo ""
echo "Commands:"
echo "  Edit .env: nano ~/binance-dca/.env"