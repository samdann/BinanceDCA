# Deploy Binance DCA service to EC2

set -e

echo "🚀 Deploying Binance DCA to EC2..."

# Configuration
EC2_HOST="${EC2_HOST}"
EC2_USER="${EC2_USER}"
REMOTE_DIR="/home/$EC2_USER/binance-dca"

# Build the project
echo "📦 Building project..."
./gradlew :ec2:shadowJar

# Copy JAR to EC2
echo "📤 Uploading to EC2..."
scp ec2/build/libs/binance-ec2-dca-1.0.0-all.jar $EC2_USER@$EC2_HOST:$REMOTE_DIR/

# Copy environment file
echo "📤 Uploading .env file..."
scp .env $EC2_USER@$EC2_HOST:$REMOTE_DIR/

# Copy systemd files
echo "📤 Uploading systemd files..."
scp ec2/systemd/* $EC2_USER@$EC2_HOST:$REMOTE_DIR/systemd/

# Install systemd services
echo "⚙️  Installing systemd services..."
ssh $EC2_USER@$EC2_HOST << 'EOF'
    cd ~/binance-dca

    # Install systemd files
    sudo cp systemd/binance-dca.service /etc/systemd/system/
    sudo cp systemd/binance-dca.timer /etc/systemd/system/

    # Reload systemd
    sudo systemctl daemon-reload

    # Enable and start timer
    sudo systemctl enable binance-dca.timer
    sudo systemctl start binance-dca.timer

    echo "✅ Deployment complete!"
    echo "Timer status:"
    sudo systemctl status binance-dca.timer --no-pager
EOF

echo "✅ Deployment successful!"
echo ""
echo "Useful commands:"
echo "  Check timer status:  ssh $EC2_USER@$EC2_HOST 'sudo systemctl status binance-dca.timer'"
echo "  View logs:           ssh $EC2_USER@$EC2_HOST 'sudo journalctl -u binance-dca -f'"
echo "  Run manually:        ssh $EC2_USER@$EC2_HOST 'sudo systemctl start binance-dca'"
echo "  Next run time:       ssh $EC2_USER@$EC2_HOST 'sudo systemctl list-timers binance-dca.timer'"