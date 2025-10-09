echo "🚀 Starting deployment of Binance Order Service..."
# Check if environment variables are set
if [ -z "$BINANCE_API_KEY" ]; then
    echo "❌ Error: BINANCE_API_KEY environment variable is not set"
    exit 1
fi

if [ -z "$BINANCE_API_SECRET" ]; then
    echo "❌ Error: BINANCE_API_SECRET environment variable is not set"
    exit 1
fi


# Build the project
echo "📦 Building the project..."
cd .. && ./gradlew clean :lambda:shadowJar && cd lambda

if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    exit 1
fi

echo "✅ Build successful!"

# Deploy with Serverless Framework
echo "🚀 Deploying to AWS..."
serverless deploy --stage prod

if [ $? -eq 0 ]; then
    echo "✅ Deployment successful!"
    echo "🔗 Your API endpoints:"
    serverless info --stage prod
else
    echo "❌ Deployment failed!"
    exit 1
fi