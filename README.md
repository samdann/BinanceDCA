# BinanceDCA

DCA Bot deployed as Lambda code in AWS

read the deployment info

```
serverless info --stage prod
```

Test with

```
curl -X GET https://m0puqpn9d1.execute-api.eu-central-1.amazonaws.com/prod/orders \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "BTCUSDT"
  }'
```

whitelist IP address in API_Key

```
curl https://api.ipify.org
```

invoke a cron (Rule on EventBridge) with serverless

```
serverless invoke --function dcaOrder --stage prod --log
```