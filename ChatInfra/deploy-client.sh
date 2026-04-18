#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Reading Terraform outputs..."
API_URL=$(terraform -chdir="$SCRIPT_DIR" output -raw apiserver_url)
WS_URL=$(terraform -chdir="$SCRIPT_DIR" output -raw chatserver_ws_url)
BUCKET=$(terraform -chdir="$SCRIPT_DIR" output -raw chatclient_bucket)
DIST_ID=$(terraform -chdir="$SCRIPT_DIR" output -raw chatclient_distribution_id)

echo "Building ChatClient..."
cd "$SCRIPT_DIR/../ChatClient"
VITE_API_BASE=$API_URL VITE_WS_URL=$WS_URL npm run build

echo "Syncing to S3..."
aws s3 sync dist/ s3://$BUCKET/ --delete --profile chatsystem

echo "Invalidating CloudFront cache..."
aws cloudfront create-invalidation --distribution-id $DIST_ID --paths "/*" --output text --profile chatsystem

echo "Done. Site is live."
